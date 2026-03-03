package com.isoanimations.config;

import java.io.*;
import java.util.Properties;

import static com.isoanimations.IsometricAnimations.LOGGER;

public class RenderConfig {
    public static final double TICKS_PER_SECOND = 20;
    public static int renderFps = 60;
    public static double tickRate = 5;
    public static final double outputFps = (renderFps * TICKS_PER_SECOND) / tickRate;
    private static File configFile = new File(PathConfig.ISOANIMATIONS_ROOT.resolve("render.cfg").toUri());

    public static void loadConfig() {
        if (configFile.exists()) {
            Properties props = new Properties();
            try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
                props.load(reader);
                renderFps = Integer.parseInt(props.getProperty("render_fps", String.valueOf(renderFps)));
                tickRate = Double.parseDouble(props.getProperty("tick_rate", String.valueOf(tickRate)));
            } catch (Exception e) {
                LOGGER.error("Failed to load render config, using defaults: ", e);
            }
        } else {
            // Create default config file if it doesn't exist
            try {
                configFile.getParentFile().mkdirs();
                configFile.createNewFile();
                saveConfig();
            } catch (Exception e) {
                LOGGER.error("Failed to create default render config: ", e);
            }
        }
    }

    public static void saveConfig() {
        // Set config values
        Properties props = new Properties();
        props.setProperty("render_fps", String.valueOf(renderFps));
        props.setProperty("tick_rate", String.valueOf(tickRate));

        // Save to file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(configFile))) {
            props.store(writer, "Isometric Animations Render Config");
        } catch (Exception e) {
            LOGGER.error("Failed to save render config: ", e);
        }
    }

    public static void setRenderFps(int renderFps) {
        RenderConfig.renderFps = renderFps;
    }

    public static void setTickRate(double tickRate) {
        RenderConfig.tickRate = tickRate;
    }
}
