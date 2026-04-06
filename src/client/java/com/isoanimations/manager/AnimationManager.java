package com.isoanimations.manager;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;

import java.util.concurrent.atomic.AtomicBoolean;

public class AnimationManager {
    // Animation Region
    private static AABB activeRegion = null;
    private static BlockPos minPos;
    private static BlockPos maxPos;

    // Fast Access for Region Checks
    public static int minX, minY, minZ;
    public static int maxX, maxY, maxZ;
    public static boolean hasActiveRegion = false;


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
        AnimationManager.minPos = BlockPos.min(pos1, pos2);
        AnimationManager.maxPos = BlockPos.max(pos2, pos2);
        activeRegion = AABB.of(BoundingBox.fromCorners(pos1, pos2));

        // Calculate min/max for easier checks
        minX = AnimationManager.minPos.getX();
        minY = AnimationManager.minPos.getY();
        minZ = AnimationManager.minPos.getZ();
        maxX = AnimationManager.maxPos.getX();
        maxY = AnimationManager.maxPos.getY();
        maxZ = AnimationManager.maxPos.getZ();
        hasActiveRegion = true;

        // Setup time states
        AnimationManager.durationTicks = durationTicks;
        startTick = Minecraft.getInstance().level.getGameTime();
        endTick = startTick + durationTicks;
    }

    public static void clearAnimation() {
        activeRegion = null;
        hasActiveRegion = false;

        minPos = null;
        maxPos = null;

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

    public static boolean inActiveRegion(int x, int y, int z) {
        return activeRegion.contains(x, y, z);
    }

    public static BlockPos getMinPos() {
        return minPos;
    }

    public static BlockPos getMaxPos() {
        return maxPos;
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
