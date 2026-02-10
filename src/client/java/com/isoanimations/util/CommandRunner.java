package com.isoanimations.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class CommandRunner {
    public static void runCommand(String command) {
        // Run the command as the player
        LocalPlayer player = Minecraft.getInstance().player;
        assert player != null;

        if (command.startsWith("/"))
            player.connection.sendCommand(command.substring(1));
        else
            player.connection.sendCommand(command);
    }
}
