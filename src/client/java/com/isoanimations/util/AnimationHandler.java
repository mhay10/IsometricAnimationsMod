package com.isoanimations.util;

import com.glisco.isometricrenders.render.AreaRenderable;
import com.glisco.isometricrenders.screen.ScreenScheduler;
import com.isoanimations.events.TickStepEvent;
import com.isoanimations.screens.FrameRenderScreen;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.Objects;

import static com.isoanimations.IsometricAnimations.LOGGER;

public class AnimationHandler {
    // Command context
    private FabricClientCommandSource source = null;

    // Animation params
    private final BlockPos pos1;
    private final BlockPos pos2;
    private final int scale;
    private final int rotation;
    private final int slant;
    private final double duration;

    // Animation state
    private double timeElapsed = 0.0;
    private boolean isRunning = false;
    private boolean frameExported = false;
    private boolean isComplete = false;

    public AnimationHandler(FabricClientCommandSource source, BlockPos pos1, BlockPos pos2, int scale, int rotation, int slant, double duration) {
        // Set command context
        this.source = source;

        // Set animation params
        this.pos1 = pos1;
        this.pos2 = pos2;
        this.scale = scale;
        this.rotation = rotation;
        this.slant = slant;
        this.duration = duration;
    }

    public void start() {
        if (this.isRunning) {
            LOGGER.warn("Animation already started.");
            return;
        }
        this.isComplete = false;
        this.isRunning = true;
        this.timeElapsed = 0.0;

        // Register the tick step event to handle frame rendering
        TickStepEvent.TICK_STEP_EVENT.register((source, steps) -> {
            if (this.isRunning && this.frameExported) {
                this.frameExported = false;
                Objects.requireNonNull(MinecraftClient.getInstance().getServer()).save(false, false, false);
                MinecraftClient.getInstance().execute(this::renderNextFrame);
            }

            return ActionResult.PASS;
        });

        // Start the animation by rendering the first frame
        this.renderNextFrame();
    }

    public void stop() {

    }


    private void renderNextFrame() {
        // Stop if animation is not running
        if (!this.isRunning) return;

        // Render last frame of animation if duration exceeded
        if (timeElapsed > duration) {
            LOGGER.info("Animation completed. Rendering last frame...");
            this.isRunning = false;
            this.isComplete = true;
        }

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

            if (this.isComplete) {
                this.source.sendFeedback(Text.literal("Frame generation complete").formatted(Formatting.GREEN));
                return;
            }

            // TODO: Move files to a specific folder for animation creation
            LOGGER.info("Frame {} ({}s) exported to {}", frameNum, frameTime, file.getAbsolutePath());


            // Move to next frame (game tick)
            this.timeElapsed += 0.05;
            this.frameExported = true;
            MinecraftClient.getInstance().execute(() -> CommandRunner.runCommand("/tick step"));
        });
        MinecraftClient.getInstance().execute(() -> ScreenScheduler.schedule(screen));
    }
}
