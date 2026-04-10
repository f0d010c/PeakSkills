package com.peakskills.crafting;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Registers /craft — opens the custom recipe GUI.
 * Player-facing: no OP required.
 */
public class PeakCraftingCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(CommandManager.literal("craft")
                .executes(ctx -> {
                    ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                    PeakCraftingGui.open(player);
                    return 1;
                })
            )
        );
    }
}
