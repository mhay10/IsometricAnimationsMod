package com.isoanimations;

import com.isoanimations.commands.CreateAnimationCommand;
import com.isoanimations.commands.OpenAnimationsCommand;
import net.fabricmc.api.ClientModInitializer;


import static com.isoanimations.IsometricAnimations.LOGGER;

public class IsometricAnimationsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // This entrypoint is suitable for setting up client-specific logic, such as
        // rendering.
        LOGGER.info("Isometric Animations Mod Initializing...");
        CreateAnimationCommand.registerCommand();
        OpenAnimationsCommand.registerCommand();
    }
}