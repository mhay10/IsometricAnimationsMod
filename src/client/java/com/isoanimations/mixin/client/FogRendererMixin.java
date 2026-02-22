package com.isoanimations.mixin.client;

import com.isoanimations.util.AnimationManager;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.world.phys.AABB;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FogRenderer.class)
public class FogRendererMixin {
    @Inject(method = "computeFogColor", at = @At("HEAD"), cancellable = true)
    private static void setupFog(CallbackInfoReturnable<Vector4f> cir) {
        // Set fog color to black when animation region active
        AABB activeRegion = AnimationManager.getActiveRegion();
        if (activeRegion != null) {
            cir.setReturnValue(new Vector4f(0, 0, 0, 1));
        }
    }
}
