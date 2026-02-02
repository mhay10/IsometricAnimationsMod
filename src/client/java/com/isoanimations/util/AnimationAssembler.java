package com.isoanimations.util;

import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.ffmpeg.global.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.util.Calendar;

import static com.isoanimations.IsometricAnimations.LOGGER;

public class AnimationAssembler {
    // Command context
    private final FabricClientCommandSource source;

    // Animation export params
    private final long frameCount;
    private final Path outputFilePath;

    public AnimationAssembler(FabricClientCommandSource source, long frameCount) {
        // Set command context
        this.source = source;

        // Set animation export params
        this.frameCount = frameCount;

        Calendar now = Calendar.getInstance();
        String outputFilename = "animation_%04d_%02d_%d_%02d-%02d-%02d.mp4".formatted(
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH) + 1,
                now.get(Calendar.DAY_OF_MONTH),
                now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE),
                now.get(Calendar.SECOND));
        this.outputFilePath = ExportConfig.ANIMATION_EXPORT_DIR.resolve(outputFilename);

        // Create export directories if they don't exist
        if (!outputFilePath.getParent().toFile().exists()) {
            if (!outputFilePath.getParent().toFile().mkdirs()) {
                LOGGER.error("Failed to create animation export directory");
            }
        }
    }

    public void createAnimation() throws InterruptedException {
        // Notify user that animation creation is starting
        this.source.sendFeedback(
                Text.literal("Creating animation from frames. This may take a while...").formatted(Formatting.YELLOW));

        // Start animation creation in a new thread
        new Thread(() -> {
            // Build path to frame sequence
            String framePattern = ExportConfig.FRAME_EXPORT_DIR.resolve("frame_%05d.png").toFile().getAbsolutePath();
            int fps = SubTickConfig.getTargetFPS();

            try {
                LOGGER.info("Starting JavaCV FFmpegFrameRecorder to create animation from {} frames at {} FPS",
                        frameCount, fps);

                // Read the first frame to get width/height
                File firstFrame = new File(String.format(framePattern, 0));
                if (!firstFrame.exists()) {
                    firstFrame = new File(String.format(framePattern, 1));
                }
                if (!firstFrame.exists()) {
                    throw new IllegalStateException("No frame images found to assemble animation.");
                }
                BufferedImage sampleImage = ImageIO.read(firstFrame);
                int width = sampleImage.getWidth();
                int height = sampleImage.getHeight();

                try (FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(outputFilePath.toFile(), width, height);
                        Java2DFrameConverter converter = new Java2DFrameConverter()) {
                    recorder.setFormat("mp4");
                    recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
                    recorder.setFrameRate(fps);
                    recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);
                    recorder.setVideoOption("threads", "0");

                    recorder.start();

                    for (int i = 0; i < frameCount; i++) {
                        File frameFile = new File(String.format(framePattern, i));
                        if (!frameFile.exists()) {
                            LOGGER.warn("Frame file missing: {} (skipping)", frameFile.getAbsolutePath());
                            continue;
                        }
                        BufferedImage img = ImageIO.read(frameFile);
                        if (img == null) {
                            LOGGER.warn("Could not read image: {} (skipping)", frameFile.getAbsolutePath());
                            continue;
                        }
                        // Convert to TYPE_3BYTE_BGR to ensure correct color order
                        if (img.getType() != BufferedImage.TYPE_3BYTE_BGR) {
                            BufferedImage bgrImg = new BufferedImage(img.getWidth(), img.getHeight(),
                                    BufferedImage.TYPE_3BYTE_BGR);
                            bgrImg.getGraphics().drawImage(img, 0, 0, null);
                            img = bgrImg;
                        }
                        // No channel swap, rely on JavaCV preset
                        Frame frame = converter.convert(img);
                        recorder.record(frame);
                    }

                    recorder.stop();
                }

                // Notify user of completion
                LOGGER.info("Animation created successfully: {}", outputFilePath.toAbsolutePath());
                source.sendFeedback(
                        Text.literal("Animation created: " + outputFilePath.getFileName()).formatted(Formatting.GREEN));

                // Add clickable message to open the animations folder
                sendOpenFolderMessage();
            } catch (Exception e) {
                LOGGER.error("Error creating animation with JavaCV FFmpegFrameRecorder.", e);
                source.sendError(Text.literal("Error creating animation: could not encode video"));
            }
        }).start();
    }

    private void sendOpenFolderMessage() {
        // Create clickable text that opens the animations folder
        MutableText message = Text.literal("[Click here to open animations folder]")
                .formatted(Formatting.GOLD, Formatting.UNDERLINE)
                .styled(style -> style
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/openanimations"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Text.literal("Open the animations folder").formatted(Formatting.YELLOW))));

        source.sendFeedback(message);
    }
}
