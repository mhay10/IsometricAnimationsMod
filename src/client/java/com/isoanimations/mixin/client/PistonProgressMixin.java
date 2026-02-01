package com.isoanimations.mixin.client;

import com.isoanimations.render.InterpolatedRenderContext;
import net.minecraft.block.entity.PistonBlockEntity;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to override piston progress calculation for interpolated rendering
 */
@Mixin(PistonBlockEntity.class)
public abstract class PistonProgressMixin {

    @Shadow
    private float progress;

    @Shadow
    private float lastProgress;

    @Shadow
    public abstract Direction getMovementDirection();

    /**
     * Override getProgress to return interpolated value during rendering
     */
    @Inject(method = "getProgress", at = @At("HEAD"), cancellable = true)
    public void onGetProgress(float tickDelta, CallbackInfoReturnable<Float> cir) {
        InterpolatedRenderContext context = InterpolatedRenderContext.get();

        // Only override during our offscreen/interpolated rendering.
        // Outside of that, vanilla behavior should apply.
        if (!context.isActive()) {
            return;
        }

        // Use the context's tickDelta for interpolation
        float contextDelta = context.getTickDelta();
        float interpolatedProgress = this.lastProgress + (this.progress - this.lastProgress) * contextDelta;

        // Clamp to valid range [0, 1] to prevent glitches
        interpolatedProgress = Math.max(0.0f, Math.min(1.0f, interpolatedProgress));

        // DEBUG: Detailed logging (disable if too spammy)
        System.out.println(String.format(
            "[PistonProgressMixin] Interpolating: lastProgress=%.4f, progress=%.4f, contextDelta=%.4f → result=%.4f (diff=%.4f)",
            this.lastProgress, this.progress, contextDelta, interpolatedProgress, (this.progress - this.lastProgress)
        ));

        cir.setReturnValue(interpolatedProgress);
    }
}