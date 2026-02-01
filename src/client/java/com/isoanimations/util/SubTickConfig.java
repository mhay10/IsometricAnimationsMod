package com.isoanimations.util;

/**
 * Configuration for sub-tick frame generation.
 * Allows customization of frame rate and interpolation behavior.
 */
public class SubTickConfig {
    // Target frames per second (higher = smoother but slower to render)
    private static int targetFPS = 1000;

    // Time between frames in seconds
    private static double frameInterval = 1.0 / targetFPS;

    // Minecraft ticks per second (constant)
    public static final int TICKS_PER_SECOND = 20;

    // Seconds per tick
    public static final double SECONDS_PER_TICK = 1.0 / TICKS_PER_SECOND;

    /**
     * Set the target frame rate for animation capture
     */
    public static void setTargetFPS(int fps) {
        if (fps <= 0 || fps > 1000) {
            throw new IllegalArgumentException("FPS must be between 1 and 1000");
        }
        targetFPS = fps;
        frameInterval = 1.0 / fps;
    }

    public static int getTargetFPS() {
        return targetFPS;
    }

    public static double getFrameInterval() {
        return frameInterval;
    }

    /**
     * Calculate which game tick a given time falls into
     */
    public static int getTickAtTime(double timeInSeconds) {
        return (int) Math.floor(timeInSeconds / SECONDS_PER_TICK);
    }

    /**
     * Calculate the tickDelta (interpolation factor) for a given time
     * @param timeInSeconds The current time in the animation
     * @return Value between 0.0 and 1.0 representing position within the current tick
     */
    public static float getTickDelta(double timeInSeconds) {
        double tickProgress = (timeInSeconds % SECONDS_PER_TICK) / SECONDS_PER_TICK;
        return (float) tickProgress;
    }

    /**
     * Calculate total number of frames for a given duration
     */
    public static long getTotalFrames(double durationInSeconds) {
        return Math.round(durationInSeconds * targetFPS);
    }

    /**
     * Calculate total number of game ticks for a given duration
     */
    public static int getTotalTicks(double durationInSeconds) {
        return (int) Math.ceil(durationInSeconds / SECONDS_PER_TICK);
    }
}

