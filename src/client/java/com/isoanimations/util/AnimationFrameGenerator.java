package com.isoanimations.util;

import com.glisco.isometricrenders.render.AreaRenderable;
import com.glisco.isometricrenders.screen.ScreenScheduler;
import com.isoanimations.events.TickStepEvent;
import com.isoanimations.screens.FrameRenderScreen;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.opengl.GL11;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

import static com.isoanimations.IsometricAnimations.LOGGER;

public class AnimationFrameGenerator {
    // Command context
    private FabricClientCommandSource source = null;

    // Callback interface for when frame generation is complete
    @FunctionalInterface
    public interface CompletionCallback {
        void onComplete(long totalFrames);
    }

    // Frame generation params
    private final BlockPos pos1;
    private final BlockPos pos2;
    private final int scale;
    private final int rotation;
    private final int slant;
    private final double duration;

    // Frame generation state
    private double timeElapsed = 0.0;
    private boolean isRunning = false;
    private boolean frameExported = false;
    private boolean isComplete = false;

    // Completion callback
    private CompletionCallback comletionCallback = null;

    public AnimationFrameGenerator(FabricClientCommandSource source, BlockPos pos1, BlockPos pos2, int scale, int rotation, int slant, double duration) {
        // Set command context
        this.source = source;

        // Set frame generation params
        this.pos1 = pos1;
        this.pos2 = pos2;
        this.scale = scale;
        this.rotation = rotation;
        this.slant = slant;
        this.duration = duration;


        // Clear previously exported frames or create export directory if it doesn't exist
        try {
            if (ExportConfig.FRAME_EXPORT_DIR.toFile().exists()) {
                Files.walk(ExportConfig.FRAME_EXPORT_DIR)
                        .filter(Files::isRegularFile)
                        .map(java.nio.file.Path::toFile)
                        .forEach(File::delete);
            } else {
                ExportConfig.FRAME_EXPORT_DIR.toFile().mkdirs();
            }
        } catch (Exception e) {
            LOGGER.error("Failed to clear previous frames from animation export directory", e);
        }
    }

    public void setCompletionCallback(CompletionCallback callback) {
        this.comletionCallback = callback;
    }

    public void start() {
        // Set state flags
        if (this.isRunning) {
            LOGGER.warn("Frame generation already started.");
            return;
        }
        this.isComplete = false;
        this.isRunning = true;
        this.timeElapsed = 0.0;

        // Register the tick step event to handle frame rendering
        TickStepEvent.TICK_STEP_EVENT.register((source, steps) -> {
            if (this.isRunning && this.frameExported) {
                // Create sync function to ensure all rendering tasks are complete
                MinecraftClient client = MinecraftClient.getInstance();
                Runnable syncAndRender = () -> {
                    // Make sure all OpenGL tasks are complete
                    RenderSystem.assertOnRenderThread();
                    GL11.glFinish();

                    // Save the world to make sure all events are processes
                    Objects.requireNonNull(client.getServer()).save(false, true, false);

                    // Sync chunk and block updates
                    client.world.getChunkManager().tick(() -> true, false);
//                    client.worldRenderer.scheduleBlockRenders();
                    RenderSystem.replayQueue();

                    // Render the next frame
                    this.frameExported = false;
                    client.execute(this::renderNextFrame);
                };

                if (RenderSystem.isOnRenderThread())
                    syncAndRender.run();
                else
                    client.execute(syncAndRender);
            }

            return ActionResult.PASS;
        });

        // Start the frame generation by rendering the first frame
        source.sendFeedback(Text.literal("Starting frame generation").formatted(Formatting.GREEN));
        this.renderNextFrame();
    }

    public void stop() {
        if (!this.isRunning) {
            LOGGER.warn("Frame generation not currently running");
            return;
        }

        // Reset state flags
        this.isRunning = false;
        this.isComplete = true;

        // Notify user that frame generation has been stopped
        this.source.sendFeedback(Text.literal("Frame generation complete").formatted(Formatting.GREEN));

        // Invoke completion callback if set
        if (this.comletionCallback != null) {
            long totalFrames = Math.round(this.duration * 20);
            this.comletionCallback.onComplete(totalFrames);
        }
    }


    private void renderNextFrame() {
        // Stop frame generation is not running
        if (!this.isRunning) return;

        // Check if this is the last frame
        boolean isLastFrame = timeElapsed >= duration;

        // Create renderable area from positions
        AreaRenderable area = AreaRenderable.of(this.pos1, this.pos2);
        area.properties().scale.set(this.scale);
        area.properties().rotation.set(this.rotation);
        area.properties().slant.set(this.slant);

        // Create and schedule the frame
        FrameRenderScreen screen = new FrameRenderScreen(area);
        screen.setExportCallback(file -> {
            // Notify player of render progress
            long frameNum = Math.round(timeElapsed * 20);
            String frameTime = String.format("%.2f", timeElapsed);
            this.source.sendFeedback(Text.literal("Frame %d (%ss) rendered".formatted(frameNum, frameTime)).formatted(Formatting.YELLOW));

            // Move files to export directory
            File newFile = ExportConfig.FRAME_EXPORT_DIR.resolve("frame_%05d.png".formatted(frameNum)).toFile();
            try {
                Files.move(file.toPath(), newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception e) {
                LOGGER.error("Failed to move exported frame to animation directory", e);
            }
            LOGGER.info("Frame {} ({}s) exported to {}", frameNum, frameTime, newFile.getAbsolutePath());

            // Stop if this was the last frame otherwise continue to next frame
            if (isLastFrame) {
                this.stop();
            } else {
                // Move to next frame (game tick)
                this.timeElapsed += 0.05;
                this.frameExported = true;
                MinecraftClient.getInstance().execute(() -> CommandRunner.runCommand("/tick step"));
            }
        });
        MinecraftClient.getInstance().execute(() -> ScreenScheduler.schedule(screen));
    }
}
