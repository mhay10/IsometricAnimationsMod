package com.isoanimations.commands;

import com.isoanimations.config.AnimationConfig;
import com.isoanimations.config.RenderConfig;
import com.isoanimations.manager.*;
import com.isoanimations.util.BufferPool;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.WorldCoordinates;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback.EVENT;

public class CreateAnimationCommand {
    public static void registerCommand() {
        EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("isoanimations")
                        // Clear previous animation state
                        .then(ClientCommandManager.literal("clear").executes(CreateAnimationCommand::clearAnimation))

                        // Test render transformations without creating animation
                        .then(ClientCommandManager.literal("testpos")
                                .then(buildAnimationArguments(false)))

                        // Create new animation with specifies arguments
                        .then(ClientCommandManager.literal("create")
                                .then(buildAnimationArguments(true)))));
    }

    private static ArgumentBuilder<FabricClientCommandSource, ?> buildAnimationArguments(boolean creatingAnimation) {
        // Build yaw argument first depending on if creating animation or not
        var yawArg = ClientCommandManager.argument("yaw", IntegerArgumentType.integer(-90, 90));
        if (creatingAnimation) {
            // Add duration argument if creating animation
            yawArg.then(ClientCommandManager.argument("duration", DoubleArgumentType.doubleArg(0.0))
                    .executes(CreateAnimationCommand::newAnimation));
        } else {
            // Only position player without creating animation
            yawArg.executes(CreateAnimationCommand::positionPlayer);
        }

        // Build rest of arguments
        return ClientCommandManager.argument("pos1", BlockPosArgument.blockPos())
                .then(ClientCommandManager.argument("pos2", BlockPosArgument.blockPos())
                        .then(ClientCommandManager.argument("scale", IntegerArgumentType.integer(100, 500))
                                .then(ClientCommandManager.argument("pitch", IntegerArgumentType.integer(0, 360))
                                        .then(yawArg))));
    }

    private static int clearAnimation(CommandContext<FabricClientCommandSource> context) {
        AnimationManager.clearAnimation();
        context.getSource().getClient().execute(context.getSource().getClient().levelRenderer::allChanged);
        context.getSource().sendFeedback(Component.literal("Cleared active animation region and stopped animation."));
        return 1;
    }

    private static int newAnimation(CommandContext<FabricClientCommandSource> context) {
        // Render only if game frozen with '/tick freeze'
        var source = context.getSource();
        boolean isFrozen = context.getSource().getWorld().tickRateManager().isFrozen();
        if (!isFrozen) {
            source.sendError(Component.literal("Game must be frozen to create animation. Use '/tick freeze' command first."));
            return 0;
        }

        // Parse command arguments
        AnimationConfig config = AnimationConfig.parse(context, true);
        int durationTicks = (int) Math.ceil(config.duration() * RenderConfig.TICKS_PER_SECOND); // Convert seconds to ticks

        // Store original settings for reset after animation
        Vec3 origPlayerPos = source.getPlayer().position();
        float xRot = source.getPlayer().getXRot();
        float yRot = source.getPlayer().getYRot();
        int origFps = source.getClient().options.framerateLimit().get();

        // Notify user about animation creation and settings
        source.sendFeedback(
                Component.literal("Creating animation with a duration of %.2f seconds (%d ticks)...".formatted(config.duration(), durationTicks))
                        .withStyle(ChatFormatting.GREEN)
        );

        // Create new animation region
        AnimationManager.createAnimation(config.pos1(), config.pos2(), durationTicks);

        // Initialize animation state and player position
        AtomicBoolean ioReady = new AtomicBoolean(false);
        positionPlayer(context);
        preAnimationInit(source, ioReady);

        // Register on client tick event to manage animation state and frame capture
        AtomicBoolean waitingForReload = new AtomicBoolean(true);
        AtomicInteger waitTicks = new AtomicInteger(0);
        final int waitThreshold = 15;
        ClientTickEvents.EndTick animationEvent = (client) -> {
            if (waitingForReload.get()) {
                // Increment wait ticks while waiting for chunks to reload
                waitTicks.incrementAndGet();

                // Wait until io operations are done and all chunks are loaded before starting animation
                var dispatcher = client.levelRenderer.getSectionRenderDispatcher();
                if (ioReady.get() && waitTicks.get() > waitThreshold && dispatcher.isQueueEmpty()) {
                    waitingForReload.set(false);

                    // Start animation frame capture
                    AnimationManager.startAnimation();
                    CommandRunner.runCommand("/tick step %d".formatted(durationTicks));
                }

                // Block rest of event while waiting to start
                return;
            }

            // Stop recording after duration has passed
            if (AnimationManager.isAnimating() && client.level.getGameTime() >= AnimationManager.getEndTick()) {
                // Cleanup animation state and reset player
                postAnimationCleanup(source, origPlayerPos, xRot, yRot, origFps);

                // Notify user about frame generation status
                source.sendFeedback(
                        Component.literal("Frame generation complete. Waiting for frame exports to finish...").withStyle(ChatFormatting.YELLOW)
                );

                // Wait for frame exports to finish before starting video creation
                CompletableFuture.runAsync(FrameExportManager::waitForExportFinish).thenRun(() -> {
                    client.execute(() -> source.sendFeedback(
                            Component.literal("Creating animation from frames. This may take a while...").withStyle(ChatFormatting.YELLOW)
                    ));
                    FrameAssemblerManager.createAnimation(source);
                });
            }
        };
        ClientTickEvents.END_CLIENT_TICK.register(animationEvent);

        return 1;
    }

    private static void preAnimationInit(FabricClientCommandSource source, AtomicBoolean ioReady) {
        // Reload chunks to ensure all render changes are applied before starting animation
        source.getClient().execute(source.getClient().levelRenderer::allChanged);

        // Initialize PBOs for frame capture
        int width = source.getClient().getWindow().getWidth();
        int height = source.getClient().getWindow().getHeight();
        FrameCaptureManager.initPBOs(width, height);

        // Initialize frame managers in separate thread
        CompletableFuture.runAsync(() -> {
            // Initialize buffer pool based on render dimensions
            BufferPool.init(width * height * 3);

            // Initialize frame export and assembler managers
            FrameExportManager.init();
            FrameAssemblerManager.init();

            // Wait a moment to ensure everything is ready
            try {
                Thread.sleep(250);
            } catch (Exception ignored) {
            }
        }).thenRun(() -> ioReady.set(true)); // Set IO ready flag after initialization is complete

        // Set render settings for animation
        source.getClient().options.hideGui = true;
        source.getClient().options.framerateLimit().set(RenderConfig.renderFps);
        CommandRunner.runCommand("/tick rate %s".formatted(RenderConfig.tickRate));
    }

    private static void postAnimationCleanup(FabricClientCommandSource source, Vec3 origPlayerPos, float origXRot, float origYRot, int origFps) {
        // Stop and clear animation state
        AnimationManager.stopAnimation();
        AnimationManager.clearAnimation();
        source.getClient().options.hideGui = false; // Unhide GUI after animation

        // Reset render transformations and player position
        CommandRunner.runCommand("/tp @s %s %s %s".formatted(origPlayerPos.x, origPlayerPos.y, origPlayerPos.z));
        source.getPlayer().setXRot(origXRot);
        source.getPlayer().setYRot(origYRot);

        // Reset original game settings
        source.getClient().options.framerateLimit().set(origFps);
        CommandRunner.runCommand("/tick rate %f".formatted(RenderConfig.TICKS_PER_SECOND));

        // Reload chunks to reset render changes
        source.getClient().execute(source.getClient().levelRenderer::allChanged);

        // TODO: Free allocated memory in buffer pool, PBOS, etc.
    }

    private static int positionPlayer(CommandContext<FabricClientCommandSource> context) {
        // Parse command arguments
        AnimationConfig config = AnimationConfig.parse(context, false);

        // Set render transformations
        RenderManager.setScale(config.scale());
        RenderManager.setPitch(config.pitch());
        RenderManager.setYaw(config.yaw());

        // Teleport player to render position
        var source = context.getSource();
        Vector3f renderPos = RenderManager.getRenderPosition(config.pos1(), config.pos2(), source.getPlayer().position(), 30);
        Vector3f centerPos = RenderManager.getCenterPosition(config.pos1(), config.pos2());
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
}
