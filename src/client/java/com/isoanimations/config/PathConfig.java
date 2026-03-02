package com.isoanimations.config;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public class PathConfig {
    public static final Path ISOANIMATIONS_ROOT = FabricLoader.getInstance().getGameDir().resolve("isoanimations");
    public static final Path FRAME_EXPORT_DIR = ISOANIMATIONS_ROOT.resolve("frames");
    public static final Path ANIMATION_EXPORT_DIR = ISOANIMATIONS_ROOT.resolve("animations");
}
