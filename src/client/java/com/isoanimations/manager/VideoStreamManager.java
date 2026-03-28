package com.isoanimations.manager;

import com.isoanimations.config.PathConfig;
import com.isoanimations.config.RenderConfig;
import com.isoanimations.util.BufferPool;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Calendar;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static com.isoanimations.IsometricAnimations.LOGGER;

public class VideoStreamManager {
    // Recording objects
    private static FFmpegFrameRecorder recorder;
    private static Frame cvFrame;
    private static Path outputFilePath;
    private static long frameCount = 0;

    // Memory pointer to prevent leaks
    private static ByteBuffer nativeFrameBuffer;

    // Threading controls
    private static Thread encodingThread;
    private static volatile boolean isRecording = false;
    private static final BlockingQueue<ByteBuffer> frameQueue = new LinkedBlockingQueue<>();

    public static void startRecording(int width, int height) {
        if (!PathConfig.ANIMATION_EXPORT_DIR.toFile().exists()) {
            PathConfig.ANIMATION_EXPORT_DIR.toFile().mkdirs();
        }

        // Set output filepath
        Calendar now = Calendar.getInstance();
        String filename = "animation_%04d_%02d_%d_%02d-%02d-%02d.mkv".formatted(
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH) + 1,
                now.get(Calendar.DAY_OF_MONTH),
                now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE),
                now.get(Calendar.SECOND)
        );
        outputFilePath = PathConfig.ANIMATION_EXPORT_DIR.resolve(filename);

        try {
            // Setup recorder
            recorder = new FFmpegFrameRecorder(outputFilePath.toString(), width, height);
            recorder.setFormat("matroksa");
            recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
            recorder.setFrameRate(RenderConfig.outputFps);
            recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);

            // Optimization flags hopefully
            recorder.setVideoOption("preset", "ultrafast");
            recorder.setVideoOption("tune", "zerolatency");
            recorder.setOption("flush_packets", "1");
//            recorder.setOption("mov_flags", "frag_keyframe+empty_moov");
            recorder.setAudioChannels(0);
//            recorder.setMaxDelay(1);

            // Start recorder
            recorder.start();
            frameCount = 0;


            // Create reusable frame object
            cvFrame = new Frame(width, height, Frame.DEPTH_UBYTE, 3); // 8 bit depth, BGR color
            nativeFrameBuffer = (ByteBuffer) cvFrame.image[0];

            // Setup export queue
            isRecording = true;
            frameQueue.clear();

            // Start encoding thread in background
            encodingThread = new Thread(VideoStreamManager::encodingLoop);
            encodingThread.setName("JavaCV Encoding Thread");
            encodingThread.start();
        } catch (Exception e) {
            LOGGER.error("Failed to start recording", e);
        }
    }

    public static void addFrameToQueue(ByteBuffer frameData) {
        if (isRecording) {
            frameQueue.add(frameData);
        } else {
            BufferPool.returnBuffer(frameData);
        }
    }

    private static void encodingLoop() {
        // Encode frames until all frames gone from recording session
        while (isRecording || !frameQueue.isEmpty()) {
            try {
                // Wait up to 10ms to get next frame from queue
                ByteBuffer buffer = frameQueue.poll(10, TimeUnit.MILLISECONDS);
                if (buffer != null) {
                    // Set frame data to export frame
                    buffer.rewind();
                    nativeFrameBuffer.clear();
                    nativeFrameBuffer.put(buffer);
                    nativeFrameBuffer.rewind();

                    // Calculate timestamp in microseconds
                    long timestamp = (frameCount * 1000000L) / Math.round(RenderConfig.outputFps);
                    recorder.setTimestamp(timestamp);

                    // Record frame and return buffer
//                    recorder.record(cvFrame);
                    frameCount++;
                    BufferPool.returnBuffer(buffer);
                }
            } catch (Exception e) {
                LOGGER.error("Failed to encode frame", e);
            }
        }

        // Cleanup once all encoding finished
        try {
            if (recorder != null) {
                recorder.stop();
                recorder.release();
            }
            if (cvFrame != null) {
                cvFrame.close();
            }
        } catch (Exception e) {
            LOGGER.error("Failed to stop recorder", e);
        }
    }

    public static void stopRecording(FabricClientCommandSource source) {
        // Let encoding thread to stop
        isRecording = false;

        // Notify user that near end of exporting
        source.sendFeedback(Component.literal("Finalizing video file...").withStyle(ChatFormatting.YELLOW));

        // Wait for encoding thread to stop in background
        CompletableFuture.runAsync(() -> {
            try {
                // Wait for encoding thread to die
                if (encodingThread != null) {
                    encodingThread.join();
                }

                // Notify user of completion with clickable message
                source.getClient().execute(() -> {
                    sendOpenVideoMessage(source);
                    sendOpenFolderMessage(source);
                });
            } catch (Exception e) {
                LOGGER.error("Interrupted while waiting for encoding thread", e);
            }
        });
    }

    private static void sendOpenVideoMessage(FabricClientCommandSource source) {
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
