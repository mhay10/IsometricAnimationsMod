package com.isoanimations.mixin.client;

import com.isoanimations.util.AnimationManager;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    @Inject(method = "addCloudsPass", at = @At("HEAD"), cancellable = true)
    private void addCloudsPass(CallbackInfo ci) {
        // Disable clouds when animation region is active
        AABB activeRegion = AnimationManager.getActiveRegion();
        if (activeRegion != null) {
            ci.cancel();
        }
    }

    @Inject(method = "addWeatherPass", at = @At("HEAD"), cancellable = true)
    private void addWeatherPass(CallbackInfo ci) {
        // Disable weather when animation region is active
        AABB activeRegion = AnimationManager.getActiveRegion();
        if (activeRegion != null) {
            ci.cancel();
        }
    }

    @Inject(method = "addSkyPass", at = @At("HEAD"), cancellable = true)
    private void addSkyPass(CallbackInfo ci) {
        // Disable sky when animation region is active
        AABB activeRegion = AnimationManager.getActiveRegion();
        if (activeRegion != null) {
            ci.cancel();
        }
    }

    @Inject(method = "addParticlesPass", at = @At("HEAD"), cancellable = true)
    private void addParticlesPass(CallbackInfo ci) {
        // Disable particles when animation region is active
        AABB activeRegion = AnimationManager.getActiveRegion();
        if (activeRegion != null) {
            ci.cancel();
        }
    }
}