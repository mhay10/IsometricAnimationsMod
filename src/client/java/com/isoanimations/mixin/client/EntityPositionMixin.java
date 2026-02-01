package com.isoanimations.mixin.client;

import com.isoanimations.render.EntityAnimationTracker;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * Mixin to track entity position changes for smooth interpolation
 */
@Mixin(Entity.class)
public abstract class EntityPositionMixin {

    @Shadow
    public abstract Vec3d getPos();

    @Shadow
    public abstract Vec3d getVelocity();

    @Shadow
    public abstract UUID getUuid();

    /**
     * Track entity position after it has been updated
     */
    @Inject(method = "tick", at = @At("RETURN"))
    private void onEntityTick(CallbackInfo ci) {
        EntityAnimationTracker tracker = EntityAnimationTracker.getInstance();
        if (tracker.isTracking()) {
            tracker.recordEntityPosition(getUuid(), getPos(), getVelocity());
        }
    }

    /**
     * Track entity position when it's explicitly set (e.g., by pistons)
     */
    @Inject(method = "setPosition(DDD)V", at = @At("RETURN"))
    private void onSetPosition(double x, double y, double z, CallbackInfo ci) {
        EntityAnimationTracker tracker = EntityAnimationTracker.getInstance();
        if (tracker.isTracking()) {
            tracker.recordEntityPosition(getUuid(), new Vec3d(x, y, z), getVelocity());
        }
    }

    /**
     * Track entity position when set via Vec3d
     */
    @Inject(method = "setPosition(Lnet/minecraft/util/math/Vec3d;)V", at = @At("RETURN"))
    private void onSetPositionVec(Vec3d pos, CallbackInfo ci) {
        EntityAnimationTracker tracker = EntityAnimationTracker.getInstance();
        if (tracker.isTracking()) {
            tracker.recordEntityPosition(getUuid(), pos, getVelocity());
        }
    }
}

