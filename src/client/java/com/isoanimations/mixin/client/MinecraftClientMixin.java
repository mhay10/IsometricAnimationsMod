package com.isoanimations.mixin.client;

import com.isoanimations.screens.FrameRenderScreen;
import com.isoanimations.util.AnimationFrameGenerator;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        Screen currentScreen = MinecraftClient.getInstance().currentScreen;
        if (currentScreen instanceof FrameRenderScreen) {
            if (AnimationFrameGenerator.isAnyRunning()) {
                // Prevent pausing on lost focus by setting pauseOnLostFocus to false
                MinecraftClient.getInstance().options.pauseOnLostFocus = false;
            } else {
                // Restore default behavior
                MinecraftClient.getInstance().options.pauseOnLostFocus = true;
            }
        }
    }
}
