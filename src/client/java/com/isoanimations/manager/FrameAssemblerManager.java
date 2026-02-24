package com.isoanimations.manager;

import com.isoanimations.config.ExportConfig;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
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
        if (!ExportConfig.ANIMATION_EXPORT_DIR.toFile().exists()) {
            ExportConfig.ANIMATION_EXPORT_DIR.toFile().mkdirs();
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
        outputFilePath = ExportConfig.ANIMATION_EXPORT_DIR.resolve(filename);

        // Mark as initialized
        isInitialized = true;
    }

    public static void createAnimation(FabricClientCommandSource source) {
        // Only create animation if has been initialized
        if (!isInitialized) return;

        // Start animation creation in a new thread
        new Thread(() -> {
            // Set frame pattern and get FPS
            String framePattern = ExportConfig.FRAME_EXPORT_DIR.resolve("frame_%06d.png").toFile().getAbsolutePath();
            int fps = Minecraft.getInstance().getFps();
            int numFrames = ExportConfig.FRAME_EXPORT_DIR.toFile().listFiles().length;

            try {
                // Read first frame to get dimensions
                File firstFrame = new File(String.format(framePattern, 0));
                BufferedImage image = ImageIO.read(firstFrame);
                int width = image.getWidth();
                int height = image.getHeight();

                // Build animation using JavaCV
                try (FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(outputFilePath.toFile(), width, height);
                     Java2DFrameConverter converter = new Java2DFrameConverter()) {
                    // Set recorder parameters and start recording
                    recorder.setFormat("mp4");
                    recorder.setVideoCodec(AV_CODEC_ID_H264);
                    recorder.setFrameRate(fps);
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
                        BufferedImage frameImg = ImageIO.read(frameFile);
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

                // Notify user of completion with clickable message to open export folder
                source.sendFeedback(
                        Component.literal("Animation created: %s".formatted(outputFilePath.getFileName()))
                                .withStyle(ChatFormatting.GREEN)
                );
                sendOpenFolderMessage(source);
            } catch (Exception e) {
                LOGGER.error("Failed to create animation: ", e);
                source.sendError(Component.literal("Error creating animation! Check logs for details"));
            }
        }).start();
    }

    private static void sendOpenFolderMessage(FabricClientCommandSource source) {
    }
}
