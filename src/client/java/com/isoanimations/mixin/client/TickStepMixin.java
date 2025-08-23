package com.isoanimations.mixin.client;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.command.TickCommand;
import net.minecraft.util.ActionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.isoanimations.events.TickStepEvent.TICK_STEP_EVENT;

@Mixin(TickCommand.class)
public class TickStepMixin {
	// Inject into step method
	@Inject(at = @At("HEAD"), method = "executeStep", cancellable = true)
	private static void onTickStep(ServerCommandSource source, int steps, CallbackInfoReturnable<Integer> cir) {
		ActionResult result = TICK_STEP_EVENT.invoker().onTickStep(source, steps);
		if (result == ActionResult.FAIL)
			cir.setReturnValue(0); // Cancel the command
	}
}