package com.isoanimations.render;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

/**
 * Represents the animation state of a block at a specific point in time.
 * Used for interpolating block positions and states between game ticks.
 */
public class BlockAnimationState {
    private final BlockPos pos;
    private final BlockState blockState;
    private final Vec3d offset;
    private final float progress;
    private final AnimationType type;
    private final boolean isSticky;

    public enum AnimationType {
        NONE,           // Static block
        PISTON,         // Piston extension/retraction
        MOVING_BLOCK,   // Block being pushed/pulled by piston
        DOOR,           // Door opening/closing
        TRAPDOOR,       // Trapdoor opening/closing
        FENCE_GATE      // Fence gate opening/closing
    }

    public BlockAnimationState(BlockPos pos, BlockState blockState, Vec3d offset, float progress, AnimationType type, boolean isSticky) {
        this.pos = pos;
        this.blockState = blockState;
        this.offset = offset;
        this.progress = progress;
        this.type = type;
        this.isSticky = isSticky;
    }

    // Backward compatibility constructor
    public BlockAnimationState(BlockPos pos, BlockState blockState, Vec3d offset, float progress, AnimationType type) {
        this(pos, blockState, offset, progress, type, false);
    }

    public BlockPos getPos() {
        return pos;
    }

    public BlockState getBlockState() {
        return blockState;
    }

    public Vec3d getOffset() {
        return offset;
    }

    public float getProgress() {
        return progress;
    }

    public AnimationType getType() {
        return type;
    }

    public boolean isSticky() {
        return isSticky;
    }

    /**
     * Interpolate between two animation states
     */
    public static BlockAnimationState interpolate(BlockAnimationState from, BlockAnimationState to, float delta) {
        if (from == null) return to;
        if (to == null) return from;

        // Interpolate offset
        Vec3d interpolatedOffset = new Vec3d(
            lerp(from.offset.x, to.offset.x, delta),
            lerp(from.offset.y, to.offset.y, delta),
            lerp(from.offset.z, to.offset.z, delta)
        );

        // Interpolate progress
        float interpolatedProgress = lerp(from.progress, to.progress, delta);

        // Sticky property: use 'to' if changed, else 'from'
        boolean sticky = to.isSticky;

        return new BlockAnimationState(
            to.pos,
            to.blockState,
            interpolatedOffset,
            interpolatedProgress,
            to.type,
            sticky
        );
    }

    private static float lerp(float start, float end, float delta) {
        return start + (end - start) * delta;
    }

    private static double lerp(double start, double end, float delta) {
        return start + (end - start) * delta;
    }

    @Override
    public String toString() {
        return "BlockAnimationState{" +
                "pos=" + pos +
                ", type=" + type +
                ", offset=" + offset +
                ", progress=" + progress +
                ", isSticky=" + isSticky +
                '}';
    }

    public static BlockAnimationState stationary(BlockPos pos, BlockState blockState) {
        return new BlockAnimationState(pos, blockState, Vec3d.ZERO, 0.0f, AnimationType.NONE, false);
    }
}
