package com.isoanimations.manager;

import net.minecraft.world.phys.Vec3;

public class CameraManager {
    private static boolean isDetached = false;
    private static Vec3 position = Vec3.ZERO;
    private static float xRot = 0.0f; // Pitch
    private static float yRot = 0.0f; // Yaw

    public static void setCamera(Vec3 pos, float pitch, float yaw) {
        position = pos;
        xRot = pitch;
        yRot = yaw;
        isDetached = true;
    }

    public static void reset() {
        isDetached = false;
    }

    public static boolean isDetached() {
        return isDetached;
    }

    public static Vec3 getPosition() {
        return position;
    }

    public static float getXRot() {
        return xRot;
    }

    public static float getYRot() {
        return yRot;
    }
}
