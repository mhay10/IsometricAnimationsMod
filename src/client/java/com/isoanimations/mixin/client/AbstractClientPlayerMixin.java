package com.isoanimations.mixin.client;

import com.isoanimations.manager.AnimationManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public class AbstractClientPlayerMixin {
    @Inject(method = "getFieldOfViewModifier", at = @At("HEAD"), cancellable = true)
    private void getFieldOfViewModifier(CallbackInfoReturnable<Float> cir) {
        // Set low FOV while animation region active
        AABB activeRegion = AnimationManager.getActiveRegion();
        if (activeRegion != null) {
            // Calculate FOV multiplier
            float targetFov = 20.0f;
            float baseFov = Minecraft.getInstance().options.fov().get().floatValue();
            cir.setReturnValue(targetFov / baseFov);
        }
    }
}
