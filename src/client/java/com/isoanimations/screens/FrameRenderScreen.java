package com.isoanimations.screens;

import com.glisco.isometricrenders.render.AreaRenderable;
import com.glisco.isometricrenders.render.RenderableDispatcher;
import com.glisco.isometricrenders.util.ImageIO;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.Window;
import net.minecraft.text.Text;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static com.isoanimations.IsometricAnimations.LOGGER;

public class FrameRenderScreen extends Screen {
    private final AreaRenderable renderable;
    private Consumer<File> exportCallback = (file) -> {};

    // Render settings
    private float tickDelta = 0.5f;

    // Export settings
    private boolean alreadyExported = false;
    private boolean exportComplete = false;
    private boolean isReady = false;
    private int readyDelay = 10; // Wait 10 ticks before exporting
    private int tickCounter = 0;
    private int exportSize = 1000;

    public FrameRenderScreen(AreaRenderable renderable) {
        super(Text.literal("Frame Render"));
        this.renderable = renderable;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Clear background
        this.renderInGameBackground(context);
        context.draw();

        // Export the frame after a delay
        if (!isReady && !exportComplete) {
            tickCounter++;
            if (tickCounter >= readyDelay) {
                isReady = true;
            }
        } else if (isReady && !alreadyExported) {
            alreadyExported = true;

            ImageIO.save(
                    RenderableDispatcher.drawIntoImage(this.renderable, tickDelta, exportSize),
                    this.renderable.exportPath()
            ).whenComplete((file, throwable) -> {
                exportCallback.accept(file);
                exportComplete = true;
                isReady = false;
            });
        }

        // Render the area
        Window window = client.getWindow();
        float aspectRatio = (float) window.getFramebufferWidth() / (float) window.getFramebufferHeight();
        RenderableDispatcher.drawIntoActiveFramebuffer(
                renderable,
                aspectRatio,
                tickDelta,
                matrixStack -> {
                }
        );

        // Close the screen when export is complete
        if (exportComplete) {
            client.execute(this::close);
        }
    }

    public void setExportCallback(Consumer<File> callback) {
        this.exportCallback = callback;
    }
}
