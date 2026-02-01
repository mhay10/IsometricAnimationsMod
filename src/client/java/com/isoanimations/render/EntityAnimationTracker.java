package com.isoanimations.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.isoanimations.IsometricAnimations.LOGGER;

/**
 * Tracks entity positions across ticks to enable smooth interpolation,
 * especially for entities being moved by pistons.
 */
public class EntityAnimationTracker extends AbstractTracker {
    private static final EntityAnimationTracker INSTANCE = new EntityAnimationTracker();

    // Entity state storage: Entity UUID -> Position for current and previous tick
    private final Map<UUID, Vec3d> currentTickPositions = new ConcurrentHashMap<>();
    private final Map<UUID, Vec3d> previousTickPositions = new ConcurrentHashMap<>();
    private final Map<UUID, Vec3d> currentTickVelocities = new ConcurrentHashMap<>();
    private final Map<UUID, Vec3d> previousTickVelocities = new ConcurrentHashMap<>();

    private EntityAnimationTracker() {
    }

    public static EntityAnimationTracker getInstance() {
        return INSTANCE;
    }

    /**
     * Start tracking entity positions within the specified area
     */
    public void startTracking(Box area) {
        // log then defer to base lifecycle
        LOGGER.info("Started tracking entity animations in area: {}", area);
        super.startTracking(area);
    }

    /**
     * Stop tracking and clear all state
     */
    public void stopTracking() {
        super.stopTracking();
        LOGGER.info("Stopped tracking entity animations");
    }

    @Override
    protected void saveCurrentToPrevious() {
        // Move current to previous BEFORE the tick executes
        previousTickPositions.clear();
        previousTickPositions.putAll(currentTickPositions);
        previousTickVelocities.clear();
        previousTickVelocities.putAll(currentTickVelocities);

        LOGGER.debug("Prepared for entity tick advance - saved {} entities as previous state",
                previousTickPositions.size());
    }

    @Override
    protected void captureCurrentState() {
        if (trackingArea == null || !isTracking())
            return;

        World world = MinecraftClient.getInstance().world;
        if (world == null)
            return;

        currentTickPositions.clear();
        currentTickVelocities.clear();

        // Get all entities in the expanded area (include a buffer for entities moving
        // into the area)
        Box expandedArea = trackingArea.expand(5.0);

        for (Entity entity : world.getEntitiesByClass(Entity.class, expandedArea, e -> true)) {
            UUID uuid = entity.getUuid();
            Vec3d pos = entity.getPos();
            Vec3d velocity = entity.getVelocity();

            currentTickPositions.put(uuid, pos);
            currentTickVelocities.put(uuid, velocity);
        }

        LOGGER.debug("Captured {} entities in tracking area", currentTickPositions.size());
    }

    @Override
    protected void clearState() {
        currentTickPositions.clear();
        previousTickPositions.clear();
        currentTickVelocities.clear();
        previousTickVelocities.clear();
    }

    /**
     * Get the interpolated position for an entity at a given tickDelta
     * 
     * @param entityId  Entity UUID
     * @param tickDelta Interpolation factor (0.0 = previous tick, 1.0 = current
     *                  tick)
     * @return Interpolated position, or null if entity is not tracked
     */
    public Vec3d getInterpolatedPosition(UUID entityId, float tickDelta) {
        if (!isTracking())
            return null;

        Vec3d current = currentTickPositions.get(entityId);
        Vec3d previous = previousTickPositions.get(entityId);

        // If we don't have both states, just return what we have (no interpolation)
        if (previous == null || current == null) {
            return current != null ? current : previous;
        }

        // Interpolate between the two positions
        return previous.lerp(current, tickDelta);
    }

    /**
     * Get the interpolated velocity for an entity at a given tickDelta
     */
    public Vec3d getInterpolatedVelocity(UUID entityId, float tickDelta) {
        if (!isTracking())
            return null;

        Vec3d current = currentTickVelocities.get(entityId);
        Vec3d previous = previousTickVelocities.get(entityId);

        if (previous == null || current == null) {
            return current != null ? current : previous;
        }

        return previous.lerp(current, tickDelta);
    }

    /**
     * Check if an entity is being tracked
     */
    public boolean isEntityTracked(UUID entityId) {
        return isTracking() && currentTickPositions.containsKey(entityId);
    }

    /**
     * Get all tracked entity UUIDs
     */
    public Set<UUID> getTrackedEntities() {
        return currentTickPositions.keySet();
    }

    /**
     * Record entity position (called from mixins or manual tracking)
     */
    public void recordEntityPosition(UUID entityId, Vec3d position, Vec3d velocity) {
        if (!isTracking())
            return;
        if (trackingArea == null || !trackingArea.contains(position)) {
            return;
        }

        currentTickPositions.put(entityId, position);
        currentTickVelocities.put(entityId, velocity);
    }
}
