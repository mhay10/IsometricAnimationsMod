package com.isoanimations.mixin.client;

import com.isoanimations.manager.AnimationManager;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {
    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private <E extends Entity> void shouldRenderMixin(E entity, Frustum culler, double camX, double camY, double camZ, CallbackInfoReturnable<Boolean> cir) {
        // Filter entities if animation region active
        AABB activeRegion = AnimationManager.getActiveRegion();
        if (activeRegion != null) {
            // Return false if entity outside active region
            boolean insideRegion = activeRegion.contains(entity.getBoundingBox().getCenter());
            if (!insideRegion) {
                cir.setReturnValue(false);
            }
        }
    }
}
