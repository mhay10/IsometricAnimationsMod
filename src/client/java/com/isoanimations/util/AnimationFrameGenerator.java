package com.isoanimations.util;

import com.glisco.isometricrenders.render.AreaRenderable;
import com.glisco.isometricrenders.screen.ScreenScheduler;
import com.isoanimations.render.AnimationStateTracker;
import com.isoanimations.render.EntityAnimationTracker;
import com.isoanimations.screens.FrameRenderScreen;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.isoanimations.events.TickStepCompleteEvent;
import net.minecraft.util.ActionResult;

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
    private long currentFrameNumber = 0;
    private int currentGameTick = 0;
    private boolean waitingForTickStep = false;
    private boolean renderingFrame = false; // Prevents concurrent frame rendering
    private boolean needsInitialTick = true; // Flag to execute first tick before rendering
    private boolean pendingStop = false;
    // Expected target used when waiting for tick completion (>=0 to set
    // currentGameTick)
    private volatile int expectedTargetTick = -1;

    // Static list of generators waiting for tick-completion notifications
    private static final CopyOnWriteArrayList<AnimationFrameGenerator> TICK_WAITERS = new CopyOnWriteArrayList<>();

    // Register a single global listener that notifies waiting generators when /tick
    // step completes
    static {
        TickStepCompleteEvent.TICK_STEP_COMPLETE_EVENT.register((serverSource, steps) -> {
            // Iterate over a snapshot to avoid concurrent modification issues
            AnimationFrameGenerator[] snapshot = TICK_WAITERS.toArray(new AnimationFrameGenerator[0]);
            for (AnimationFrameGenerator g : snapshot) {
                g.onTickStepCompleted(steps);
            }
            return ActionResult.SUCCESS;
        });
    }

    // Completion callback
    private CompletionCallback completionCallback = null;

    // Animation state tracker
    private final AnimationStateTracker stateTracker = AnimationStateTracker.getInstance();
    private final EntityAnimationTracker entityTracker = EntityAnimationTracker.getInstance();

    // Called from the global tick-completion listener when /tick step completes
    private void onTickStepCompleted(int steps) {
        // Remove ourselves - if we weren't waiting any more ignore
        if (!TICK_WAITERS.remove(this))
            return;
        final int setTo = this.expectedTargetTick;
        this.expectedTargetTick = -1;

        MinecraftClient.getInstance().execute(() -> {
            // Capture new state AFTER the tick has executed
            stateTracker.captureAfterTick();
            entityTracker.captureAfterTick();

            if (setTo >= 0)
                this.currentGameTick = setTo;
            this.waitingForTickStep = false;
            LOGGER.info("Tick advanced to {}, now rendering frame {}", this.currentGameTick, this.currentFrameNumber);
            this.renderNextFrame();
        });
    }

    // Pending file moves for batching
    private final List<File> pendingSources = new ArrayList<>();
    private final List<File> pendingDests = new ArrayList<>();

    // Static method to check if any AnimationFrameGenerator is running
    private static volatile boolean anyRunning = false;

    public static boolean isAnyRunning() {
        return anyRunning;
    }

    public AnimationFrameGenerator(FabricClientCommandSource source, BlockPos pos1, BlockPos pos2, int scale,
            int rotation, int slant, double duration) {
        // Set command context
        this.source = source;

        // Set frame generation params
        this.pos1 = pos1;
        this.pos2 = pos2;
        this.scale = scale;
        this.rotation = rotation;
        this.slant = slant;
        this.duration = duration;

        this.initExportDir();
    }

    public void setCompletionCallback(CompletionCallback callback) {
        this.completionCallback = callback;
    }

    public void start() {
        // Set state flags
        if (this.isRunning) {
            LOGGER.warn("Frame generation already started.");
            return;
        }
        this.isRunning = true;
        anyRunning = true;
        this.timeElapsed = 0.0;
        this.currentFrameNumber = 0;
        this.currentGameTick = 0;
        this.needsInitialTick = true; // Need to execute first tick before rendering

        // Start tracking animation states in the rendering area
        stateTracker.startTracking(pos1, pos2);

        // Start the frame generation
        long totalFrames = SubTickConfig.getTotalFrames(duration);
        int totalTicks = SubTickConfig.getTotalTicks(duration);
        source.sendFeedback(Text.literal(
                "Starting frame generation: %d frames @ %d FPS (%d game ticks)"
                        .formatted(totalFrames, SubTickConfig.getTargetFPS(), totalTicks))
                .formatted(Formatting.GREEN));

        // Start rendering immediately - we'll manage state manually
        this.renderNextFrame();
    }

    public void stop() {
        // Perform any pending file moves
        for (int i = 0; i < pendingSources.size(); i++) {
            try {
                Files.move(pendingSources.get(i).toPath(), pendingDests.get(i).toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
                LOGGER.info("Frame moved to {} - COMPLETE", pendingDests.get(i).getAbsolutePath());
            } catch (Exception e) {
                LOGGER.error("Failed to move frame to {}", pendingDests.get(i).getAbsolutePath(), e);
            }
        }
        pendingSources.clear();
        pendingDests.clear();

        if (!this.isRunning) {
            LOGGER.warn("Frame generation not currently running");
            return;
        }

        // Stop tracking animation states
        stateTracker.stopTracking();
        entityTracker.stopTracking();

        // Remove ourselves from any tick waiters in case we were waiting
        TICK_WAITERS.remove(this);

        // Trim memory held by trackers aggressively since we're stopping
        try {
            stateTracker.trimMemory(true); // aggressive - safe at stop
        } catch (Exception e) {
            LOGGER.warn("Failed to trim memory in state tracker", e);
        }
        try {
            entityTracker.trimMemory(true);
        } catch (Exception e) {
            LOGGER.warn("Failed to trim memory in entity tracker", e);
        }
        System.gc();

        // Capture references for notification and callback
        final FabricClientCommandSource localSource = this.source;
        final CompletionCallback localCallback = this.completionCallback;

        // Reset state flags
        this.isRunning = false;
        anyRunning = false;

        // Notify user that frame generation has been stopped
        if (localSource != null) {
            try {
                localSource.sendFeedback(
                        Text.literal("Frame generation complete: %d frames rendered".formatted(currentFrameNumber))
                                .formatted(Formatting.GREEN));
            } catch (Exception e) {
                LOGGER.warn("Failed to send completion feedback", e);
            }
        }

        // Invoke completion callback if set
        if (localCallback != null) {
            try {
                localCallback.onComplete(currentFrameNumber);
            } catch (Exception e) {
                LOGGER.warn("Completion callback threw", e);
            }
        }

        // Clear heavy references to allow GC
        this.source = null;
        this.completionCallback = null;
    }

    private void renderNextFrame() {
        // Stop frame generation if not running
        if (!this.isRunning)
            return;

        // If we're already rendering a frame, don't start another one
        if (renderingFrame) {
            LOGGER.warn("Attempted to render frame {} while frame {} is still rendering - skipping",
                    currentFrameNumber + 1, currentFrameNumber);
            return;
        }

        // If we're waiting for a tick step to complete, don't render yet
        if (waitingForTickStep)
            return;

        // If this is the very first frame, execute the initial tick first
        if (needsInitialTick) {
            LOGGER.info("Executing initial tick (tick 0) before rendering any frames");
            needsInitialTick = false;
            waitingForTickStep = true;

            MinecraftClient.getInstance().execute(() -> {
                // Prepare for tick advance (saves current state as previous)
                stateTracker.prepareTickAdvance();

                // Execute the initial tick
                CommandRunner.runCommand("/tick step 1");

                // Wait for tick to process - register as a waiter so we get notified when tick
                // step completes
                this.expectedTargetTick = -1; // initial tick does not drive currentGameTick
                TICK_WAITERS.add(this);
            });
            return;
        }

        // Mark that we're now rendering a frame
        renderingFrame = true;

        // Calculate which game tick we're in and the interpolation factor
        int targetTick = SubTickConfig.getTickAtTime(timeElapsed);
        float tickDelta = SubTickConfig.getTickDelta(timeElapsed);

        // Check if we need to advance to a new game tick BEFORE rendering this frame
        if (targetTick > currentGameTick) {
            // Clamp advancement so we only move at most one tick per render to avoid
            // burst-stepping
            int advanceTo = Math.min(targetTick, currentGameTick + 1);
            if (advanceTo < targetTick) {
                LOGGER.warn("Generator falling behind: requested tick {} but clamping to {} (current={})",
                        targetTick, advanceTo, currentGameTick);
            }

            // We need to advance the game tick BEFORE rendering this frame
            LOGGER.info(
                    "Need to advance from tick {} to tick {} (using {}) BEFORE rendering frame {} (time={}) - executing /tick step",
                    currentGameTick, targetTick, advanceTo, currentFrameNumber, timeElapsed);
            waitingForTickStep = true;
            renderingFrame = false; // Release the lock since we're not rendering yet

            // Execute tick step asynchronously and continue rendering after it completes
            final int finalAdvanceTo = advanceTo; // capture for lambda
            MinecraftClient.getInstance().execute(() -> {
                // STEP 1: Save current state as previous BEFORE the tick executes
                stateTracker.prepareTickAdvance();

                // STEP 2: Execute the tick step (world state changes)
                CommandRunner.runCommand("/tick step 1");

                // Wait for the tick to fully process - register as a waiter; set expected to
                // the clamped value
                this.expectedTargetTick = finalAdvanceTo;
                TICK_WAITERS.add(this);
            });
            return; // Don't continue rendering until tick has advanced
        }

        // Check if this is the last frame - render up to and including the final time
        long totalFrames = SubTickConfig.getTotalFrames(duration);
        boolean isLastFrame = currentFrameNumber >= totalFrames - 1;

        LOGGER.info("=== Rendering frame {}/{}: time={}, tick={}, delta={}, isLast={} ===",
                currentFrameNumber + 1, totalFrames, timeElapsed, targetTick, tickDelta, isLastFrame);

        // Create renderable area from positions
        AreaRenderable area = AreaRenderable.of(this.pos1, this.pos2);
        area.properties().scale.set(this.scale);
        area.properties().rotation.set(this.rotation);
        area.properties().slant.set(this.slant);

        // Create and schedule the frame with proper tickDelta
        FrameRenderScreen screen = new FrameRenderScreen(area, tickDelta, this.pos1, this.pos2);
        screen.setExportCallback(file -> {
            // Notify player of render progress
            String frameTime = String.format("%.3f", timeElapsed);
            String tickInfo = String.format("tick %d, delta %.2f", targetTick, tickDelta);
            this.source.sendFeedback(Text.literal(
                    "Frame %d/%d (%ss, %s) rendered"
                            .formatted(currentFrameNumber + 1, SubTickConfig.getTotalFrames(duration), frameTime,
                                    tickInfo))
                    .formatted(Formatting.YELLOW));

            // Periodic memory cleanup to prevent slowdown on long renders
            if (currentFrameNumber > 0 && currentFrameNumber % 100 == 0) {
                LOGGER.info("Performing memory cleanup at frame {}", currentFrameNumber);
                stateTracker.trimMemory();
            }

            // Queue file for batched move
            File newFile = ExportConfig.FRAME_EXPORT_DIR.resolve("frame_%05d.png".formatted(currentFrameNumber))
                    .toFile();
            pendingSources.add(file);
            pendingDests.add(newFile);

            // Check if we should perform batched moves
            if (pendingSources.size() >= 500 || isLastFrame) {
                for (int i = 0; i < pendingSources.size(); i++) {
                    try {
                        Files.move(pendingSources.get(i).toPath(), pendingDests.get(i).toPath(),
                                StandardCopyOption.REPLACE_EXISTING);
                        LOGGER.info("Frame moved to {} - COMPLETE", pendingDests.get(i).getAbsolutePath());
                    } catch (Exception e) {
                        LOGGER.error("Failed to move frame to {}", pendingDests.get(i).getAbsolutePath(), e);
                    }
                }
                pendingSources.clear();
                pendingDests.clear();
            }

            // Stop if this was the last frame, otherwise continue to next frame
            if (isLastFrame) {
                renderingFrame = false; // Clear the lock before stopping
                // Instead of calling stop() here, set a flag to stop after export is fully
                // complete
                pendingStop = true;
            } else {
                // Move to next frame time
                this.timeElapsed += SubTickConfig.getFrameInterval();
                this.currentFrameNumber++;

                // Calculate next frame's tick to log transition info
                int nextFrameTick = SubTickConfig.getTickAtTime(timeElapsed);
                if (nextFrameTick > targetTick) {
                    LOGGER.info("Next frame {} will be in tick {} (current was tick {})",
                            currentFrameNumber, nextFrameTick, targetTick);
                }

                // Clear the rendering flag BEFORE scheduling next frame
                renderingFrame = false;

                // Render next frame immediately - tick advancement is now handled at the start
                // of renderNextFrame
                MinecraftClient.getInstance().execute(this::renderNextFrame);
            }
        });
        // Schedule the screen and hook into its close event
        MinecraftClient.getInstance().execute(() -> {
            ScreenScheduler.schedule(screen);
            // Monitor for screen close to perform stop if needed
            new Thread(() -> {
                while (!screen.isClosed()) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ignored) {
                    }
                }
                if (pendingStop) {
                    MinecraftClient.getInstance().execute(this::stop);
                }
            }).start();
        });
    }

    private void initExportDir() {
        // Clear previous frames from export directory if it exists otherwise create it
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
}
