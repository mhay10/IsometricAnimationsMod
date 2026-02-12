package com.isoanimations.commands;

import com.isoanimations.util.AnimationManager;
import com.isoanimations.util.CommandRunner;
import com.isoanimations.util.RenderManager;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.moulberry.flashback.Flashback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.WorldCoordinates;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

import java.util.Objects;

import static com.isoanimations.IsometricAnimations.LOGGER;
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
                                                        .then(ClientCommandManager.argument("skew", IntegerArgumentType.integer(-90, 90))
                                                                .then(ClientCommandManager.argument("rotation", IntegerArgumentType.integer(0, 360))
                                                                        .executes(CreateAnimationCommand::testPosition)))))))

                        // Create new animation with specifies arguments
                        .then(ClientCommandManager.literal("create")
                                .then(ClientCommandManager.argument("pos1", BlockPosArgument.blockPos())
                                        .then(ClientCommandManager.argument("pos2", BlockPosArgument.blockPos())
                                                .then(ClientCommandManager.argument("scale", IntegerArgumentType.integer(100, 500))
                                                        .then(ClientCommandManager.argument("skew", IntegerArgumentType.integer(-90, 90))
                                                                .then(ClientCommandManager.argument("rotation", IntegerArgumentType.integer(0, 360))
                                                                        .then(ClientCommandManager.argument("duration", DoubleArgumentType.doubleArg(0))
                                                                                .executes(CreateAnimationCommand::newAnimation))))))))));
    }

    public static int clearAnimation(CommandContext<FabricClientCommandSource> context) {
        AnimationManager.clearPrevAnimation();
        context.getSource().sendFeedback(Component.literal("Cleared active animation region and stopped animation."));
        return 1;
    }

    public static int testPosition(CommandContext<FabricClientCommandSource> context) {
        // Get arguments from command context
        WorldCoordinates pos1 = context.getArgument("pos1", WorldCoordinates.class);
        WorldCoordinates pos2 = context.getArgument("pos2", WorldCoordinates.class);
        int scale = context.getArgument("scale", Integer.class);
        int skew = context.getArgument("skew", Integer.class);
        int rotation = context.getArgument("rotation", Integer.class);

        // Convert arguments to more usable forms
        var blockPos1 = pos1.getBlockPos(Objects.requireNonNull(context.getSource().getClient().getSingleplayerServer()).createCommandSourceStack()); // wtf is this
        var blockPos2 = pos2.getBlockPos(Objects.requireNonNull(context.getSource().getClient().getSingleplayerServer()).createCommandSourceStack());

        // Set render transformations
        RenderManager.setScale(scale);
        RenderManager.setSkew(skew);
        RenderManager.setRotation(rotation);

        // Teleport player to render position
        Vector3d renderPos = RenderManager.getRenderPosition(blockPos1, blockPos2);
        CommandRunner.runCommand("/tp @s %.6f %.6f %.6f".formatted(renderPos.x, renderPos.y, renderPos.z));

        // Make sure world is loaded at render position before proceeding
        var source = context.getSource();
        source.getClient().getSingleplayerServer().saveEverything(false, true, false);

        // Make player face center of region
        var selectedRegion = BoundingBox.fromCorners(blockPos1, blockPos2);
        source.getClient().execute(() -> {
            source.getClient().player.lookAt(EntityAnchorArgument.Anchor.EYES, selectedRegion.getCenter().getCenter());
        });

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
        int scale = context.getArgument("scale", Integer.class);
        int skew = context.getArgument("skew", Integer.class);
        int rotation = context.getArgument("rotation", Integer.class);
        double duration = context.getArgument("duration", Double.class);

        // Convert arguments to more usable forms
        var blockPos1 = pos1.getBlockPos(Objects.requireNonNull(source.getClient().getSingleplayerServer()).createCommandSourceStack()); // wtf is this
        var blockPos2 = pos2.getBlockPos(Objects.requireNonNull(source.getClient().getSingleplayerServer()).createCommandSourceStack());
        int durationTicks = (int) Math.ceil(duration * 20); // Convert seconds to ticks

        // Create animation region
        AnimationManager.createAnimation(blockPos1, blockPos2, durationTicks);

        // Set render transformations
        RenderManager.setScale(scale);
        RenderManager.setSkew(skew);
        RenderManager.setRotation(rotation);

        // Teleport player to render position
        Vector3d renderPos = RenderManager.getRenderPosition(blockPos1, blockPos2);
        Vec3 centerPos = RenderManager.getCenterPosition(blockPos1, blockPos2);
        CommandRunner.runCommand("/tp @s %s %s %s".formatted((float) renderPos.x, (float) renderPos.y, (float) renderPos.z));

        // Make sure world is loaded at render position before proceeding
        source.getClient().getSingleplayerServer().saveEverything(false, true, false);

        // Make player face center of region
        source.getClient().execute(() -> {
            source.getClient().player.lookAt(EntityAnchorArgument.Anchor.EYES, centerPos);
        });

        // Start recording and wait for ready state
        LOGGER.info("Starting animation recording...");
        Flashback.startRecordingReplay();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Wait for recorder to be ready before stepping
            if (!AnimationManager.isAnimating() && Flashback.RECORDER != null && Flashback.RECORDER.readyToWrite()) {
                AnimationManager.startAnimation();
                CommandRunner.runCommand("/tick step %d".formatted(durationTicks));
            }

            // Stop recording after duration has passed
            if (AnimationManager.isAnimating() && client.level.getGameTime() >= AnimationManager.getEndTick()) {
                // Stop animation and recording
                AnimationManager.stopAnimation();
                Flashback.finishRecordingReplay();
                LOGGER.info("Animation recording finished...");

                // Start export process
                exportAnimation(source);
            }
        });

        return 1;
    }

    private static void exportAnimation(FabricClientCommandSource source) {
        // TODO: Figure out how to export replay as video

        // Set screen to null to render thread
        Minecraft.getInstance().execute(() -> {
            Minecraft.getInstance().setScreen(null);
            LOGGER.info("No GUI screen for you hahahaha");
        });

    }
}
