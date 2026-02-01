package com.isoanimations.screens;

import com.glisco.isometricrenders.render.AreaRenderable;
import com.glisco.isometricrenders.util.ImageIO;
import com.isoanimations.render.EntityAnimationTracker;
import com.isoanimations.render.InterpolatedIsometricRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.Window;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.chunk.WorldChunk;

import java.io.File;
import java.util.function.Consumer;

import static com.isoanimations.IsometricAnimations.LOGGER;

public class FrameRenderScreen extends Screen {
    private final AreaRenderable renderable;
    private Consumer<File> exportCallback = (file) -> {
    };

    // Render settings
    private final float tickDelta;

    // World area to ensure is loaded
    private final BlockPos pos1;
    private final BlockPos pos2;

    // Export settings
    private boolean alreadyExported = false;
    private boolean exportComplete = false;
    private final int exportSize = 1000;
    private int renderCounter = 0;
    private static final int FRAMES_BEFORE_EXPORT = 3; // Wait 3 render frames to ensure world is ready
    private boolean worldLoaded = false;
    private volatile boolean closed = false;

    public FrameRenderScreen(AreaRenderable renderable) {
        this(renderable, 0.5f);
    }

    public FrameRenderScreen(AreaRenderable renderable, float tickDelta) {
        this(renderable, tickDelta, null, null);
    }

    public FrameRenderScreen(AreaRenderable renderable, float tickDelta, BlockPos pos1, BlockPos pos2) {
        super(Text.literal("Frame Render"));
        this.renderable = renderable;
        this.tickDelta = tickDelta;
        this.pos1 = pos1;
        this.pos2 = pos2;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Ensure world is loaded for this frame
        ensureWorldLoaded();

        // Clear background
        this.renderInGameBackground(context);
        context.draw();

        // Render the area first to ensure it's loaded
        Window window = client.getWindow();
        float aspectRatio = (float) window.getFramebufferWidth() / (float) window.getFramebufferHeight();
        InterpolatedIsometricRenderer.renderIntoFramebufferWithInterpolation(
                renderable,
                aspectRatio,
                tickDelta,
                matrixStack -> {
                });

        // Wait for a few render frames to ensure everything is properly loaded/rendered
        renderCounter++;

        if (!alreadyExported && renderCounter >= FRAMES_BEFORE_EXPORT && worldLoaded) {
            alreadyExported = true;

            LOGGER.info("Rendering frame with interpolation at tickDelta={}", tickDelta);

            ImageIO.save(
                    InterpolatedIsometricRenderer.renderWithInterpolation(this.renderable, tickDelta, exportSize),
                    this.renderable.exportPath()).whenComplete((file, throwable) -> {
                        exportCallback.accept(file);
                        exportComplete = true;
                    });
        }

        // Close the screen when export is complete
        if (exportComplete) {
            client.execute(this::close);
        }
    }

    /**
     * Ensures all chunks in the renderable area are loaded and up-to-date before
     * rendering.
     * This is called every frame to make sure the world state is fully
     * synchronized.
     *
     * This method actively forces the world to update by:
     * 1. Verifying all chunks in the area are loaded
     * 2. Forcing block state updates
     * 3. Updating block entities in the area
     * 4. Forcing the world renderer to update the area
     */
    private void ensureWorldLoaded() {
        if (client == null || client.world == null) {
            worldLoaded = false;
            return;
        }

        // If positions weren't provided, we can't verify chunk loading
        if (pos1 == null || pos2 == null) {
            worldLoaded = true;
            return;
        }

        // Get the bounding box of the renderable area
        BlockPos minPos = new BlockPos(
                Math.min(pos1.getX(), pos2.getX()),
                Math.min(pos1.getY(), pos2.getY()),
                Math.min(pos1.getZ(), pos2.getZ()));
        BlockPos maxPos = new BlockPos(
                Math.max(pos1.getX(), pos2.getX()),
                Math.max(pos1.getY(), pos2.getY()),
                Math.max(pos1.getZ(), pos2.getZ()));

        // Calculate chunk positions
        int minChunkX = minPos.getX() >> 4;
        int minChunkZ = minPos.getZ() >> 4;
        int maxChunkX = maxPos.getX() >> 4;
        int maxChunkZ = maxPos.getZ() >> 4;

        // First pass: Verify all chunks are loaded
        boolean allChunksLoaded = true;
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                WorldChunk chunk = client.world.getChunk(chunkX, chunkZ);

                if (chunk == null) {
                    allChunksLoaded = false;
                    LOGGER.warn("Chunk at ({}, {}) is not loaded", chunkX, chunkZ);
                }
            }
        }

        // Second pass: Force block and block entity updates in the entire area
        // This ensures all blocks and their states are synchronized with the current
        // game state
        if (allChunksLoaded) {
            int blocksUpdated = 0;
            int blockEntitiesUpdated = 0;

            // Iterate through all positions in the area
            for (BlockPos pos : BlockPos.iterate(minPos, maxPos)) {
                try {
                    // Force access to block state - this ensures the world fetches current state
                    client.world.getBlockState(pos);
                    blocksUpdated++;

                    // Update block entities if present - critical for pistons and other animated
                    // blocks
                    var blockEntity = client.world.getBlockEntity(pos);
                    if (blockEntity != null) {
                        // Mark the block entity as changed to force re-render
                        blockEntity.markDirty();
                        blockEntitiesUpdated++;
                    }
                } catch (Exception e) {
                    // Silently continue - some blocks might not be accessible
                }
            }

            if (renderCounter == 0) {
                LOGGER.info("Forced world updates: {} blocks accessed, {} block entities updated",
                        blocksUpdated, blockEntitiesUpdated);
            }
        }

        worldLoaded = allChunksLoaded;

        if (worldLoaded && renderCounter == 0) {
            LOGGER.info("World loaded and updated successfully for renderable area");
        } else if (!worldLoaded) {
            LOGGER.warn("World not fully loaded - some chunks are missing");
        }
    }

    public void setExportCallback(Consumer<File> callback) {
        this.exportCallback = callback;
    }

    @Override
    public void close() {
        // Stop entity tracking when screen closes
        EntityAnimationTracker.getInstance().stopTracking();
        closed = true;
        super.close();
    }

    public boolean isClosed() {
        return closed;
    }
}
