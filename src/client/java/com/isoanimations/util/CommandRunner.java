package com.isoanimations.util;

import net.minecraft.client.MinecraftClient;

public class CommandRunner {
    public static void runCommand(String command) {
        var player = MinecraftClient.getInstance().player;
        assert player != null;

        if (command.startsWith("/"))
            player.networkHandler.sendChatCommand(command.substring(1));
        else
            player.networkHandler.sendChatMessage(command);

    }
}
