package com.isoanimations.render;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;

import static com.isoanimations.IsometricAnimations.LOGGER;

/**
 * Thread-local context for interpolated rendering.
 * When active, provides interpolated block positions for smooth animation.
 */
public class InterpolatedRenderContext {
    private static final ThreadLocal<InterpolatedRenderContext> INSTANCE = ThreadLocal.withInitial(InterpolatedRenderContext::new);

    private boolean active = false;
    private float currentTickDelta = 0.0f;
    private Map<BlockPos, Vec3d> positionOffsets = new HashMap<>();

    public static InterpolatedRenderContext get() {
        return INSTANCE.get();
    }

    /**
     * Activate interpolated rendering for the current thread
     */
    public void activate(float tickDelta) {
        this.active = true;
        this.currentTickDelta = tickDelta;
        this.positionOffsets.clear();

        LOGGER.debug("[InterpolatedRenderContext] ACTIVATED with tickDelta={}", tickDelta);

        // Populate position offsets from AnimationStateTracker
        AnimationStateTracker tracker = AnimationStateTracker.getInstance();
        if (tracker.isTracking()) {
            int offsetCount = 0;
            for (BlockPos pos : tracker.getTrackedPositions()) {
                BlockAnimationState state = tracker.getInterpolatedState(pos, tickDelta);
                if (state != null && !state.getOffset().equals(Vec3d.ZERO)) {
                    positionOffsets.put(pos, state.getOffset());
                    offsetCount++;
                }
            }
            LOGGER.debug("[InterpolatedRenderContext] Loaded {} position offsets", offsetCount);
        } else {
            LOGGER.warn("[InterpolatedRenderContext] Tracker NOT tracking!");
        }
    }

    /**
     * Deactivate interpolated rendering
     */
    public void deactivate() {
        this.active = false;
        this.currentTickDelta = 0.0f;
        this.positionOffsets.clear();
    }

    /**
     * Check if interpolated rendering is active
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Get the current tick delta
     */
    public float getTickDelta() {
        return currentTickDelta;
    }

    /**
     * Get the interpolated position offset for a block
     */
    public Vec3d getPositionOffset(BlockPos pos) {
        return positionOffsets.getOrDefault(pos, Vec3d.ZERO);
    }

    /**
     * Transform a block position to its interpolated position
     */
    public Vec3d transformPosition(BlockPos pos) {
        if (!active) {
            return new Vec3d(pos.getX(), pos.getY(), pos.getZ());
        }

        Vec3d offset = getPositionOffset(pos);
        return new Vec3d(
            pos.getX() + offset.x,
            pos.getY() + offset.y,
            pos.getZ() + offset.z
        );
    }

    /**
     * Transform a Vec3d position based on the nearest block position
     */
    public Vec3d transformPosition(Vec3d pos) {
        if (!active) {
            return pos;
        }

        BlockPos blockPos = BlockPos.ofFloored(pos);
        Vec3d offset = getPositionOffset(blockPos);
        return pos.add(offset);
    }
}