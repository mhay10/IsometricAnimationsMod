package com.isoanimations.util;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.concurrent.atomic.AtomicBoolean;

public class AnimationManager {
    private static BoundingBox activeRegion = null;
    private static BlockPos pos1;
    private static BlockPos pos2;
    private static AtomicBoolean isAnimating = new AtomicBoolean(false);

    private static int durationTicks;
    private static long startTick;
    private static long endTick;

    public static void createAnimation(BlockPos pos1, BlockPos pos2, int durationTicks) {
        clearPrevAnimation();

        // Setup region states
        AnimationManager.pos1 = pos1;
        AnimationManager.pos2 = pos2;
        activeRegion = BoundingBox.fromCorners(pos1, pos2);

        // Setup time states
        AnimationManager.durationTicks = durationTicks;
        startTick = Minecraft.getInstance().level.getGameTime();
        endTick = startTick + durationTicks;
    }

    public static void clearPrevAnimation() {
        pos1 = null;
        pos2 = null;
        durationTicks = 0;
        activeRegion = null;
    }

    public static void startAnimation() {
        isAnimating.set(true);
    }

    public static void stopAnimation() {
        isAnimating.set(false);
    }

    public static BlockPos getPos1() {
        return pos1;
    }

    public static BlockPos getPos2() {
        return pos2;
    }

    public static BoundingBox getActiveRegion() {
        return activeRegion;
    }


    public static boolean isAnimating() {
        return isAnimating.get();
    }

    public static int getDurationTicks() {
        return durationTicks;
    }

    public static long getStartTick() {
        return startTick;
    }

    public static long getEndTick() {
        return endTick;
    }
}
