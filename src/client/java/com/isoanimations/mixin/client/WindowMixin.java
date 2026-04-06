package com.isoanimations.mixin.client;

import com.isoanimations.manager.AnimationManager;
import com.isoanimations.manager.FrameCaptureManager;
import com.mojang.blaze3d.TracyFrameCapture;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Window.class)
public class WindowMixin {
    @Shadow
    private int width;

    @Shadow
    private int height;

    @Inject(method = "updateDisplay", at = @At("HEAD"))
    private void updateDisplay(TracyFrameCapture tracyFrameCapture, CallbackInfo ci) {
        // Capture frame when animation region active
        AABB activeRegion = AnimationManager.getActiveRegion();
        if (activeRegion != null && AnimationManager.isAnimating()) {
            FrameCaptureManager.captureFrame(this.width, this.height);
        }
    }
}
