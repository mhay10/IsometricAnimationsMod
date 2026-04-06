package com.isoanimations.mixin.client.sodium;

import com.isoanimations.manager.AnimationManager;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
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

@Mixin(LevelSlice.class)
public class LevelSliceMixin {
    @Inject(method = "getBlockState(III)Lnet/minecraft/world/level/block/state/BlockState;", at = @At("HEAD"), cancellable = true)
    private void getBlockState(int blockX, int blockY, int blockZ, CallbackInfoReturnable<BlockState> cir) {
        // Filter blocks if animation region active
        if (AnimationManager.hasActiveRegion) {
            // Return AIR block if outside active region
            if (!AnimationManager.inActiveRegion(blockX, blockY, blockZ)) {
                cir.setReturnValue(Blocks.AIR.defaultBlockState());
            }
        }
    }

    @Inject(method = "getBlockEntity(III)Lnet/minecraft/world/level/block/entity/BlockEntity;", at = @At("HEAD"), cancellable = true)
    private void getBlockEntity(int blockX, int blockY, int blockZ, CallbackInfoReturnable<BlockEntity> cir) {
        // Filter block entities if animation region active
        if (AnimationManager.hasActiveRegion) {
            // Return null block entity if outside active region
            if (!AnimationManager.inActiveRegion(blockX, blockY, blockZ)) {
                cir.setReturnValue(null);
            }
        }
    }

    @Inject(method = "getFluidState", at = @At("HEAD"), cancellable = true)
    private void getFluidState(BlockPos pos, CallbackInfoReturnable<FluidState> cir) {
        // Filter fluids if animation region active
        if (AnimationManager.hasActiveRegion) {
            // Return empty fluid state if outside active region
            if (!AnimationManager.inActiveRegion(pos.getX(), pos.getY(), pos.getZ())) {
                cir.setReturnValue(Fluids.EMPTY.defaultFluidState());
            }
        }
    }
}