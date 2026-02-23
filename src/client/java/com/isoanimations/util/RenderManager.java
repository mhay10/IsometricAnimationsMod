package com.isoanimations.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class RenderManager {
    private static int pitch = 0;
    private static int yaw = 0;
    private static int scale = 100;

    public static Vector3f getRenderPosition(BlockPos pos1, BlockPos pos2, Vec3 playerPos, float targetFov) {
        // Get animation region center
        Vector3f regionCenter = getCenterPosition(pos1, pos2);

        // Calculate radius of region
        double dx = pos1.getX() - pos2.getX();
        double dy = pos1.getY() - pos2.getY();
        double dz = pos1.getZ() - pos2.getZ();
        float diagonalDistance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        float radius = diagonalDistance / 2.0f;

        // Calculate base distance from radius and FOV
        float baseDistance = (radius / (float) Math.sin(Math.toRadians(targetFov / 2.0f))) * 1.55f;

        // Create target position at base distance
        Vector3f targetPos = new Vector3f(0, 0, baseDistance);

        // Calculate transformation matrix
        Matrix4f transform = new Matrix4f();
        transform.rotateY((float) Math.toRadians(pitch));
        transform.rotateX((float) Math.toRadians(yaw));
        transform.scale(scale / 100.0f);

        // Apply transformations to player position
        transform.transformPosition(targetPos);

        // Translate to world coordinates
        targetPos.add(regionCenter);
        return targetPos;
    }

    public static Vector3f getCenterPosition(BlockPos pos1, BlockPos pos2) {
        return new AABB(
                new Vec3(pos1.getX(), pos1.getY(), pos1.getZ()),
                new Vec3(pos2.getX(), pos2.getY(), pos2.getZ())
        ).getCenter().toVector3f();
    }

    public static void setPitch(int pitch) {
        RenderManager.pitch = pitch;
    }

    public static void setYaw(int yaw) {
        RenderManager.yaw = yaw;
    }

    public static void setScale(int scale) {
        RenderManager.scale = scale;
    }

    public static int getPitch() {
        return pitch;
    }

    public static int getYaw() {
        return yaw;
    }

    public static int getScale() {
        return scale;
    }
}
