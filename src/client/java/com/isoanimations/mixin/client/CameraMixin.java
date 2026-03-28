package com.isoanimations.mixin.client;

import com.isoanimations.manager.CameraManager;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    protected abstract void setPosition(Vec3 vec3);

    @Shadow
    protected abstract void setRotation(float f, float g);

    @Inject(method = "setup", at = @At("TAIL"))
    private void setup(Level level, Entity entity, boolean detached, boolean mirror, float a, CallbackInfo ci) {
        if (CameraManager.isDetached()) {
            var pos = CameraManager.getPosition();
            var xRot = CameraManager.getXRot();
            var yRot = CameraManager.getYRot();

            this.setPosition(pos);
            this.setRotation(yRot, xRot);
        }
    }
}
