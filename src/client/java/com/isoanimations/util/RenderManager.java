package com.isoanimations.util;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

import static com.isoanimations.IsometricAnimations.LOGGER;

public class RenderManager {
    private static int rotation = 0;
    private static int skew = 0;
    private static int scale = 100;

    public static Vector3d getRenderPosition(BlockPos pos1, BlockPos pos2) {


        // Get center position of region
        Vector3d center = getCenterPositionVec3d(pos1, pos2);

        // Get min scaling factor
        double dimX = Math.abs(pos2.getX() - pos1.getX());
        double dimY = Math.abs(pos2.getY() - pos1.getY());
        double dimZ = Math.abs(pos2.getZ() - pos1.getZ());
        double diag = Math.sqrt(dimX * dimX + dimY * dimY + dimZ * dimZ);

        // Calculate min distance from area based on FOV to fit entire area in view
        double fov = Minecraft.getInstance().options.fov().get();
        double requiredDistance = diag / (2.0 * Math.tan(Math.toRadians(fov / 2.0)));

        // Calculate offset and apply transformations
        Vector3d direction = new Vector3d(1, 1, 1).normalize();
        Vector3d offset = direction.mul(requiredDistance);

        // TODO: Fix scaling and skew not working like isorenders mod
        offset.mul(scale / 100.0); // Scale
        offset.y += -offset.z * Math.tan(Math.toRadians(skew)); // Skew
        offset.rotateY(Math.toRadians(rotation)); // Rotation

        LOGGER.info("Calculated render offset: " + offset);

        // Add offset to center positionw
        offset.y++; // This better work >:)
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
