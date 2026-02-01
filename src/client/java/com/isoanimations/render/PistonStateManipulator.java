package com.isoanimations.render;

import net.minecraft.block.entity.PistonBlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static com.isoanimations.IsometricAnimations.LOGGER;

/**
 * NUCLEAR OPTION: Directly manipulate piston BlockEntity fields to force interpolated positions.
 * This is necessary because isometric-renders doesn't use getProgress() - it renders static blocks.
 */
public class PistonStateManipulator {
    private static final Map<BlockPos, PistonState> savedStates = new HashMap<>();

    private static Field progressField = null;
    private static Field lastProgressField = null;

    static {
        try {
            // Get private fields via reflection
            progressField = PistonBlockEntity.class.getDeclaredField("progress");
            progressField.setAccessible(true);

            lastProgressField = PistonBlockEntity.class.getDeclaredField("lastProgress");
            lastProgressField.setAccessible(true);

            LOGGER.info("[PistonStateManipulator] Reflection setup successful");
        } catch (Exception e) {
            LOGGER.error("[PistonStateManipulator] Failed to setup reflection", e);
        }
    }

    /**
     * Temporarily set piston to interpolated position
     */
    public static void applyInterpolation(World world, BlockPos pos, float tickDelta) {
        try {
            var blockEntity = world.getBlockEntity(pos);
            if (!(blockEntity instanceof PistonBlockEntity pistonEntity)) {
                return;
            }

            // Get current values
            float currentProgress = progressField.getFloat(pistonEntity);
            float currentLastProgress = lastProgressField.getFloat(pistonEntity);

            // Save original state FIRST
            savedStates.put(pos, new PistonState(currentProgress, currentLastProgress));

            // Calculate interpolated value
            float interpolated;

            if (Math.abs(currentProgress - currentLastProgress) < 0.0001f) {
                // No change between last/current tick -> piston is stationary for this tick.
                // Do NOT try to "invent" motion by forcing lastProgress to 0; that causes
                // repeated/extra frames when the same world state is rendered multiple times.
                interpolated = currentProgress;
            } else {
                // Normal interpolation
                interpolated = currentLastProgress + (currentProgress - currentLastProgress) * tickDelta;
                interpolated = Math.max(0.0f, Math.min(1.0f, interpolated));
                LOGGER.info("[PistonStateManipulator] Interpolation at {}: {} -> {} (delta {}) = {}",
                    pos, currentLastProgress, currentProgress, tickDelta, interpolated);
            }

            // Override ONLY progress for rendering.
            // Keep lastProgress intact so the next render pass/tick still has correct slope.
            progressField.setFloat(pistonEntity, interpolated);

        } catch (Exception e) {
            LOGGER.error("[PistonStateManipulator] Failed to apply interpolation", e);
        }
    }

    /**
     * Restore original piston state
     */
    public static void restore(World world, BlockPos pos) {
        try {
            PistonState saved = savedStates.get(pos);
            if (saved == null) return;

            var blockEntity = world.getBlockEntity(pos);
            if (!(blockEntity instanceof PistonBlockEntity pistonEntity)) {
                return;
            }

            progressField.setFloat(pistonEntity, saved.progress);
            lastProgressField.setFloat(pistonEntity, saved.lastProgress);

            savedStates.remove(pos);

        } catch (Exception e) {
            LOGGER.error("[PistonStateManipulator] Failed to restore state", e);
        }
    }

    /**
     * Restore all saved states
     */
    public static void restoreAll(World world) {
        // Create a copy of the keys to avoid concurrent modification
        var positions = new HashMap<>(savedStates);

        for (Map.Entry<BlockPos, PistonState> entry : positions.entrySet()) {
            try {
                BlockPos pos = entry.getKey();
                PistonState saved = entry.getValue();

                var blockEntity = world.getBlockEntity(pos);
                if (blockEntity instanceof PistonBlockEntity pistonEntity) {
                    progressField.setFloat(pistonEntity, saved.progress);
                    lastProgressField.setFloat(pistonEntity, saved.lastProgress);
                }
            } catch (Exception e) {
                LOGGER.error("[PistonStateManipulator] Failed to restore state", e);
            }
        }

        // Clear all saved states after restoration
        savedStates.clear();
    }

    private record PistonState(float progress, float lastProgress) {}
}
