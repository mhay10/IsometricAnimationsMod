package com.isoanimations.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.util.ActionResult;

public class TickStepEvent {
    public static final Event<TickStepCallback> TICK_STEP_EVENT = EventFactory.createArrayBacked(
            TickStepCallback.class,
            (listeners) -> (source, steps) -> {
                for (TickStepCallback listener : listeners) {
                    ActionResult result = listener.onTickStep(source, steps);
                    if (result != ActionResult.PASS) {
                        return result;
                    }
                }
                return ActionResult.SUCCESS;
            }
    );
}
