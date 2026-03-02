package com.isoanimations.config;

// Output FPS Formula = (fps * tps) / (tick rate)

public class RenderConfig {
    public static final int ticksPerSecond = 20;
    public static int renderFps = 60;
    public static double tickRate = 5;
    public static final double outputFps = (renderFps * ticksPerSecond) / tickRate;

    public static void loadConfig() {
        // TOOD: Load config from file (maybe idk)
    }

    public static void setRenderFps(int renderFps) {
        RenderConfig.renderFps = renderFps;
    }

    public static void setTickRate(double tickRate) {
        RenderConfig.tickRate = tickRate;
    }
}
