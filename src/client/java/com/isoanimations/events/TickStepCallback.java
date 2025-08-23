package com.isoanimations.events;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.ActionResult;

@FunctionalInterface
public interface TickStepCallback {
    ActionResult onTickStep(ServerCommandSource source, int steps);
}