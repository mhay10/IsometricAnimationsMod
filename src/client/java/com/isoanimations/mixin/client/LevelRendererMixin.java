package com.isoanimations.mixin.client;

import com.isoanimations.util.AnimationManager;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.joml.Matrix4f;
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
    private void renderLevel(CallbackInfo ci) {
        BoundingBox activeRegion = AnimationManager.getActiveRegion();

        // Filter sections to only ones intersecting active region
        visibleSections.removeIf(section -> {
            if (activeRegion != null) {
                // Create section bounding box
                BlockPos origin = section.getRenderOrigin();
                int sectionSize = 16; // Minecraft sections are 16x16x16
                BoundingBox sectionBox = new BoundingBox(
                        origin.getX(), origin.getY(), origin.getZ(),
                        origin.getX() + sectionSize, origin.getY() + sectionSize, origin.getZ() + sectionSize
                );

                // Remove section if does not intersect with active region
                return !activeRegion.intersects(sectionBox);
            }

            // If no active region, render all sections
            return false;
        });


    }
}
