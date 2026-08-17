package com.peakskills.command;

import com.mojang.brigadier.CommandDispatcher;
import com.peakskills.gui.FishingJournalGui;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class FishingCommand {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            register(dispatcher, "fishing");
            register(dispatcher, "fishingjournal");
        });
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher, String name) {
        dispatcher.register(Commands.literal(name).executes(context -> {
            ServerPlayer player = context.getSource().getPlayer();
            if (player == null) {
                context.getSource().sendFailure(Component.literal("Must be run by a player"));
                return 0;
            }
            FishingJournalGui.open(player);
            return 1;
        }));
    }

    private FishingCommand() {}
}
