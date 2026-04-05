package com.peakskills.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.peakskills.gui.ProfileGui;
import com.peakskills.player.PlayerDataManager;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class ProfileCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(
                CommandManager.literal("profile")

                    // /profile — view your own profile
                    .executes(ctx -> {
                        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                        ProfileGui.open(player);
                        return 1;
                    })

                    // /profile <player> — view another player's profile
                    .then(CommandManager.argument("player", StringArgumentType.word())
                        .executes(ctx -> {
                            ServerPlayerEntity viewer = ctx.getSource().getPlayerOrThrow();
                            String name = StringArgumentType.getString(ctx, "player");
                            ServerPlayerEntity target = ctx.getSource().getServer()
                                .getPlayerManager().getPlayer(name);
                            if (target == null) {
                                ctx.getSource().sendError(Text.literal("Player not found: " + name));
                                return 0;
                            }
                            ProfileGui.open(viewer,
                                PlayerDataManager.get(target.getUuid()),
                                target.getName().getString());
                            return 1;
                        })
                    )
            )
        );
    }
}
