package com.isoanimations.mixin.client;

import com.isoanimations.animation.AnimationController;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    @Shadow
    @Final
    public ObjectArrayList<SectionRenderDispatcher.RenderSection> visibleSections;

    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void renderLevel(GraphicsResourceAllocator graphicsResourceAllocator,
                             DeltaTracker deltaTracker,
                             boolean bl,
                             Camera camera,
                             Matrix4f matrix4f,
                             Matrix4f matrix4f2,
                             Matrix4f matrix4f3,
                             GpuBufferSlice gpuBufferSlice,
                             Vector4f vector4f,
                             boolean bl2,
                             CallbackInfo ci) {
        // Filter visible sections to only ones in animation region
        var region = AnimationController.getActiveRegion();
        if (region != null) {
            visibleSections.removeIf(section -> {
                // Create bounding box for section
                var origin = section.getRenderOrigin();
                int maxX = origin.getX() + 16;
                int maxY = origin.getY() + 16;
                int maxZ = origin.getZ() + 16;
                var sectionBox = new BoundingBox(origin.getX(), origin.getY(), origin.getZ(), maxX, maxY, maxZ);

                // Check if section intersects with animation region
                return !region.intersects(sectionBox);
            });
        }
    }
}