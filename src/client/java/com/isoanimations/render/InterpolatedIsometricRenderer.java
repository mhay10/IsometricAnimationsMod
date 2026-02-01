package com.isoanimations.render;

import com.glisco.isometricrenders.render.AreaRenderable;
import com.glisco.isometricrenders.render.RenderableDispatcher;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.*;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

import java.util.function.Consumer;

import static com.isoanimations.IsometricAnimations.LOGGER;

/**
 * Custom renderer that applies interpolated block positions during rendering.
 * Wraps the isometric-renders library and activates interpolation context.
 */
public class InterpolatedIsometricRenderer {

    /**
     * Render an area into a NativeImage with interpolated block positions
     */
    public static NativeImage renderWithInterpolation(
            AreaRenderable renderable,
            float tickDelta,
            int size
    ) {
        InterpolatedRenderContext context = InterpolatedRenderContext.get();
        MinecraftClient client = MinecraftClient.getInstance();

        try {
            // Activate interpolation context
            context.activate(tickDelta);

            LOGGER.info("Rendering frame with tickDelta={}", tickDelta);

            // NUCLEAR OPTION: Directly manipulate piston states since isometric-renders
            // doesn't use getProgress() - it just renders static block models
            if (client.world != null) {
                AnimationStateTracker tracker = AnimationStateTracker.getInstance();

                if (tracker.isTracking()) {
                    // Apply interpolation to ALL tracked positions that have PistonBlockEntity
                    for (BlockPos pos : tracker.getTrackedPositions()) {
                        var blockEntity = client.world.getBlockEntity(pos);

                        if (blockEntity instanceof net.minecraft.block.entity.PistonBlockEntity) {
                            PistonStateManipulator.applyInterpolation(client.world, pos, tickDelta);
                        }
                    }
                }
            }

            // Let the isometric-renders library render
            NativeImage result = RenderableDispatcher.drawIntoImage(renderable, tickDelta, size);

            LOGGER.info("Frame rendered successfully");

            return result;

        } finally {
            // Restore original piston states
            if (client.world != null) {
                PistonStateManipulator.restoreAll(client.world);
            }

            // Deactivate context
            context.deactivate();
        }
    }

    /**
     * Render into the active framebuffer with interpolation
     */
    public static void renderIntoFramebufferWithInterpolation(
            AreaRenderable renderable,
            float aspectRatio,
            float tickDelta,
            Consumer<Matrix4fStack> matrixTransform
    ) {
        InterpolatedRenderContext context = InterpolatedRenderContext.get();

        try {
            // Activate interpolation context
            context.activate(tickDelta);

            // Let the isometric-renders library do the rendering
            RenderableDispatcher.drawIntoActiveFramebuffer(
                renderable,
                aspectRatio,
                tickDelta,
                matrixTransform
            );

        } finally {
            // Always deactivate context
            context.deactivate();
        }
    }
}

