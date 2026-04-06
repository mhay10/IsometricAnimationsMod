package com.isoanimations.manager;

import com.isoanimations.util.BufferPool;
import com.isoanimations.util.ExportFrame;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;

import static com.isoanimations.IsometricAnimations.LOGGER;

public class FrameCaptureManager {
    private static final int QUEUE_SIZE = 3;
    private static final Queue<QueuedFrame> frameQueue = new LinkedList<>();

    private record QueuedFrame(int textureId, long captureTime) {
    }

    public static void captureFrame(int width, int height) {
        // Generate texture for this frame
        int textureId = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);

        // Allocate VRAM for texture
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB, width, height, 0, GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);

        // Copy framebuffer into texture
        GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, width, height);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

        // Add to queue
        long captureTime = System.nanoTime() / 1000;
        frameQueue.add(new QueuedFrame(textureId, captureTime));

        // Process oldest frame if queue is full
        if (frameQueue.size() >= QUEUE_SIZE) {
            processOldestFrame(width, height);
        }
    }

//    public static void captureFrame() {
//        Window window = Minecraft.getInstance().getWindow();
//        int width = window.getWidth();
//        int height = window.getHeight();
//
//        // Block until buffer available
//        ByteBuffer frameData = BufferPool.getBufferBlocking();
//        if (frameData != null) {
//            try {
//                // Set frame buffer properties
//                int frameSize = width * height * 3; // BGR format
//                frameData.clear();
//                frameData.limit(frameSize);
//
//                // Read pixels into framebuffer
//                GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1); // Pack with 1 byte alignment + no padding
//                GL11.glReadPixels(0, 0, width, height, GL15.GL_BGR, GL11.GL_UNSIGNED_BYTE, frameData);
//                GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 4); // Restore default unpack alignment
//
//                // Queue frame for encoding
//                long captureTime = System.nanoTime() / 1000;
//                VideoStreamManager.addFrameToQueue(new ExportFrame(frameData, captureTime));
//            } catch (Exception e) {
//                LOGGER.error("Failed to capture frame", e);
//                BufferPool.returnBuffer(frameData);
//            }
//        }
//    }

    private static void processOldestFrame(int width, int height) {
        QueuedFrame oldest = frameQueue.poll();
        if (oldest != null) {
            ByteBuffer frameData = BufferPool.getBufferBlocking();
            if (frameData != null) {
                try {
                    // Initialize frame buffer
                    frameData.clear();
                    frameData.limit(width * height * 3);

                    // Copy texture to frame buffer
                    GL11.glBindTexture(GL11.GL_TEXTURE_2D, oldest.textureId());
                    GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, frameData);

                    // Send frame data for encoding
                    VideoStreamManager.addFrameToQueue(new ExportFrame(frameData, oldest.captureTime()));
                } catch (Exception e) {
                    BufferPool.returnBuffer(frameData);
                }
            }

            // Clean up texture
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
            GL11.glDeleteTextures(oldest.textureId());
        }
    }

    private static void flushQueue(int width, int height) {
        while (!frameQueue.isEmpty()) {
            processOldestFrame(width, height);
        }
    }
}
