package com.peakskills.command;

import com.mojang.brigadier.CommandDispatcher;
import com.peakskills.gui.CollectionsGui;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class CollectionsCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            register(dispatcher)
        );
    }

    private static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            CommandManager.literal("collections")
                .executes(ctx -> {
                    ServerCommandSource source = ctx.getSource();
                    if (source.getPlayer() == null) {
                        source.sendError(Text.literal("Must be run by a player"));
                        return 0;
                    }
                    ServerPlayerEntity player = source.getPlayer();
                    CollectionsGui.open(player);
                    return 1;
                })
        );
    }
}
