package com.isoanimations.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

import static com.isoanimations.IsometricAnimations.LOGGER;

public class RenderManager {
    private static int rotation = 0;
    private static int skew = 0;
    private static int scale = 100;

    public static Vector3d getRenderPosition(BlockPos pos1, BlockPos pos2) {
        // Get center position and radius of region
        Vector3d center = getCenterPositionVec3d(pos1, pos2);
        double radius = center.distance(pos1.getX(), pos1.getY(), pos1.getZ());

        // Calculate offset from center based on scale, skew, and rotation
        Vector3d offset = new Vector3d(radius, radius, radius);

        offset.mul(scale / 100.0); // Scale
        offset.x += offset.z * Math.tan(Math.toRadians(skew)); // Skew
        offset.rotateY(Math.toRadians(rotation)); // Rotation

        LOGGER.info("Calculated render offset: " + offset);

        // Add offset to center position
        return center.add(offset);
    }

    public static Vec3 getCenterPosition(BlockPos pos1, BlockPos pos2) {
        // Get center position of region
        return new Vec3(
                (pos1.getX() + pos2.getX()) / 2.0,
                (pos1.getY() + pos2.getY()) / 2.0,
                (pos1.getZ() + pos2.getZ()) / 2.0
        );
    }

    public static Vector3d getCenterPositionVec3d(BlockPos pos1, BlockPos pos2) {
        Vec3 centerPos = getCenterPosition(pos1, pos2);
        return new Vector3d(centerPos.x, centerPos.y, centerPos.z);
    }

    public static void setRotation(int rotation) {
        RenderManager.rotation = rotation;
    }

    public static void setSkew(int skew) {
        RenderManager.skew = skew;
    }

    public static void setScale(int scale) {
        RenderManager.scale = scale;
    }

    public static int getRotation() {
        return rotation;
    }

    public static int getSkew() {
        return skew;
    }

    public static int getScale() {
        return scale;
    }
}
