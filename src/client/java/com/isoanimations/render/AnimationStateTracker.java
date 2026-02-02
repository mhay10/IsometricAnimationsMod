package com.isoanimations.render;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;

import static com.isoanimations.IsometricAnimations.LOGGER;

/**
 * Tracks animation states of all blocks within the rendering area across ticks.
 * Maintains history to enable smooth interpolation between game ticks.
 */
public class AnimationStateTracker extends AbstractTracker {
    private static final AnimationStateTracker INSTANCE = new AnimationStateTracker();

    // State storage: BlockPos -> List<SubTickState> for current tick, BlockPos ->
    // BlockAnimationState for previous tick
    private final Map<BlockPos, List<SubTickState>> currentTickStates = new ConcurrentHashMap<>();
    private final Map<BlockPos, BlockAnimationState> previousTickStates = new ConcurrentHashMap<>();

    private AnimationStateTracker() {
    }

    public static AnimationStateTracker getInstance() {
        return INSTANCE;
    }

    /**
     * Start tracking animation states within the specified area
     */
    public void startTracking(BlockPos pos1, BlockPos pos2) {
        Box area = new Box(
                Math.min(pos1.getX(), pos2.getX()),
                Math.min(pos1.getY(), pos2.getY()),
                Math.min(pos1.getZ(), pos2.getZ()),
                Math.max(pos1.getX(), pos2.getX()) + 1,
                Math.max(pos1.getY(), pos2.getY()) + 1,
                Math.max(pos1.getZ(), pos2.getZ()) + 1);
        LOGGER.info("[AnimationStateTracker] startTracking called with pos1={} pos2={} trackingArea={}", pos1, pos2,
                area);
        super.startTracking(area);
    }

    /**
     * Stop tracking and clear all state
     */
    public void stopTracking() {
        super.stopTracking();
    }

    /**
     * Trim memory used by stored states.
     * Default (no-arg) performs a conservative trim suitable for long-running
     * renders: it prunes sub-tick history but preserves the previous tick state
     * required for interpolation.
     */
    public void trimMemory() {
        trimMemory(false);
    }

    /**
     * Trim memory, with an option for aggressive cleanup.
     * If aggressive==true, previous tick state will also be cleared. Use
     * aggressive mode when stopping rendering completely.
     */
    public void trimMemory(boolean aggressive) {
        // Prune sub-tick history in current tick states - keep only the most
        // recent sub-tick state per position to preserve interpolation data
        for (Map.Entry<BlockPos, List<SubTickState>> entry : currentTickStates.entrySet()) {
            List<SubTickState> states = entry.getValue();
            if (states.size() > 1) {
                SubTickState last = states.get(states.size() - 1);
                states.clear();
                states.add(last);
            }
        }

        // Only clear previous tick states when doing an aggressive trim or when
        // tracking has stopped; previous tick states are necessary for
        // interpolation within the current tick
        if (aggressive || !isTracking()) {
            previousTickStates.clear();
        }

        // Suggest garbage collection
        System.gc();
    }

    @Override
    protected void saveCurrentToPrevious() {
        // Move current to previous BEFORE the tick executes
        previousTickStates.clear();
        // previousTickStates.putAll(currentTickStates); // Incorrect: type mismatch
        // Correct: extract last SubTickState.state from each entry
        for (Map.Entry<BlockPos, List<SubTickState>> entry : currentTickStates.entrySet()) {
            List<SubTickState> states = entry.getValue();
            if (!states.isEmpty()) {
                previousTickStates.put(entry.getKey(), states.get(states.size() - 1).state);
            }
        }

        LOGGER.debug("Prepared for tick advance - saved {} blocks as previous state", previousTickStates.size());
    }

    @Override
    protected void captureCurrentState() {
        if (trackingArea == null || !isTracking())
            return;

        World world = MinecraftClient.getInstance().world;
        if (world == null) {
            LOGGER.warn("[AnimationStateTracker] captureCurrentState: world is null! No states will be tracked.");
            return;
        }

        currentTickStates.clear();

        // Iterate through all blocks in the area
        BlockPos.iterate(
                (int) trackingArea.minX, (int) trackingArea.minY, (int) trackingArea.minZ,
                (int) trackingArea.maxX - 1, (int) trackingArea.maxY - 1, (int) trackingArea.maxZ - 1).forEach(pos -> {
                    BlockPos immutablePos = pos.toImmutable();
                    BlockState state = world.getBlockState(immutablePos);

                    // Create animation state for this block
                    BlockAnimationState animState = BlockAnimationStateFactory.createFromWorld(world, immutablePos,
                            state);
                    if (animState != null) {
                        // Fix: store as a list of SubTickState, as required by currentTickStates
                        List<SubTickState> states = new ArrayList<>();
                        states.add(new SubTickState(animState, 1.0f)); // Use 1.0f to indicate end of tick
                        currentTickStates.put(immutablePos, states);
                    }
                });

        LOGGER.info("[AnimationStateTracker] captureCurrentState: Captured {} animated blocks in area {}",
                currentTickStates.size(), trackingArea);
    }

    @Override
    protected void clearState() {
        currentTickStates.clear();
        previousTickStates.clear();
    }

    /**
     * Called when a game tick advances - saves current state as previous and
     * captures new state
     * 
     * @deprecated Use prepareTickAdvance() before tick and captureAfterTick() after
     *             tick instead
     */
    @Deprecated
    public void onTickAdvance() {
        // Maintain backwards-compatible behavior by performing both prepare and capture
        prepareTickAdvance();
        captureAfterTick();

        LOGGER.debug("Tick advanced - tracked {} blocks", currentTickStates.size());
    }

    // Sub-tick state holder
    public static class SubTickState {
        public final BlockAnimationState state;
        public final float subTickTime; // 0.0 to 1.0 within the tick

        public SubTickState(BlockAnimationState state, float subTickTime) {
            this.state = state;
            this.subTickTime = subTickTime;
        }
    }

    /**
     * Record a block animation state at a specific sub-tick time (called from
     * mixins)
     */
    public void recordBlockState(BlockPos pos, BlockAnimationState state, float subTickTime) {
        if (!isTracking())
            return;
        if (trackingArea == null || !trackingArea.contains(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)) {
            return;
        }
        List<SubTickState> states = currentTickStates.computeIfAbsent(pos.toImmutable(), k -> new ArrayList<>());
        // Only add if this sub-tick time is new or state is different
        if (states.isEmpty() || states.get(states.size() - 1).subTickTime < subTickTime
                || !states.get(states.size() - 1).state.equals(state)) {
            states.add(new SubTickState(state, subTickTime));
        }
    }

    /**
     * Get the interpolated animation state for a block at a given tickDelta
     * (sub-tick aware)
     * 
     * @param pos       Block position
     * @param tickDelta Interpolation factor (0.0 = previous tick, 1.0 = current
     *                  tick)
     * @return Interpolated animation state, or null if block is not tracked
     */
    public BlockAnimationState getInterpolatedState(BlockPos pos, float tickDelta) {
        if (!isTracking())
            return null;
        List<SubTickState> states = currentTickStates.get(pos);
        BlockAnimationState previous = previousTickStates.get(pos);
        if (states == null || states.isEmpty()) {
            return previous;
        }
        // Find the two sub-tick states that bracket tickDelta
        SubTickState before = null, after = null;
        for (SubTickState s : states) {
            if (s.subTickTime <= tickDelta)
                before = s;
            if (s.subTickTime >= tickDelta) {
                after = s;
                break;
            }
        }
        if (before == null)
            before = new SubTickState(previous, 0.0f);
        if (after == null)
            after = states.get(states.size() - 1);
        float localDelta = (tickDelta - before.subTickTime) / Math.max(0.0001f, after.subTickTime - before.subTickTime);
        return BlockAnimationState.interpolate(before.state, after.state, localDelta);
    }

    /**
     * At the end of each tick, move the last sub-tick state to previousTickStates
     * and clear the list
     */
    public void finalizeTick() {
        for (Map.Entry<BlockPos, List<SubTickState>> entry : currentTickStates.entrySet()) {
            List<SubTickState> states = entry.getValue();
            if (!states.isEmpty()) {
                previousTickStates.put(entry.getKey(), states.get(states.size() - 1).state);
            }
        }
        currentTickStates.clear();
    }

    // isTracking() and getTrackingArea() are inherited from AbstractTracker

    /**
     * Get all currently tracked block positions
     */
    public Iterable<BlockPos> getTrackedPositions() {
        return currentTickStates.keySet();
    }
}
