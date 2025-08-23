package com.isoanimations.commands;


import com.glisco.isometricrenders.mixin.access.DefaultPosArgumentAccessor;
import com.isoanimations.util.AnimationHandler;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.DefaultPosArgument;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback.EVENT;


// Test command: /animate -4 -51 5 5 -59 4 125 200 20 1.5
public class CreateAnimationCommand {
    private static AnimationHandler animationHandler = null;


    public static void registerCommand() {
        EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("animate")
                        .then(ClientCommandManager.argument("pos1", BlockPosArgumentType.blockPos())
                                .then(ClientCommandManager.argument("pos2", BlockPosArgumentType.blockPos())
                                        .then(ClientCommandManager.argument("scale", IntegerArgumentType.integer(0, 500))
                                                .then(ClientCommandManager.argument("rotation", IntegerArgumentType.integer(0, 360))
                                                        .then(ClientCommandManager.argument("slant", IntegerArgumentType.integer(-90, 90))
                                                                .then(ClientCommandManager.argument("duration", DoubleArgumentType.doubleArg(0))
                                                                        .executes(CreateAnimationCommand::execute)))))))));
    }

    public static int execute(CommandContext<FabricClientCommandSource> context) {
        // Get arguments from the command context
        BlockPos pos1 = getPosFromArgument(context.getArgument("pos1", DefaultPosArgument.class), context.getSource());
        BlockPos pos2 = getPosFromArgument(context.getArgument("pos2", DefaultPosArgument.class), context.getSource());
        int scale = context.getArgument("scale", Integer.class);
        int rotation = context.getArgument("rotation", Integer.class);
        int slant = context.getArgument("slant", Integer.class);
        double duration = context.getArgument("duration", Double.class);

        // Render only if game frozen with '/tick freeze'
        boolean isFrozen = context.getSource().getWorld().getTickManager().isFrozen();
        if (!isFrozen) {
            context.getSource().sendError(Text.literal("Game must be frozen to create animation. Use '/tick freeze' command."));
            return 0;
        }

        // Render the first frame
        animationHandler = new AnimationHandler(context.getSource(), pos1, pos2, scale, rotation, slant, duration);
        animationHandler.start();
        context.getSource().sendFeedback(Text.literal("Starting animation...").formatted(Formatting.GREEN));

        return 1;
    }


    private static BlockPos getPosFromArgument(DefaultPosArgument arg, FabricClientCommandSource source) {
        // Get the player's position to use as a reference for the absolute coordinates
        DefaultPosArgumentAccessor accessor = (DefaultPosArgumentAccessor) arg;
        Vec3d pos = source.getPlayer().getPos();

        // Convert the relative coordinates to absolute coordinates using the accessor
        return BlockPos.ofFloored(
                accessor.isometric$getX().toAbsoluteCoordinate(pos.x),
                accessor.isometric$getY().toAbsoluteCoordinate(pos.y),
                accessor.isometric$getZ().toAbsoluteCoordinate(pos.z)
        );
    }
}
