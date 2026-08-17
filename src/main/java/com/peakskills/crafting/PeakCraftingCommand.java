package com.peakskills.crafting;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

/**
 * Registers /craft — opens the custom recipe GUI.
 * Player-facing: no OP required.
 */
public class PeakCraftingCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(Commands.literal("craft")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    PeakCraftingGui.open(player);
                    return 1;
                })
            )
        );
    }
}
