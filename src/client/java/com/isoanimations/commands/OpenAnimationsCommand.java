package com.isoanimations.commands;

import com.isoanimations.util.ExportConfig;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.awt.Desktop;
import java.io.File;

import static com.isoanimations.IsometricAnimations.LOGGER;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback.EVENT;

public class OpenAnimationsCommand {
    public static void registerCommand() {
        LOGGER.info("Registering /openanimations client command...");
        EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("openanimations")
                    .executes(OpenAnimationsCommand::execute));
            LOGGER.info("Successfully registered /openanimations client command");
        });
    }

    public static int execute(CommandContext<FabricClientCommandSource> context) {
        try {
            File animationsFolder = ExportConfig.ANIMATION_EXPORT_DIR.toFile();

            // Create the folder if it doesn't exist
            if (!animationsFolder.exists()) {
                animationsFolder.mkdirs();
            }

            // Check if Desktop is supported on this platform
            if (!Desktop.isDesktopSupported()) {
                context.getSource().sendError(Text.literal("Desktop operations are not supported on this platform"));
                LOGGER.warn("Desktop operations not supported on this platform");
                return 0;
            }

            Desktop desktop = Desktop.getDesktop();

            // Check if the open action is supported
            if (!desktop.isSupported(Desktop.Action.OPEN)) {
                context.getSource().sendError(Text.literal("Opening folders is not supported on this platform"));
                LOGGER.warn("Desktop.OPEN action not supported on this platform");
                return 0;
            }

            // Open the animations folder
            desktop.open(animationsFolder);
            LOGGER.info("Opened animations folder: {}", animationsFolder.getAbsolutePath());

            return 1;
        } catch (Exception e) {
            LOGGER.error("Failed to open animations folder", e);
            context.getSource().sendError(Text.literal("Failed to open animations folder: " + e.getMessage()));
            return 0;
        }
    }
}

