package com.isoanimations.mixin.client;

import com.isoanimations.manager.AnimationManager;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockEntityRenderDispatcher.class)
public class BlockEntityRenderDispatcherMixin {
    @Inject(method = "tryExtractRenderState", at = @At("HEAD"), cancellable = true)
    private <E extends BlockEntity> void tryExtractRenderState(E blockEntity, float partialTicks, CrumblingOverlay breakProgress, CallbackInfoReturnable<BlockEntityRenderState> cir) {
        // Filter block entities if animation region active
        AABB activeRegion = AnimationManager.getActiveRegion();
        if (activeRegion != null) {
            // Return NULL if block entity outside active region
            boolean insideRegion = activeRegion.contains(blockEntity.getBlockPos().getCenter());
            if (!insideRegion) {
                cir.setReturnValue(null);
            }
        }
    }
}
