package com.isoanimations.render;

import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.PistonBlockEntity;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Factory for creating BlockAnimationState objects from world block data.
 * Handles detection and state extraction for all animated block types.
 */
public class BlockAnimationStateFactory {

    /**
     * Create an animation state for a block based on its current world state
     */
    public static BlockAnimationState createFromWorld(World world, BlockPos pos, BlockState state) {
        Block block = state.getBlock();

        // Check for piston
        if (block instanceof PistonBlock || block instanceof PistonHeadBlock) {
            return createPistonState(world, pos, state);
        }

        // Check for moving block (block being pushed by piston)
        if (block instanceof PistonExtensionBlock) {
            return createMovingBlockState(world, pos, state);
        }

        // Check for door
        if (block instanceof DoorBlock) {
            return createOpenableState(pos, state, BlockAnimationState.AnimationType.DOOR);
        }

        // Check for trapdoor
        if (block instanceof TrapdoorBlock) {
            return createOpenableState(pos, state, BlockAnimationState.AnimationType.TRAPDOOR);
        }

        // Check for fence gate
        if (block instanceof FenceGateBlock) {
            return createOpenableState(pos, state, BlockAnimationState.AnimationType.FENCE_GATE);
        }

        // Default: stationary block
        return BlockAnimationState.stationary(pos, state);
    }

    /**
     * Calculates the offset vector for a block based on its facing and progress.
     * This avoids code duplication for pistons and moving blocks.
     */
    private static Vec3d calculateOffset(Direction facing, float progress) {
        return new Vec3d(
            facing.getOffsetX() * progress,
            facing.getOffsetY() * progress,
            facing.getOffsetZ() * progress
        );
    }

    /**
     * Handles piston base and head animation state.
     * Adds comments to clarify logic.
     */
    private static BlockAnimationState createPistonState(World world, BlockPos pos, BlockState state) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        Block block = state.getBlock();
        boolean isSticky = isStickyPiston(block);

        Direction facing = state.get(Properties.FACING);
        boolean powered = state.contains(Properties.EXTENDED) && state.get(Properties.EXTENDED);

        if (blockEntity instanceof PistonBlockEntity pistonEntity) {
            float progress = pistonEntity.getProgress(1.0f); // Get current progress
            Vec3d offset = calculateOffset(facing, progress);


            // Diagnostic logging
            if (progress == 0.0f) {
                if (powered) {
                    com.isoanimations.IsometricAnimations.LOGGER.info("Piston at {} is extended and idle (progress=0.0)", pos);
                } else {
                    com.isoanimations.IsometricAnimations.LOGGER.info("Piston at {} is retracted and idle (progress=0.0)", pos);
                }
            } else {
                com.isoanimations.IsometricAnimations.LOGGER.info("Piston at {} animating: progress={} extended={}", pos, progress, powered);
            }

            return new BlockAnimationState(
                pos,
                state,
                offset,
                progress,
                BlockAnimationState.AnimationType.PISTON,
                isSticky
            );
        }

        // Fallback: try to infer piston state from neighbors if BlockEntity is missing
        BlockPos neighborPos = pos.offset(facing);
        BlockEntity neighborEntity = world.getBlockEntity(neighborPos);
        float fallbackProgress = 0.0f;
        if (neighborEntity instanceof PistonBlockEntity neighborPiston) {
            fallbackProgress = neighborPiston.getProgress(1.0f);
            com.isoanimations.IsometricAnimations.LOGGER.warn("[Piston] Fallback: Using neighbor piston entity at {} for {} (progress={})", neighborPos, pos, fallbackProgress);
        } else {
            com.isoanimations.IsometricAnimations.LOGGER.warn("No PistonBlockEntity at {} for piston state (blockState: {}) - expected during animation", pos, state);
        }
        Vec3d fallbackOffset = calculateOffset(facing, fallbackProgress);
        return new BlockAnimationState(
            pos,
            state,
            fallbackOffset,
            fallbackProgress,
            BlockAnimationState.AnimationType.PISTON,
            isSticky
        );
    }

    /**
     * Handles moving block animation state (block being pushed by piston).
     * Removes System.out.println for performance; add debug logging if needed.
     */
    private static BlockAnimationState createMovingBlockState(World world, BlockPos pos, BlockState state) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        Block block = state.getBlock();
        boolean isSticky = isStickyPiston(block);

        Direction facing = state.contains(Properties.FACING) ? state.get(Properties.FACING) : Direction.UP;
        BlockPos basePos = pos.offset(facing.getOpposite());
        BlockEntity baseEntity = world.getBlockEntity(basePos);
        boolean isFirstPushedBlock = false;
        if (baseEntity instanceof PistonBlockEntity) {
            // The block is directly in front of a piston
            isFirstPushedBlock = true;
        }

        if (blockEntity instanceof PistonBlockEntity pistonEntity) {
            float progress = pistonEntity.getProgress(1.0f);
            // If this is the first pushed block, reduce progress slightly to avoid overlap
            if (isFirstPushedBlock && progress > 0.0f && progress < 1.0f) {
                progress = Math.max(0.0f, progress - 0.01f);
            }
            Vec3d offset = calculateOffset(facing, progress);

            return new BlockAnimationState(
                pos,
                state,
                offset,
                progress,
                BlockAnimationState.AnimationType.MOVING_BLOCK,
                isSticky
            );
        }

        // Fallback: try to infer from piston base
        float fallbackProgress = 0.0f;
        if (baseEntity instanceof PistonBlockEntity basePiston) {
            fallbackProgress = basePiston.getProgress(1.0f);
            if (isFirstPushedBlock && fallbackProgress > 0.0f && fallbackProgress < 1.0f) {
                fallbackProgress = Math.max(0.0f, fallbackProgress - 0.01f);
            }
            com.isoanimations.IsometricAnimations.LOGGER.warn("[MovingBlock] Fallback: Using base piston entity at {} for {} (progress={})", basePos, pos, fallbackProgress);
        } else {
            com.isoanimations.IsometricAnimations.LOGGER.warn("No PistonBlockEntity at {} for moving block state (blockState: {}) - expected during animation", pos, state);
        }
        Vec3d fallbackOffset = calculateOffset(facing, fallbackProgress);
        return new BlockAnimationState(
            pos,
            state,
            fallbackOffset,
            fallbackProgress,
            BlockAnimationState.AnimationType.MOVING_BLOCK,
            isSticky
        );
    }

    /**
     * Handles openable blocks (doors, trapdoors, fence gates) with shared logic.
     * Reduces code duplication and clarifies intent.
     */
    private static BlockAnimationState createOpenableState(BlockPos pos, BlockState state, BlockAnimationState.AnimationType type) {
        boolean open = state.get(Properties.OPEN);
        float progress = open ? 1.0f : 0.0f;
        return new BlockAnimationState(
            pos,
            state,
            Vec3d.ZERO,
            progress,
            type
        );
    }


    /**
     * Checks if a block is a sticky piston by comparing its registry ID.
     */
    private static boolean isStickyPiston(Block block) {
        // Check registry ID for sticky piston
        return block instanceof PistonBlock && block.getTranslationKey().equals("block.minecraft.sticky_piston");
    }
}
