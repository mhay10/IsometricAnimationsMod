package com.isoanimations.mixin.client;

import com.isoanimations.render.AnimationStateTracker;
import com.isoanimations.render.BlockAnimationState;
import net.minecraft.block.Block;
import net.minecraft.block.PistonBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.PistonBlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PistonBlockEntity.class)
public abstract class PistonBlockEntityMixin {

    @Shadow
    private float progress;

    @Shadow
    private float lastProgress;

    @Shadow
    private boolean extending;

    @Shadow
    public abstract Direction getMovementDirection();

    private static boolean isStickyPiston(Block block) {
        return block instanceof PistonBlock && block.getTranslationKey().equals("block.minecraft.sticky_piston");
    }

    /**
     * Capture piston animation state at the START of tick to preserve "before" state
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private static void onPistonTickStart(World world, BlockPos pos, BlockState state, PistonBlockEntity blockEntity, CallbackInfo ci) {
        AnimationStateTracker tracker = AnimationStateTracker.getInstance();
        if (!tracker.isTracking()) return;

        float progress = blockEntity.getProgress(1.0f);
        Direction facing = blockEntity.getMovementDirection();
        boolean isSticky = isStickyPiston(state.getBlock());

        Vec3d offset = new Vec3d(
            facing.getOffsetX() * progress,
            facing.getOffsetY() * progress,
            facing.getOffsetZ() * progress
        );

        BlockAnimationState animState = new BlockAnimationState(
            pos,
            state,
            offset,
            progress,
            BlockAnimationState.AnimationType.PISTON,
            isSticky
        );

        tracker.recordBlockState(pos, animState, 0.0f); // HEAD = sub-tick 0.0
    }

    /**
     * Capture piston animation state at the END of tick to get updated state
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private static void onPistonTickEnd(World world, BlockPos pos, BlockState state, PistonBlockEntity blockEntity, CallbackInfo ci) {
        AnimationStateTracker tracker = AnimationStateTracker.getInstance();
        if (!tracker.isTracking()) return;

        float progress = blockEntity.getProgress(1.0f);
        Direction facing = blockEntity.getMovementDirection();
        boolean isSticky = isStickyPiston(state.getBlock());

        Vec3d offset = new Vec3d(
            facing.getOffsetX() * progress,
            facing.getOffsetY() * progress,
            facing.getOffsetZ() * progress
        );

        BlockAnimationState animState = new BlockAnimationState(
            pos,
            state,
            offset,
            progress,
            BlockAnimationState.AnimationType.PISTON,
            isSticky
        );

        tracker.recordBlockState(pos, animState, 1.0f); // TAIL = sub-tick 1.0
    }
}
