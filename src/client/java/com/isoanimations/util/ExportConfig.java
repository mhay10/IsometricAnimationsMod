package com.isoanimations.util;

import com.glisco.isometricrenders.util.ExportPathSpec;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public class ExportConfig {
    public static final Path EXPORT_ROOT = FabricLoader.getInstance().getGameDir().resolve("isoanimations");
    public static final Path FRAME_EXPORT_DIR = EXPORT_ROOT.resolve("frames");
    public static final Path ANIMATION_EXPORT_DIR = EXPORT_ROOT.resolve("animations");
}
