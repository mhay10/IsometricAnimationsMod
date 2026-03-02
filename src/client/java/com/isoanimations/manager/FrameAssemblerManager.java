package com.isoanimations.manager;

import com.isoanimations.config.PathConfig;
import com.isoanimations.config.RenderConfig;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Calendar;

import static com.isoanimations.IsometricAnimations.LOGGER;
import static org.bytedeco.ffmpeg.global.avcodec.*;
import static org.bytedeco.ffmpeg.global.avutil.*;

public class FrameAssemblerManager {
    private static boolean isInitialized = false;
    private static Path outputFilePath = null;

    public static void init() {
        // Ensure export directory exists
        if (!PathConfig.ANIMATION_EXPORT_DIR.toFile().exists()) {
            PathConfig.ANIMATION_EXPORT_DIR.toFile().mkdirs();
        }

        // Set output file path
        Calendar now = Calendar.getInstance();
        String filename = "animation_%04d_%02d_%d_%02d-%02d-%02d.mp4".formatted(
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH) + 1,
                now.get(Calendar.DAY_OF_MONTH),
                now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE),
                now.get(Calendar.SECOND)
        );
        outputFilePath = PathConfig.ANIMATION_EXPORT_DIR.resolve(filename);

        // Mark as initialized
        isInitialized = true;
    }

    public static void createAnimation(FabricClientCommandSource source) {
        // Only create animation if has been initialized
        if (!isInitialized) return;

        // Start animation creation in a new thread
        new Thread(() -> {
            // Set frame pattern and FPS
            String framePattern = PathConfig.FRAME_EXPORT_DIR.resolve("frame_%06d.tga").toFile().getAbsolutePath();
            int numFrames = PathConfig.FRAME_EXPORT_DIR.toFile().listFiles().length;

            try {
                // Read first frame to get dimensions
                File firstFrame = new File(String.format(framePattern, 0));
                BufferedImage image = readTgaAsBgr(firstFrame);
                int width = image.getWidth();
                int height = image.getHeight();

                // Build animation using JavaCV
                try (FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(outputFilePath.toFile(), width, height);
                     Java2DFrameConverter converter = new Java2DFrameConverter()) {
                    // Set recorder parameters and start recording
                    recorder.setFormat("mp4");
                    recorder.setVideoCodec(AV_CODEC_ID_H264);
                    recorder.setFrameRate(RenderConfig.outputFps);
                    recorder.setPixelFormat(AV_PIX_FMT_YUV420P);

                    recorder.start();

                    // Add frames to recorder
                    for (int i = 0; i < numFrames; i++) {
                        // Get frame file and check if it exists
                        File frameFile = new File(String.format(framePattern, i));
                        if (!frameFile.exists()) {
                            LOGGER.warn("Frame file missing: {} (skipping)", frameFile.getAbsolutePath());
                            continue;
                        }

                        // Read frame image and check if read correctly
                        BufferedImage frameImg = readTgaAsBgr(frameFile);
                        if (frameImg == null) {
                            LOGGER.warn("Could not read frame image: {} (skipping)", frameFile.getAbsolutePath());
                            continue;
                        }

                        // Convert frame to correct color order
                        if (frameImg.getType() != BufferedImage.TYPE_3BYTE_BGR) {
                            BufferedImage bgrImage = new BufferedImage(frameImg.getWidth(), frameImg.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
                            bgrImage.getGraphics().drawImage(frameImg, 0, 0, null);
                            frameImg = bgrImage;
                        }

                        // Record frame
                        Frame frame = converter.convert(frameImg);
                        recorder.record(frame);
                    }

                    // Stop recorder
                    recorder.stop();
                }

                // Notify user of completion with clickable message
                sendOpenVideoMessage(source);
                sendOpenFolderMessage(source);
            } catch (Exception e) {
                LOGGER.error("Failed to create animation: ", e);
                source.sendError(Component.literal("Error creating animation! Check logs for details"));
            }
        }).start();
    }

    private static BufferedImage readTgaAsBgr(File file) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            // Allocate buffers for width, height, and channels
            var widthBuffer = stack.mallocInt(1);
            var heightBuffer = stack.mallocInt(1);
            var channelsBuffer = stack.mallocInt(1);

            // Load image data into buffer
            ByteBuffer tgaBuffer = STBImage.stbi_load(file.getAbsolutePath(), widthBuffer, heightBuffer, channelsBuffer, 3);
            if (tgaBuffer == null) {
                LOGGER.error("Failed to load TGA image: {} --> {}", file.getAbsolutePath(), STBImage.stbi_failure_reason());
                return null;
            }

            // Get image dimensions from buffers
            int width = widthBuffer.get(0);
            int height = heightBuffer.get(0);

            // Create BGR image and buffer for pixel data
            BufferedImage bgrImage = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
            byte[] bgrPixels = ((DataBufferByte) bgrImage.getRaster().getDataBuffer()).getData();

            // Convert the RGB data to BGR format
            for (int i = 0; i < width * height; i++) {
                int index = i * 3;
                bgrPixels[index] = tgaBuffer.get(index + 2); // Blue
                bgrPixels[index + 1] = tgaBuffer.get(index + 1); // Green
                bgrPixels[index + 2] = tgaBuffer.get(index); // Red
            }

            // Free the TGA buffer
            STBImage.stbi_image_free(tgaBuffer);

            return bgrImage;
        }
    }

    private static void sendOpenVideoMessage(FabricClientCommandSource source) {
        // Only send if animation has been created
        if (!isInitialized) return;

        // Create clickable link to video file
        String filename = outputFilePath.toFile().getAbsolutePath();
        Component link = Component.literal(outputFilePath.getFileName().toString())
                .withStyle(style -> style
                        .withColor(ChatFormatting.GREEN)
                        .withUnderlined(true)
                        .withClickEvent(new ClickEvent.OpenFile(filename))
                        .withHoverEvent(new HoverEvent.ShowText(Component.literal("Open video file")))
                );

        // Combine link with message and send to user
        Component message = Component.literal("Animation created: ")
                .withStyle(ChatFormatting.GREEN)
                .append(link);
        source.sendFeedback(message);

    }

    private static void sendOpenFolderMessage(FabricClientCommandSource source) {
        // Only send if animation has been created
        if (!isInitialized) return;

        // Create clickable link to folder
        String folderPath = PathConfig.ANIMATION_EXPORT_DIR.toFile().getAbsolutePath();
        Component link = Component.literal("Animations Folder")
                .withStyle(style -> style
                        .withColor(ChatFormatting.AQUA)
                        .withUnderlined(true)
                        .withClickEvent(new ClickEvent.OpenFile(folderPath))
                        .withHoverEvent(new HoverEvent.ShowText(Component.literal("Open animation export folder")))
                );

        // Combine link with message and send to user
        Component message = Component.literal("Find all animations in: ")
                .withStyle(ChatFormatting.AQUA)
                .append(link);
        source.sendFeedback(message);
    }
}
