package com.isoanimations.manager;

import com.isoanimations.config.ExportConfig;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static com.isoanimations.IsometricAnimations.LOGGER;

public class FrameExportManager {
    private static final ExecutorService exportExecutor = Executors.newSingleThreadExecutor();
    private static int frameCounter = 0;
    private static AtomicInteger pendingFrames = new AtomicInteger(0);

    public static void init() {
        // Ensure export directory exists
        ExportConfig.FRAME_EXPORT_DIR.toFile().mkdirs();

        // Clear existing frames in export directory
        try {
            Files.walk(ExportConfig.FRAME_EXPORT_DIR)
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

    public static void queueFrameExport(byte[] frameData, int width, int height) {
        // Increment pending frame count
        int currentFrame = frameCounter++;
        pendingFrames.incrementAndGet();
        LOGGER.info("Queued frame {} for export ({} pending)", currentFrame, pendingFrames.get());

        // Submit export task to executor thread
        exportExecutor.submit(() -> {
            // Create blank image in memory
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

            // Decode raw RGBA data into image
            int bytesPerPixel = 4;
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    // Flip y coordinate to match OpenGL's bottom-left origin
                    int flippedY = height - 1 - y;

                    // Get starting index for pixel data
                    int index = (y * width + x) * bytesPerPixel;

                    // Extract RGBA components
                    int r = frameData[index] & 0xFF;
                    int g = frameData[index + 1] & 0xFF;
                    int b = frameData[index + 2] & 0xFF;
                    int a = 255;

                    // Set pixel color in image as ARGB
                    int argb = (a << 24) | (r << 16) | (g << 8) | b;
                    image.setRGB(x, flippedY, argb);
                }
            }

            // Write image to disk
            try {
                // Save frame with sequential filename
                File frame = new File(ExportConfig.FRAME_EXPORT_DIR.resolve("frame_%06d.png".formatted(currentFrame)).toUri());
                ImageIO.write(image, "PNG", frame);
            } catch (Exception e) {
                LOGGER.error("Failed to export frame {}: {}", currentFrame, e);
            } finally {
                // Decrement pending frame count after export completes
                pendingFrames.decrementAndGet();
                LOGGER.info("Frame {} export complete ({} pending)", currentFrame, pendingFrames.get());
            }
        });
    }

    public static void waitForExportFinish() {
        while (pendingFrames.get() > 0) {
            try {
                Thread.sleep(100);
            } catch (Exception ignored) {
            }
        }
    }
}
