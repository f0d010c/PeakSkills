package com.peakskills.command;

import com.mojang.brigadier.CommandDispatcher;
import com.peakskills.gui.CollectionsGui;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class CollectionsCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            register(dispatcher)
        );
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("collections")
                .executes(ctx -> {
                    CommandSourceStack source = ctx.getSource();
                    if (source.getPlayer() == null) {
                        source.sendFailure(Component.literal("Must be run by a player"));
                        return 0;
                    }
                    ServerPlayer player = source.getPlayer();
                    CollectionsGui.open(player);
                    return 1;
                })
        );
    }
}
