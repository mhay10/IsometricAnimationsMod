package com.isoanimations.animation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public class AnimationController {
    private static BlockPos pos1 = null;
    private static BlockPos pos2 = null;
    private static int durationTicks = 0;

    //
    private static volatile BoundingBox activeRegion = null;

    public static void createAnimation(BlockPos pos1, BlockPos pos2, int durationTicks) {
        AnimationController.pos1 = pos1;
        AnimationController.pos2 = pos2;
        AnimationController.durationTicks = durationTicks;
    }

    public void startAnimation() {
        this.setActionRegion(pos1, pos2);
    }

    private void stopAnimation() {
        activeRegion = null;
    }

    // Setters and getters
    public void setActionRegion(BlockPos pos1, BlockPos pos2) {
        activeRegion = BoundingBox.fromCorners(pos1, pos2);
    }

    public static BoundingBox getActiveRegion() {
        return activeRegion;
    }
}
