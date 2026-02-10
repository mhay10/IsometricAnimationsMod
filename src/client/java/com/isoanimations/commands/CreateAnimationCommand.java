package com.isoanimations.commands;

import com.isoanimations.animation.AnimationController;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.WorldCoordinates;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.Objects;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback.EVENT;

public class CreateAnimationCommand {
    public static void registerCommand() {
        EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(
                        ClientCommandManager.literal("animate")
                                .then(ClientCommandManager.argument("pos1", BlockPosArgument.blockPos())
                                        .then(ClientCommandManager.argument("pos2", BlockPosArgument.blockPos())
                                                .then(ClientCommandManager.argument("duration", IntegerArgumentType.integer())
                                                        .executes(CreateAnimationCommand::execute))))));
    }

    public static int execute(CommandContext<FabricClientCommandSource> context) {
        // Render only if game frozen with '/tick freeze'
        var source = context.getSource();
        boolean isFrozen = context.getSource().getWorld().tickRateManager().isFrozen();
        if (!isFrozen) {
            source.sendError(Component.literal("Game must be frozen to create animation. Use '/tick freeze' command first."));
            return 0;
        }

        // Get arguments from command context
        WorldCoordinates pos1 = context.getArgument("pos1", WorldCoordinates.class);
        WorldCoordinates pos2 = context.getArgument("pos2", WorldCoordinates.class);
        int duration = context.getArgument("duration", Integer.class);

        /*
         TODO: Initial implementation plan as commands
         /animate pos1 pos2 duration --> starts:
             /flashback start
             /tick step (duration)
             /flashback stop
             /tick unfreeze
         */

        // Convert arguments to BlockPos and ticks
        var blockPos1 = pos1.getBlockPos(Objects.requireNonNull(source.getClient().getSingleplayerServer()).createCommandSourceStack()); // wtf is this
        var blockPos2 = pos2.getBlockPos(Objects.requireNonNull(source.getClient().getSingleplayerServer()).createCommandSourceStack());
        int durationTicks = duration * 20; // Convert seconds to ticks

        source.sendFeedback(Component.literal("Region: (%s, %s, %s) to (%s, %s, %s), Duration: %d ticks".formatted(
                blockPos1.getX(), blockPos1.getY(), blockPos1.getZ(),
                blockPos2.getX(), blockPos2.getY(), blockPos2.getZ(),
                duration
        )));


        return 1;
    }
}
