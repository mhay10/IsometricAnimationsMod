package com.isoanimations.commands;

import com.isoanimations.util.AnimationManager;
import com.isoanimations.util.CommandRunner;
import com.isoanimations.util.RenderManager;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.WorldCoordinates;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.Objects;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback.EVENT;

public class CreateAnimationCommand {
    public static void registerCommand() {
        EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("isoanimations")
                        // Clear previous animation state
                        .then(ClientCommandManager.literal("clear").executes(CreateAnimationCommand::clearAnimation))

                        // Test render transformations without creating animation
                        .then(ClientCommandManager.literal("testpos")
                                .then(ClientCommandManager.argument("pos1", BlockPosArgument.blockPos())
                                        .then(ClientCommandManager.argument("pos2", BlockPosArgument.blockPos())
                                                .then(ClientCommandManager.argument("scale", IntegerArgumentType.integer(100, 500))
                                                        .then(ClientCommandManager.argument("pitch", IntegerArgumentType.integer(0, 360))
                                                                .then(ClientCommandManager.argument("yaw", IntegerArgumentType.integer(-90, 90))
                                                                        .executes(CreateAnimationCommand::positionPlayer)))))))

                        // Create new animation with specifies arguments
                        .then(ClientCommandManager.literal("create")
                                .then(ClientCommandManager.argument("pos1", BlockPosArgument.blockPos())
                                        .then(ClientCommandManager.argument("pos2", BlockPosArgument.blockPos())
                                                .then(ClientCommandManager.argument("scale", IntegerArgumentType.integer(100, 500))
                                                        .then(ClientCommandManager.argument("pitch", IntegerArgumentType.integer(0, 360))
                                                                .then(ClientCommandManager.argument("yaw", IntegerArgumentType.integer(-90, 90))
                                                                        .then(ClientCommandManager.argument("duration", DoubleArgumentType.doubleArg(0.1))
                                                                                .executes(CreateAnimationCommand::newAnimation))))))))));
    }

    public static int clearAnimation(CommandContext<FabricClientCommandSource> context) {
        AnimationManager.clearAnimation();
        context.getSource().getClient().execute(context.getSource().getClient().levelRenderer::allChanged);
        context.getSource().sendFeedback(Component.literal("Cleared active animation region and stopped animation."));
        return 1;
    }

    public static int newAnimation(CommandContext<FabricClientCommandSource> context) {
        // Render only if game frozen with '/tick freeze'
        var source = context.getSource();
        boolean isFrozen = context.getSource().getWorld().tickRateManager().isFrozen();
        if (!isFrozen) {
            source.sendError(Component.literal("Game must be frozen to create animation. Use '/tick freeze' command first."));
            return 0;
        }

        // Get arguments from command context
        WorldCoordinates pos1 = context.getArgument("pos1", WorldCoordinates.class);
        WorldCoordinates pos2 = context.getArgument("pos2", WorldCoordinates.class);
        double duration = context.getArgument("duration", Double.class);

        // Convert arguments to more usable forms
        var blockPos1 = pos1.getBlockPos(Objects.requireNonNull(source.getClient().getSingleplayerServer()).createCommandSourceStack()); // wtf is this
        var blockPos2 = pos2.getBlockPos(Objects.requireNonNull(source.getClient().getSingleplayerServer()).createCommandSourceStack());
        int durationTicks = (int) Math.ceil(duration * 20); // Convert seconds to ticks

        // Create new animation region and set render transformations
        AnimationManager.createAnimation(blockPos1, blockPos2, durationTicks);
        positionPlayer(context);
        source.getClient().execute(source.getClient().levelRenderer::allChanged);

        // Start recording animation
        ClientTickEvents.EndTick animationEvent = (client) -> {
            // Wait for recorder to be ready before stepping
            if (!AnimationManager.isAnimating() && !AnimationManager.isAnimationFinished()) {
                // TODO: Add short delay before starting animation to ensure player is positioned and all chunks are loaded

                AnimationManager.startAnimation();
                CommandRunner.runCommand("/tick step %d".formatted(durationTicks));
            }

            // Stop recording after duration has passed
            if (AnimationManager.isAnimating() && client.level.getGameTime() >= AnimationManager.getEndTick()) {
                // Stop animation and recording
                AnimationManager.stopAnimation();

                // TODO: Add short delay before exporting animation to ensure all frames are captured

                // Start export process
                AnimationManager.finishAnimation();
                exportAnimation(source);
            }
        };
        ClientTickEvents.END_CLIENT_TICK.register(animationEvent);

        return 1;
    }

    private static int positionPlayer(CommandContext<FabricClientCommandSource> context) {
        var source = context.getSource();

        // Get arguments from command context
        WorldCoordinates pos1 = context.getArgument("pos1", WorldCoordinates.class);
        WorldCoordinates pos2 = context.getArgument("pos2", WorldCoordinates.class);
        int scale = context.getArgument("scale", Integer.class);
        int pitch = context.getArgument("pitch", Integer.class);
        int yaw = context.getArgument("yaw", Integer.class);

        // Convert arguments to more usable forms
        var blockPos1 = pos1.getBlockPos(Objects.requireNonNull(source.getClient().getSingleplayerServer()).createCommandSourceStack()); // wtf is this
        var blockPos2 = pos2.getBlockPos(Objects.requireNonNull(source.getClient().getSingleplayerServer()).createCommandSourceStack());

        // Set render transformations
        RenderManager.setScale(scale);
        RenderManager.setPitch(pitch);
        RenderManager.setYaw(yaw);

        // Teleport player to render position
        Vector3f renderPos = RenderManager.getRenderPosition(blockPos1, blockPos2, source.getPlayer().position(), 30);
        Vector3f centerPos = RenderManager.getCenterPosition(blockPos1, blockPos2);
        CommandRunner.runCommand("/tp @s %s %s %s".formatted(renderPos.x, renderPos.y, renderPos.z));

        // Make sure world is loaded
        source.getClient().getSingleplayerServer().saveEverything(false, true, false);

        // Position player on client thread
        source.getClient().execute(() -> {
            // Look at center of animation region
            source.getClient().player.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(centerPos));

            // Try to prevent camera clipping region
            CommandRunner.runCommand("/tp @s ~ ~0.5 ~");
        });

        return 1;
    }

    private static void exportAnimation(FabricClientCommandSource source) {
        // TODO: Figure out how to export replay as video

        source.sendFeedback(Component.literal("Exporting now (not really tho)..."));
    }
}
