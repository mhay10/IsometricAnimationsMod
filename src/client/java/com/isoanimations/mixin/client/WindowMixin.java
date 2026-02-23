package com.isoanimations.mixin.client;

import com.isoanimations.util.AnimationManager;
import com.isoanimations.util.FrameManager;
import com.mojang.blaze3d.TracyFrameCapture;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Window.class)
public class WindowMixin {
    @Inject(method = "updateDisplay", at = @At("HEAD"))
    private void updateDisplay(TracyFrameCapture tracyFrameCapture, CallbackInfo ci) {
        // Capture frame when animation region active
        AABB activeRegion = AnimationManager.getActiveRegion();
        if (activeRegion != null) {
            FrameManager.captureFrame();
        }
    }
}
