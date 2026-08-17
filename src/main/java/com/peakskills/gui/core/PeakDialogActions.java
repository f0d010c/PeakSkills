package com.peakskills.gui.core;

import com.mojang.brigadier.arguments.StringArgumentType;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

/** One-use, player-bound callbacks for actions submitted by vanilla dialogs. */
public final class PeakDialogActions {
    private static final long TTL_MILLIS = Duration.ofMinutes(2).toMillis();
    private static final int MAX_TOKEN_LENGTH = 36;
    private static final Map<UUID, Map<String, PendingAction>> ACTIONS = new HashMap<>();

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(Commands.literal("_peakskills_ui")
                .then(Commands.argument("token", StringArgumentType.word())
                    .executes(context -> consume(context.getSource().getPlayer(),
                        StringArgumentType.getString(context, "token"))))));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
            ACTIONS.remove(handler.getPlayer().getUUID()));
    }

    public static void begin(ServerPlayer player) {
        ACTIONS.put(player.getUUID(), new HashMap<>());
    }

    public static String issue(ServerPlayer player, Runnable action) {
        String token = UUID.randomUUID().toString();
        ACTIONS.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>())
            .put(token, new PendingAction(System.currentTimeMillis() + TTL_MILLIS, action));
        return token;
    }

    private static int consume(ServerPlayer player, String token) {
        if (player == null || token == null || token.length() > MAX_TOKEN_LENGTH) return 0;
        Map<String, PendingAction> playerActions = ACTIONS.get(player.getUUID());
        if (playerActions == null) return 0;

        PendingAction pending = playerActions.remove(token);
        if (pending == null || pending.expiresAt() < System.currentTimeMillis()) return 0;
        ACTIONS.remove(player.getUUID());
        pending.action().run();
        return 1;
    }

    private record PendingAction(long expiresAt, Runnable action) {}

    private PeakDialogActions() {}
}
