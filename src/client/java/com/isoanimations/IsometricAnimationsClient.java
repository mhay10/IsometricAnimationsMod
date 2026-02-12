package com.isoanimations;

import com.isoanimations.commands.CreateAnimationCommand;
import net.fabricmc.api.ClientModInitializer;

import static com.isoanimations.IsometricAnimations.LOGGER;

public class IsometricAnimationsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing Isometric Animations Mod...");

        // Register client-side commands
        CreateAnimationCommand.registerCommand();
    }
}