package com.isoanimations.mixin.client;

import com.isoanimations.util.AnimationManager;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderSectionRegion.class)
public class RenderSectionRegionMixin {
    @Inject(method = "getBlockState", at = @At("HEAD"), cancellable = true)
    private void getBlockState(BlockPos pos, CallbackInfoReturnable<BlockState> cir) {
        // Filter blocks if animation region active
        AABB activeRegion = AnimationManager.getActiveRegion();
        if (activeRegion != null) {
            // Return AIR block if outside active region
            boolean insideRegion = activeRegion.contains(pos.getCenter());
            if (!insideRegion) {
                cir.setReturnValue(Blocks.AIR.defaultBlockState());
            }
        }
    }

    @Inject(method = "getBlockEntity", at = @At("HEAD"), cancellable = true)
    private void getBlockEntity(BlockPos pos, CallbackInfoReturnable<BlockEntity> cir) {
        // Filter block entities if animation region active
        AABB activeRegion = AnimationManager.getActiveRegion();
        if (activeRegion != null) {
            // Return null block entity if outside active region
            boolean insideRegion = activeRegion.contains(pos.getCenter());
            if (!insideRegion) {
                cir.setReturnValue(null);
            }
        }
    }

    @Inject(method = "getFluidState", at = @At("HEAD"), cancellable = true)
    private void getFluidState(BlockPos pos, CallbackInfoReturnable<FluidState> cir) {
        // Filter fluids if animation region active
        AABB activeRegion = AnimationManager.getActiveRegion();
        if (activeRegion != null) {
            // Return empty fluid state if outside active region
            boolean insideRegion = activeRegion.contains(pos.getCenter());
            if (!insideRegion) {
                cir.setReturnValue(Fluids.EMPTY.defaultFluidState());
            }
        }
    }
}
