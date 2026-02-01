package com.isoanimations.render;

import net.minecraft.util.math.Box;

import static com.isoanimations.IsometricAnimations.LOGGER;

/**
 * Common lifecycle and area management for trackers that sample world state
 * across ticks.
 * Subclasses must implement how state is captured and how current is promoted
 * to previous.
 */
public abstract class AbstractTracker {
    protected Box trackingArea = null;
    private boolean tracking = false;

    /**
     * Start tracking the provided area. Clears any prior state and captures the
     * initial snapshot.
     */
    public void startTracking(Box area) {
        this.trackingArea = area;
        this.tracking = true;
        clearState();
        captureCurrentState();
        LOGGER.info("Started tracking area: {}", trackingArea);
    }

    /**
     * Stop tracking and clear stored state.
     */
    public void stopTracking() {
        this.tracking = false;
        this.trackingArea = null;
        clearState();
        LOGGER.info("Stopped tracking and cleared state");
    }

    /**
     * Called before the game tick advances. Subclasses should move current state ->
     * previous here.
     */
    public void prepareTickAdvance() {
        if (!tracking)
            return;
        saveCurrentToPrevious();
        LOGGER.debug("Prepared for tick advance");
    }

    /**
     * Called after the game tick advances. Subclasses should capture the new
     * current state here.
     */
    public void captureAfterTick() {
        if (!tracking)
            return;
        captureCurrentState();
        LOGGER.debug("Captured state after tick");
    }

    public boolean isTracking() {
        return tracking;
    }

    public Box getTrackingArea() {
        return trackingArea;
    }

    /** Clear all stored tracking state */
    protected abstract void clearState();

    /** Save the current state into the 'previous' storage */
    protected abstract void saveCurrentToPrevious();

    /** Capture the current state from the world */
    protected abstract void captureCurrentState();
}
