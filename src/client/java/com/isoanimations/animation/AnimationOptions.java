package com.isoanimations.animation;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public class AnimationOptions {
    public static Path outputPath = FabricLoader.getInstance().getGameDir().resolve("isoanimations");
    public static int fps = 60;

}
