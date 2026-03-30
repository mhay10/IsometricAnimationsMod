package com.isoanimations.manager;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;

import java.util.concurrent.atomic.AtomicBoolean;

public class AnimationManager {
    // Animation Region
    private static AABB activeRegion = null;
    private static BlockPos pos1;
    private static BlockPos pos2;

    // Animation State
    private static final AtomicBoolean isAnimating = new AtomicBoolean(false);
    private static final AtomicBoolean animationFinished = new AtomicBoolean(false);
    private static boolean testingPosition = false;

    // Animation Config
    private static int durationTicks;
    private static long startTick;
    private static long endTick;
    private static int originalFps;

    public static void createAnimation(BlockPos pos1, BlockPos pos2, int durationTicks) {
        clearAnimation();

        // Setup region states
        AnimationManager.pos1 = pos1;
        AnimationManager.pos2 = pos2;
        activeRegion = AABB.of(BoundingBox.fromCorners(pos1, pos2));

        // Setup time states
        AnimationManager.durationTicks = durationTicks;
        startTick = Minecraft.getInstance().level.getGameTime();
        endTick = startTick + durationTicks;
    }

    public static void clearAnimation() {
        activeRegion = null;
        pos1 = null;
        pos2 = null;
        startTick = -1;
        endTick = -1;
        durationTicks = 0;
        isAnimating.set(false);
        animationFinished.set(false);
        testingPosition = false;
    }

    public static void startAnimation() {
        isAnimating.set(true);
        animationFinished.set(false);
    }

    public static void stopAnimation() {
        isAnimating.set(false);
        animationFinished.set(true);
    }

    public static BlockPos getPos1() {
        return pos1;
    }

    public static BlockPos getPos2() {
        return pos2;
    }

    public static AABB getActiveRegion() {
        return activeRegion;
    }

    public static boolean isAnimating() {
        return isAnimating.get();
    }

    public static boolean isAnimationFinished() {
        return animationFinished.get();
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

    public static void setOriginalFps(int fps) {
        originalFps = fps;
    }

    public static int getOriginalFps() {
        return originalFps;
    }

    public static void setTestingPosition(boolean isTesting) {
        testingPosition = isTesting;
    }

    public static boolean isTestingPosition() {
        return testingPosition;
    }
}
