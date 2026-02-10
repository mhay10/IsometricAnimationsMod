package com.isoanimations;

import com.isoanimations.commands.CreateAnimationCommand;
import net.fabricmc.api.ClientModInitializer;

public class IsometricAnimationsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Register client-side commands
        CreateAnimationCommand.registerCommand();
    }
}