package com.isoanimations.manager;

import com.isoanimations.config.PathConfig;
import com.isoanimations.util.BufferPool;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.isoanimations.IsometricAnimations.LOGGER;

public class FrameExportManager {
    private static final ExecutorService exportExecutor = Executors.newWorkStealingPool();
    private static final AtomicInteger pendingFrames = new AtomicInteger(0);
    private static int frameCounter = 0;

    public static void init() {
        // Ensure export directory exists
        if (!PathConfig.FRAME_EXPORT_DIR.toFile().exists()) {
            PathConfig.FRAME_EXPORT_DIR.toFile().mkdirs();
        }

        // Clear existing frames in export directory
        try {
            Files.walk(PathConfig.FRAME_EXPORT_DIR)
                    .filter(Files::isRegularFile)
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        } catch (Exception e) {
            LOGGER.error("Failed to clear export directory: ", e);
        }
    }

    public static void queueFrameExport(ByteBuffer frameData, int width, int height) {
        // Increment pending frame count
        int currentFrame = frameCounter++;
        pendingFrames.incrementAndGet();
        LOGGER.info("Queued frame {} for export ({} pending)", currentFrame, pendingFrames.get());

        // Submit export task to executor thread
        exportExecutor.submit(() -> {

            File frame = new File(PathConfig.FRAME_EXPORT_DIR.resolve("frame_%06d.tga".formatted(currentFrame)).toUri());
            try (FileOutputStream fos = new FileOutputStream(frame);
                 FileChannel channel = fos.getChannel()) {
                // Create TGA image header
                byte[] header = new byte[18];
                header[2] = 2; // Uncompressed true-color image
                header[12] = (byte) (width & 0xFF); // Width low byte
                header[13] = (byte) ((width >> 8) & 0xFF); // Width high byte
                header[14] = (byte) (height & 0xFF); // Height low byte
                header[15] = (byte) ((height >> 8) & 0xFF); // Height high byte
                header[16] = 24; // Bits per pixel
                header[17] = 0; // Image descriptor

                // Write header and image data to file
                channel.write(ByteBuffer.wrap(header));
                channel.write(frameData);
            } catch (Exception e) {
                LOGGER.error("Failed to export frame {}: {} (", currentFrame, e);
            } finally {
                // Return buffer to pool
                BufferPool.returnBuffer(frameData);

                // Decrement pending frame count after export completes
                pendingFrames.decrementAndGet();
                LOGGER.info("Frame {} export complete ({} pending)", currentFrame, pendingFrames.get());
            }
        });
    }

    public static void waitForExportFinish() {
        // Wait until all pending frames have been exported
        while (pendingFrames.get() > 0) {
            try {
                Thread.sleep(100);
            } catch (Exception ignored) {
            }
        }

        // Reset frame counter after all exports complete
        frameCounter = 0;
        System.gc();
    }
}
