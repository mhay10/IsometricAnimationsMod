package com.isoanimations.commands;


import com.glisco.isometricrenders.mixin.access.DefaultPosArgumentAccessor;
import com.isoanimations.util.AnimationAssembler;
import com.isoanimations.util.AnimationFrameGenerator;
import com.isoanimations.util.SubTickConfig;
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

import static com.isoanimations.IsometricAnimations.LOGGER;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback.EVENT;


// Test command: /animate -4 -51 5 5 -59 4 125 200 20 1.5 60
// FPS parameter is optional, defaults to 60
public class CreateAnimationCommand {
    public static void registerCommand() {
        LOGGER.info("Registering /animate client command...");
        EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("animate")
                    // Help subcommand
                    .then(ClientCommandManager.literal("help")
                            .executes(CreateAnimationCommand::showHelp))
                    // Main command with arguments
                    .then(ClientCommandManager.argument("pos1", BlockPosArgumentType.blockPos())
                            .then(ClientCommandManager.argument("pos2", BlockPosArgumentType.blockPos())
                                    .then(ClientCommandManager.argument("scale", IntegerArgumentType.integer(0, 500))
                                            .then(ClientCommandManager.argument("rotation", IntegerArgumentType.integer(0, 360))
                                                    .then(ClientCommandManager.argument("slant", IntegerArgumentType.integer(-90, 90))
                                                            .then(ClientCommandManager.argument("duration", DoubleArgumentType.doubleArg(0))
                                                                    .executes(CreateAnimationCommand::execute)
                                                                    .then(ClientCommandManager.argument("fps", IntegerArgumentType.integer(20, 1000))
                                                                            .executes(CreateAnimationCommand::executeWithFPS)))))))));
            LOGGER.info("Successfully registered /animate client command");
        });
    }

    public static int showHelp(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(Text.literal("=== Isometric Animation Command Help ===").styled(style -> style.withBold(true).withColor(0x00FFFF)));
        context.getSource().sendFeedback(Text.literal(""));
        context.getSource().sendFeedback(Text.literal("Usage: ").styled(style -> style.withColor(0xFFFF00))
                .append(Text.literal("/animate <pos1> <pos2> <scale> <rotation> <slant> <duration> [fps]").styled(style -> style.withColor(0xFFFFFF))));
        context.getSource().sendFeedback(Text.literal(""));
        context.getSource().sendFeedback(Text.literal("Parameters:").styled(style -> style.withBold(true)));
        context.getSource().sendFeedback(Text.literal("  pos1, pos2").styled(style -> style.withColor(0x00FF00))
                .append(Text.literal(" - Corner positions of the area to render").styled(style -> style.withColor(0xAAAAAA))));
        context.getSource().sendFeedback(Text.literal("  scale").styled(style -> style.withColor(0x00FF00))
                .append(Text.literal(" - Render scale (0-500)").styled(style -> style.withColor(0xAAAAAA))));
        context.getSource().sendFeedback(Text.literal("  rotation").styled(style -> style.withColor(0x00FF00))
                .append(Text.literal(" - Camera rotation angle (0-360)").styled(style -> style.withColor(0xAAAAAA))));
        context.getSource().sendFeedback(Text.literal("  slant").styled(style -> style.withColor(0x00FF00))
                .append(Text.literal(" - Camera slant angle (-90 to 90)").styled(style -> style.withColor(0xAAAAAA))));
        context.getSource().sendFeedback(Text.literal("  duration").styled(style -> style.withColor(0x00FF00))
                .append(Text.literal(" - Animation duration in seconds").styled(style -> style.withColor(0xAAAAAA))));
        context.getSource().sendFeedback(Text.literal("  fps").styled(style -> style.withColor(0x00FF00))
                .append(Text.literal(" - [OPTIONAL] Frames per second (20-1000, default: 1000)").styled(style -> style.withColor(0xAAAAAA))));
        context.getSource().sendFeedback(Text.literal(""));
        context.getSource().sendFeedback(Text.literal("Important Notes:").styled(style -> style.withBold(true).withColor(0xFF5555)));
        context.getSource().sendFeedback(Text.literal("  • Set up your animation BEFORE freezing").styled(style -> style.withColor(0xFFAAAA)));
        context.getSource().sendFeedback(Text.literal("  • Use '/tick freeze' to freeze the game").styled(style -> style.withColor(0xFFAAAA)));
        context.getSource().sendFeedback(Text.literal("  • Then run this command while frozen").styled(style -> style.withColor(0xFFAAAA)));
        context.getSource().sendFeedback(Text.literal(""));
        context.getSource().sendFeedback(Text.literal("Example:").styled(style -> style.withBold(true)));
        context.getSource().sendFeedback(Text.literal("  /animate -4 -51 5 5 -59 4 125 200 20 1.5 60").styled(style -> style.withColor(0x55FF55)));
        return 1;
    }

    public static int execute(CommandContext<FabricClientCommandSource> context) {
        // Default to 1000 FPS for smoothest animation
        SubTickConfig.setTargetFPS(1000);
        return executeAnimation(context);
    }

    public static int executeWithFPS(CommandContext<FabricClientCommandSource> context) {
        // Get FPS from command argument
        int fps = context.getArgument("fps", Integer.class);
        SubTickConfig.setTargetFPS(fps);
        return executeAnimation(context);
    }

    private static int executeAnimation(CommandContext<FabricClientCommandSource> context) {
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
            context.getSource().sendError(Text.literal("Important: Set up your animation BEFORE freezing, then freeze and run /animate"));
            return 0;
        }

        // Set callback to create animation after frame generation is complete
        AnimationFrameGenerator frameGenerator = new AnimationFrameGenerator(context.getSource(), pos1, pos2, scale, rotation, slant, duration);
        frameGenerator.setCompletionCallback(totalFrames -> {
            try {
                new AnimationAssembler(context.getSource(), totalFrames).createAnimation();
            } catch (InterruptedException ignored) {
            }
        });

        // Start frame generation
        frameGenerator.start();

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
