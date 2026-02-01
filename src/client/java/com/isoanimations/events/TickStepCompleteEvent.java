package com.isoanimations.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.util.ActionResult;
import net.minecraft.server.command.ServerCommandSource;

public class TickStepCompleteEvent {
    public interface TickStepCompleteCallback {
        ActionResult onTickComplete(ServerCommandSource source, int steps);
    }

    public static final Event<TickStepCompleteCallback> TICK_STEP_COMPLETE_EVENT = EventFactory.createArrayBacked(
            TickStepCompleteCallback.class,
            (listeners) -> (source, steps) -> {
                for (TickStepCompleteCallback listener : listeners) {
                    ActionResult result = listener.onTickComplete(source, steps);
                    if (result != ActionResult.PASS) {
                        return result;
                    }
                }
                return ActionResult.SUCCESS;
            });
}
