package com.isoanimations.manager;

import com.isoanimations.util.BufferPool;
import com.isoanimations.util.ExportFrame;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import java.nio.ByteBuffer;

import static com.isoanimations.IsometricAnimations.LOGGER;

public class FrameCaptureManager {
    public static void captureFrame() {
        Window window = Minecraft.getInstance().getWindow();
        int width = window.getWidth();
        int height = window.getHeight();

        // Block until buffer available
        ByteBuffer frameData = BufferPool.getBufferBlocking();
        if (frameData != null) {
            try {
                // Set frame buffer properties
                int frameSize = width * height * 3; // BGR format
                frameData.clear();
                frameData.limit(frameSize);

                // Read pixels into framebuffer
                GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1); // Pack with 1 byte alignment + no padding
                GL11.glReadPixels(0, 0, width, height, GL15.GL_BGR, GL11.GL_UNSIGNED_BYTE, frameData);
                GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 4); // Restore default unpack alignment

                // Queue frame for encoding
                long captureTime = System.nanoTime() / 1000;
                VideoStreamManager.addFrameToQueue(new ExportFrame(frameData, captureTime));
            } catch (Exception e) {
                LOGGER.error("Failed to capture frame", e);
                BufferPool.returnBuffer(frameData);
            }
        }
    }
}
