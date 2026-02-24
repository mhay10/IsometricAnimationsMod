package com.isoanimations.manager;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL21;

import java.nio.ByteBuffer;

public class FrameManager {
    private static int lastWidth = 0;
    private static int lastHeight = 0;

    private static final int[] pbos = new int[2];
    private static int pboIndex = 0;
    private static boolean isFirstFrame = true;

    public static void initPBOs(int width, int height) {
        // Initialize PBOs with empty data
        int dataSize = width * height * 4; // RGBA
        for (int i = 0; i < pbos.length; i++) {
            pbos[i] = GL15.glGenBuffers();
            GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, pbos[i]);
            GL15.glBufferData(GL21.GL_PIXEL_PACK_BUFFER, dataSize, GL15.GL_STREAM_READ);
        }

        // Unbind PBOs
        GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, 0);
    }

    public static void captureFrame() {
        // Get current window dimensions
        Window window = Minecraft.getInstance().getWindow();
        int width = window.getWidth();
        int height = window.getHeight();

        // Reinitialize PBOs if dimensions changed
        if (width != lastWidth || height != lastHeight) {
            initPBOs(width, height);
        }

        // Update PBO indexes
        pboIndex = (pboIndex + 1) % pbos.length;
        int nextIndex = (pboIndex + 1) % pbos.length;

        // Bind current PBO and read pixels into it
        GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, pbos[pboIndex]);
        GL11.glReadPixels(0, 0, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, 0);

        // Bind PBO for reading the previous frame's data
        if (!isFirstFrame) {
            GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, pbos[nextIndex]);

            // Map buffer to CPU memory
            ByteBuffer buffer = GL15.glMapBuffer(GL21.GL_PIXEL_PACK_BUFFER, GL15.GL_READ_ONLY, null);
            if (buffer != null) {
                // Copy data from buffer
                byte[] frameData = new byte[width * height * 4];
                buffer.get(frameData);

                // Unmap buffer after reading
                GL15.glUnmapBuffer(GL21.GL_PIXEL_PACK_BUFFER);

                // Process frame data in separate thread
                FrameExportManager.queueFrameExport(frameData, width, height);
            }
        }

        // Prepare for next frame capture
        GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, 0); // Unbind PBO
        isFirstFrame = false;
        lastWidth = width;
        lastHeight = height;
    }
}
