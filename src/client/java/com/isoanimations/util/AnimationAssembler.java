package com.isoanimations.util;

import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.io.FileWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static com.isoanimations.IsometricAnimations.LOGGER;

public class AnimationAssembler {
    // Command context
    private FabricClientCommandSource source = null;

    // Animation export params
    private long frameCount;
    private final int frameRate = 20;
    private final Path outputFilePath;
    private final Path inputFilePath = ExportConfig.ANIMATION_EXPORT_DIR.resolve("frames.txt");

    // FFmpeg detection state
    private boolean ffmpegDetected = false;

    public AnimationAssembler(FabricClientCommandSource source, long frameCount) {
        // Set command context
        this.source = source;

        // Detect FFmpeg on initialization
        this.ffmpegDetected = this.detectFFmpeg();

        // Set animation export params
        this.frameCount = frameCount;

        Calendar now = Calendar.getInstance();
        String outputFilename = "animation_%04d_%02d_%d_%02d-%02d-%02d.mp4".formatted(
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH) + 1,
                now.get(Calendar.DAY_OF_MONTH),
                now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE),
                now.get(Calendar.SECOND)
        );
        this.outputFilePath = ExportConfig.ANIMATION_EXPORT_DIR.resolve(outputFilename);

        // Create export directories if they don't exist
        if (!outputFilePath.getParent().toFile().exists())
            outputFilePath.getParent().toFile().mkdirs();
    }

    public void createAnimation() {
        // Ensure FFmpeg is detected before proceeding
        if (!this.ffmpegDetected) {
            LOGGER.error("FFmpeg not detected, cannot create animation.");
            return;
        }

        // Create input file for FFmpeg
        this.createInputFile();

        // Build and execute FFmpeg command
        ArrayList<String> command = new ArrayList<>(List.of((new String[]{
                "ffmpeg",
                "-r", "20",
                "-safe", "0",
                "-f", "concat", "-i", inputFilePath.toFile().getAbsolutePath(),
                "-c:v", "libx264",
                "-pix_fmt", "yuv420p",
                "-y", outputFilePath.toFile().getAbsolutePath()
        })));
        try {
            // Execute FFmpeg command
            Process process = new ProcessBuilder(command)
                    .redirectError(ProcessBuilder.Redirect.INHERIT)
                    .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                    .start();
            process.onExit().join();

            // Notify user of completion
            LOGGER.info("Animation created successfully: {}", outputFilePath.toAbsolutePath());
            source.sendFeedback(Text.literal("Animation created successfully: " + outputFilePath.getFileName()).formatted(Formatting.GREEN));

            // Clean up temporary input file
            this.cleanupInputFile();
        } catch (Exception e) {
            LOGGER.error("Error executing FFmpeg command to create animation.", e);
            source.sendError(Text.literal("Error creating animation: could not execute FFmpeg command"));
        }
    }

    private void cleanupInputFile() {
        try {
            if (inputFilePath.toFile().exists()) {
                inputFilePath.toFile().delete();
                LOGGER.info("Deleted temporary FFmpeg input file: {}", inputFilePath.toAbsolutePath());
            }
        } catch (Exception e) {
            LOGGER.error("Error deleting temporary FFmpeg input file.", e);
        }
    }

    private void createInputFile() {
        try {
            FileWriter framesFile = new FileWriter(inputFilePath.toFile().getAbsolutePath());
            for (int frame = 0; frame <= frameCount; frame++) {
                // Get frame file path and format for FFmpeg
                String framePath = ExportConfig.FRAME_EXPORT_DIR.resolve("frame_%05d.png".formatted(frame))
                        .toAbsolutePath().toString()
                        .replace("\\", "/").replace("'", "'\\''");

                // Write frame entry to input file
                framesFile.write("file '%s'\n".formatted(framePath));
                LOGGER.info("Added frame {}/{} to FFmpeg input file: {}", frame, this.frameCount, framePath);
            }
            framesFile.close();
        } catch (Exception e) {
            LOGGER.error("Error creating frames input file for FFmpeg.", e);
            source.sendError(Text.literal("Error creating animation: could not create frames input file"));
        }
    }

    public boolean isFFmpegDetected() {
        return this.ffmpegDetected;
    }

    private boolean detectFFmpeg() {
        try {
            // Check if FFmpeg is installed by running 'ffmpeg -version' command
            Process process = new ProcessBuilder("ffmpeg", "-version").redirectError(ProcessBuilder.Redirect.DISCARD).start();
            process.onExit().join();

            // Read the output to confirm FFmpeg is working
            String processOutput = new String(process.getInputStream().readAllBytes());
            LOGGER.info("FFmpeg detected. Version: {}", processOutput.split(" ")[2]);
            return true;
        } catch (Exception e) {
            // FFmpeg not found or error occurred
            LOGGER.error("FFmpeg not found. Please ensure FFmpeg is installed and added to your system PATH.");
            source.sendError(Text.literal("Error creating animation: FFmpeg not found"));
            return false;
        }
    }
}
