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
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.opencv.video.Video;

import java.util.concurrent.atomic.AtomicBoolean;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback.EVENT;

public class CreateAnimationCommand {
    private static boolean eventRegistered = false;
    private static long chunkReloadStartTime = 0;

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
            yawArg.executes(context -> {
                AnimationManager.setTestingPosition(true);
                return positionCamera(context);
            });
        }

        // Build rest of arguments
        return ClientCommandManager.argument("pos1", BlockPosArgument.blockPos())
                .then(ClientCommandManager.argument("pos2", BlockPosArgument.blockPos())
                        .then(ClientCommandManager.argument("scale", IntegerArgumentType.integer(100, 500))
                                .then(ClientCommandManager.argument("pitch", IntegerArgumentType.integer(0, 360))
                                        .then(yawArg))));
    }

    private static int clearAnimation(CommandContext<FabricClientCommandSource> context) {
        CameraManager.reset();
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

        // Check if previous animation is still exporting
        if (VideoStreamManager.isExporting()) {
            source.sendError(Component.literal("Cannot start new animation. Thr previous animation is still exporting"));
            return 0;
        }

        // Parse command arguments
        AnimationConfig config = AnimationConfig.parse(context, true);
        int durationTicks = (int) Math.ceil(config.duration() * RenderConfig.TICKS_PER_SECOND); // Convert seconds to ticks

        // Store original settings for reset after animation
        int origFps = source.getClient().options.framerateLimit().get();
        AnimationManager.setOriginalFps(origFps);

        // Notify user about animation creation and settings
        source.sendFeedback(
                Component.literal("Creating animation with a duration of %.2f seconds (%d ticks)...".formatted(config.duration(), durationTicks))
                        .withStyle(ChatFormatting.GREEN)
        );

        // Create new animation region
        AnimationManager.createAnimation(config.pos1(), config.pos2(), durationTicks);

        // Initialize animation state and camera position
        positionCamera(context);
        preAnimationInit(source);

        // Register on client tick event to manage animation state and frame capture
        if (!eventRegistered) {
            ClientTickEvents.END_CLIENT_TICK.register(client -> {
                // Exit if no animation region active
                if (AnimationManager.getActiveRegion() == null) {
                    return;
                }

                // ===== START ANIMATION LOGIC =====
                if (!AnimationManager.isAnimating() && !AnimationManager.isAnimationFinished()) {
                    // Wait for chunk compiler to populate queue
                    if (System.currentTimeMillis() - chunkReloadStartTime < 1000) {
                        return;
                    }

                    // Wait for chunk compiler to finish building meshes
                    boolean chunksCompiling = !client.levelRenderer.getSectionRenderDispatcher().isQueueEmpty();
                    if (chunksCompiling) {
                        return;
                    }

                    // Start animation after chunks fully loaded
                    AnimationManager.startAnimation();
                    CommandRunner.runCommand("/tick step %d".formatted(AnimationManager.getDurationTicks()));
                }

                // ===== END ANIMATION LOGIC =====
                if (AnimationManager.isAnimating() && client.level.getGameTime() >= AnimationManager.getEndTick()) {
                    postAnimationCleanup(source, AnimationManager.getOriginalFps());
                }
            });

            eventRegistered = true;
        }

        return 1;
    }

    private static void preAnimationInit(FabricClientCommandSource source) {
        // Reload chunks to ensure all render changes are applied before starting animation
        source.getClient().execute(source.getClient().levelRenderer::allChanged);

        // Initialize buffer pool based on window dimensions
        int width = source.getClient().getWindow().getWidth();
        int height = source.getClient().getWindow().getHeight();
        BufferPool.init(width * height * 3);

        // Start streaming thread to encode frames
        VideoStreamManager.startRecording(width, height);

        // Set render settings for animation
        source.getClient().options.hideGui = true;
        source.getClient().options.framerateLimit().set(RenderConfig.renderFps);
        CommandRunner.runCommand("/tick rate %s".formatted(RenderConfig.tickRate));

        // Record time chunks were reloaded
        chunkReloadStartTime = System.currentTimeMillis();
    }

    private static void postAnimationCleanup(FabricClientCommandSource source, int origFps) {
        VideoStreamManager.stopRecording(source);

        // Stop and clear animation state
        AnimationManager.stopAnimation();
        AnimationManager.clearAnimation();
        source.getClient().options.hideGui = false; // Unhide GUI after animation

        // Reset render transformations and player position
        CameraManager.reset();

        // Reset original game settings
        source.getClient().options.framerateLimit().set(origFps);
        CommandRunner.runCommand("/tick rate %f".formatted(RenderConfig.TICKS_PER_SECOND));

        // Reload chunks to reset render changes
        source.getClient().execute(source.getClient().levelRenderer::allChanged);
    }

    private static int positionCamera(CommandContext<FabricClientCommandSource> context) {
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

        // Offset Y pos to help center camera
        renderPos.y += 0.5f;

        // Calculate pitch and yaw for center of reigon
        double dx = centerPos.x - renderPos.x;
        double dy = centerPos.y - renderPos.y;
        double dz = centerPos.z - renderPos.z;
        double distXZ = Math.sqrt(dx * dx + dz * dz);

        float pitch = (float) -(Math.atan2(dy, distXZ) * (180 / Math.PI));
        float yaw = (float) (Math.atan2(dz, dx) * (180 / Math.PI)) - 90.0f;
        CameraManager.setCamera(new Vec3(renderPos), pitch, yaw);

        return 1;
    }
}
