package com.isoanimations;

import com.isoanimations.commands.CreateAnimationCommand;
import com.isoanimations.config.RenderConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;

import static com.isoanimations.IsometricAnimations.LOGGER;

public class IsometricAnimationsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing Isometric Animations Mod...");

        // Load configurations from file
        RenderConfig.loadConfig();

        // Register client-side commands
        CreateAnimationCommand.registerCommand();
    }
}