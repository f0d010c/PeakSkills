package com.peakskills.command;

import com.peakskills.gui.SettingsGui;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

public class SettingsCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(
                Commands.literal("settings")
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        SettingsGui.open(player);
                        return 1;
                    })
            )
        );
    }
}
