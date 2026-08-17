package com.peakskills.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.peakskills.gui.ProfileGui;
import com.peakskills.player.PlayerDataManager;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class ProfileCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(
                Commands.literal("profile")

                    // /profile — view your own profile
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        ProfileGui.open(player);
                        return 1;
                    })

                    // /profile <player> — view another player's profile
                    .then(Commands.argument("player", StringArgumentType.word())
                        .executes(ctx -> {
                            ServerPlayer viewer = ctx.getSource().getPlayerOrException();
                            String name = StringArgumentType.getString(ctx, "player");
                            ServerPlayer target = ctx.getSource().getServer()
                                .getPlayerList().getPlayerByName(name);
                            if (target == null) {
                                ctx.getSource().sendFailure(Component.literal("Player not found: " + name));
                                return 0;
                            }
                            ProfileGui.open(viewer,
                                PlayerDataManager.get(target.getUUID()),
                                target.getName().getString());
                            return 1;
                        })
                    )
            )
        );
    }
}
