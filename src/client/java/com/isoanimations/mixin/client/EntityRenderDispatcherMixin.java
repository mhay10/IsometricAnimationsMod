package com.isoanimations.mixin.client;

import com.isoanimations.render.EntityAnimationTracker;
import com.isoanimations.render.InterpolatedRenderContext;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

import static com.isoanimations.IsometricAnimations.LOGGER;

/**
 * Mixin to apply smooth interpolation to entity rendering
 */
@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {

    @Unique
    private final ThreadLocal<Boolean> isometricAnimations$shouldPop = ThreadLocal.withInitial(() -> false);

    /**
     * Apply smooth interpolation to entity rendering by adjusting the render position
     */
    @Inject(
        method = "render",
        at = @At("HEAD")
    )
    private <E extends Entity> void applyEntityInterpolation(
        E entity,
        double x,
        double y,
        double z,
        float tickDelta,
        MatrixStack matrices,
        VertexConsumerProvider vertexConsumers,
        int light,
        CallbackInfo ci
    ) {
        EntityAnimationTracker tracker = EntityAnimationTracker.getInstance();
        InterpolatedRenderContext renderContext = InterpolatedRenderContext.get();

        // Reset the flag
        isometricAnimations$shouldPop.set(false);

        // Only apply if we're in an interpolated render context and tracking entities
        if (!tracker.isTracking() || !renderContext.isActive()) {
            return;
        }

        UUID entityId = entity.getUuid();
        if (!tracker.isEntityTracked(entityId)) {
            return;
        }

        // Get the interpolated position based on our custom tick delta
        float customTickDelta = renderContext.getTickDelta();
        Vec3d interpolatedPos = tracker.getInterpolatedPosition(entityId, customTickDelta);
        if (interpolatedPos == null) {
            return;
        }

        // Get the default Minecraft interpolated position (what it would render at normally)
        Vec3d defaultLerpedPos = entity.getLerpedPos(tickDelta);

        // Calculate the offset we need to apply to move the entity from its default
        // interpolated position to our custom interpolated position
        double offsetX = interpolatedPos.x - defaultLerpedPos.x;
        double offsetY = interpolatedPos.y - defaultLerpedPos.y;
        double offsetZ = interpolatedPos.z - defaultLerpedPos.z;

        // Debug logging for entities with significant movement
        double offsetMagnitude = Math.sqrt(offsetX * offsetX + offsetY * offsetY + offsetZ * offsetZ);
        if (offsetMagnitude > 0.01) {
            LOGGER.info("[EntityInterpolation] Entity {}: customDelta={}, defaultDelta={}, offset=({}, {}, {}), magnitude={}",
                entity.getType().getName().getString(), customTickDelta, tickDelta,
                String.format("%.3f", offsetX), String.format("%.3f", offsetY), String.format("%.3f", offsetZ),
                String.format("%.3f", offsetMagnitude));
        }

        // Only apply offset if it's significant (avoid floating point errors)
        if (Math.abs(offsetX) > 0.001 || Math.abs(offsetY) > 0.001 || Math.abs(offsetZ) > 0.001) {
            // Push matrix and apply our custom interpolation offset
            matrices.push();
            matrices.translate(offsetX, offsetY, offsetZ);
            isometricAnimations$shouldPop.set(true);
        }
    }

    /**
     * Pop the matrix we pushed after rendering is done
     */
    @Inject(
        method = "render",
        at = @At("RETURN")
    )
    private <E extends Entity> void cleanupEntityInterpolation(
        E entity,
        double x,
        double y,
        double z,
        float tickDelta,
        MatrixStack matrices,
        VertexConsumerProvider vertexConsumers,
        int light,
        CallbackInfo ci
    ) {
        // Only pop if we pushed earlier
        if (isometricAnimations$shouldPop.get()) {
            matrices.pop();
            isometricAnimations$shouldPop.set(false);
        }
    }
}

