package com.isoanimations.config;

import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.arguments.coordinates.WorldCoordinates;
import net.minecraft.core.BlockPos;

import java.util.Objects;

public record AnimationConfig(BlockPos pos1, BlockPos pos2, int scale, int pitch, int yaw, double duration) {
    public static AnimationConfig parse(CommandContext<FabricClientCommandSource> context, boolean includeDuration) {
        BlockPos pos1 = getBlockPosArgument(context, "pos1");
        BlockPos pos2 = getBlockPosArgument(context, "pos2");
        int scale = context.getArgument("scale", Integer.class);
        int pitch = context.getArgument("pitch", Integer.class);
        int yaw = context.getArgument("yaw", Integer.class);
        double duration = includeDuration ? context.getArgument("duration", Double.class) : 0.0;

        return new AnimationConfig(pos1, pos2, scale, pitch, yaw, duration);
    }

    private static BlockPos getBlockPosArgument(CommandContext<FabricClientCommandSource> context, String name) {
        WorldCoordinates coords = context.getArgument(name, WorldCoordinates.class);
        return coords.getBlockPos(Objects.requireNonNull(context.getSource().getClient().getSingleplayerServer().createCommandSourceStack())); // wtf is this
    }
}
