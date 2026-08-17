package com.peakskills.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.UUID;
import com.peakskills.config.PeakConfig;
import com.peakskills.fishing.event.FishingCommunityEventManager;
import com.peakskills.gui.SkillsGui;
import com.peakskills.player.PlayerData;
import com.peakskills.player.PlayerDataFailsafe;
import com.peakskills.player.PlayerDataManager;
import com.peakskills.skill.Skill;
import com.peakskills.skill.XPTable;
import com.peakskills.stat.StatManager;
import com.peakskills.xp.XpManager;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;

public class SkillsCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(
                Commands.literal("skills")

                    // /skills — open your own GUI
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        SkillsGui.open(player);
                        return 1;
                    })

                    // /skills <player> — view another player's skills
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
                            SkillsGui.open(viewer, PlayerDataManager.get(target.getUUID()),
                                target.getName().getString());
                            return 1;
                        })
                    )

                    // --- ADMIN SUBCOMMANDS (requires op level 2) ---

                    // /skills addxp <player> <skill> <amount>
                    .then(Commands.literal("addxp")
                        .requires(SkillsCommand::isOp)
                        .then(Commands.argument("player", StringArgumentType.word())
                            .then(Commands.argument("skill", StringArgumentType.word())
                                .then(Commands.argument("amount", LongArgumentType.longArg(1, 10_000_000L))
                                    .executes(ctx -> {
                                        ServerPlayer target = resolvePlayer(ctx.getSource().getServer(),
                                            StringArgumentType.getString(ctx, "player"));
                                        if (target == null) { ctx.getSource().sendFailure(Component.literal("Player not found")); return 0; }
                                        Skill skill = resolveSkill(StringArgumentType.getString(ctx, "skill"));
                                        if (skill == null) { ctx.getSource().sendFailure(Component.literal("Unknown skill")); return 0; }
                                        long amount = LongArgumentType.getLong(ctx, "amount");
                                        XpManager.addXp(target, skill, amount);
                                        ctx.getSource().sendSuccess(() -> Component.literal("Added " + amount + " " + skill.getDisplayName() + " XP to " + target.getName().getString()).withStyle(ChatFormatting.GREEN), true);
                                        return 1;
                                    })
                                )
                            )
                        )
                    )

                    // /skills setlevel <player> <skill> <level>
                    .then(Commands.literal("setlevel")
                        .requires(SkillsCommand::isOp)
                        .then(Commands.argument("player", StringArgumentType.word())
                            .then(Commands.argument("skill", StringArgumentType.word())
                                .then(Commands.argument("level", IntegerArgumentType.integer(1, Skill.MAX_LEVEL))
                                    .executes(ctx -> {
                                        ServerPlayer target = resolvePlayer(ctx.getSource().getServer(),
                                            StringArgumentType.getString(ctx, "player"));
                                        if (target == null) { ctx.getSource().sendFailure(Component.literal("Player not found")); return 0; }
                                        Skill skill = resolveSkill(StringArgumentType.getString(ctx, "skill"));
                                        if (skill == null) { ctx.getSource().sendFailure(Component.literal("Unknown skill")); return 0; }
                                        int level = IntegerArgumentType.getInteger(ctx, "level");
                                        PlayerData data = PlayerDataManager.get(target.getUUID());
                                        // Set XP to exactly what's needed for this level (works up and down)
                                        long needed = XPTable.xpForLevel(level);
                                        long current = data.getXp(skill);
                                        long delta = needed - current;
                                        if (delta != 0) data.addXp(skill, delta);
                                        StatManager.applyStats(target);
                                        ctx.getSource().sendSuccess(() -> Component.literal("Set " + target.getName().getString() + "'s " + skill.getDisplayName() + " to level " + level).withStyle(ChatFormatting.GREEN), true);
                                        return 1;
                                    })
                                )
                            )
                        )
                    )

                    // /skills reset <player> [skill]
                    .then(Commands.literal("reset")
                        .requires(SkillsCommand::isOp)
                        .then(Commands.argument("player", StringArgumentType.word())
                            // /skills reset <player> — reset ALL skills
                            .executes(ctx -> {
                                ServerPlayer target = resolvePlayer(ctx.getSource().getServer(),
                                    StringArgumentType.getString(ctx, "player"));
                                if (target == null) { ctx.getSource().sendFailure(Component.literal("Player not found")); return 0; }
                                PlayerData data = PlayerDataManager.get(target.getUUID());
                                for (Skill skill : Skill.values()) {
                                    long xp = data.getXp(skill);
                                    if (xp > 0) data.addXp(skill, -xp);
                                }
                                StatManager.applyStats(target);
                                ctx.getSource().sendSuccess(() -> Component.literal("Reset all skills for " + target.getName().getString()).withStyle(ChatFormatting.YELLOW), true);
                                return 1;
                            })
                            // /skills reset <player> <skill> — reset ONE skill
                            .then(Commands.argument("skill", StringArgumentType.word())
                                .executes(ctx -> {
                                    ServerPlayer target = resolvePlayer(ctx.getSource().getServer(),
                                        StringArgumentType.getString(ctx, "player"));
                                    if (target == null) { ctx.getSource().sendFailure(Component.literal("Player not found")); return 0; }
                                    Skill skill = resolveSkill(StringArgumentType.getString(ctx, "skill"));
                                    if (skill == null) { ctx.getSource().sendFailure(Component.literal("Unknown skill")); return 0; }
                                    PlayerData data = PlayerDataManager.get(target.getUUID());
                                    long xp = data.getXp(skill);
                                    if (xp > 0) data.addXp(skill, -xp);
                                    StatManager.applyStats(target);
                                    ctx.getSource().sendSuccess(() -> Component.literal("Reset " + skill.getDisplayName() + " for " + target.getName().getString()).withStyle(ChatFormatting.YELLOW), true);
                                    return 1;
                                })
                            )
                        )
                    )

                    // /skills removexp <player> <skill> <amount>
                    .then(Commands.literal("removexp")
                        .requires(SkillsCommand::isOp)
                        .then(Commands.argument("player", StringArgumentType.word())
                            .then(Commands.argument("skill", StringArgumentType.word())
                                .then(Commands.argument("amount", LongArgumentType.longArg(1, 10_000_000L))
                                    .executes(ctx -> {
                                        ServerPlayer target = resolvePlayer(ctx.getSource().getServer(),
                                            StringArgumentType.getString(ctx, "player"));
                                        if (target == null) { ctx.getSource().sendFailure(Component.literal("Player not found")); return 0; }
                                        Skill skill = resolveSkill(StringArgumentType.getString(ctx, "skill"));
                                        if (skill == null) { ctx.getSource().sendFailure(Component.literal("Unknown skill")); return 0; }
                                        long amount = LongArgumentType.getLong(ctx, "amount");
                                        PlayerData data = PlayerDataManager.get(target.getUUID());
                                        long current = data.getXp(skill);
                                        long remove = Math.min(amount, current); // can't go below 0
                                        if (remove > 0) data.addXp(skill, -remove);
                                        StatManager.applyStats(target);
                                        ctx.getSource().sendSuccess(() -> Component.literal("Removed " + remove + " " + skill.getDisplayName() + " XP from " + target.getName().getString()).withStyle(ChatFormatting.YELLOW), true);
                                        return 1;
                                    })
                                )
                            )
                        )
                    )
                    // /skills backup [player]
                    .then(Commands.literal("backup")
                        .requires(SkillsCommand::isOp)
                        // /skills backup — back up ALL online players
                        .executes(ctx -> {
                            var source = ctx.getSource();
                            int count = 0;
                            for (ServerPlayer p : source.getServer().getPlayerList().getPlayers()) {
                                if (doBackup(source, p) == 1) count++;
                            }
                            int finalCount = count;
                            source.sendSuccess(() -> Component.literal("Backed up " + finalCount + " player(s).").withStyle(ChatFormatting.GREEN), true);
                            return finalCount;
                        })
                        // /skills backup <player> — back up a specific player
                        .then(Commands.argument("player", StringArgumentType.word())
                            .executes(ctx -> {
                                String name = StringArgumentType.getString(ctx, "player");
                                ServerPlayer target = resolvePlayer(ctx.getSource().getServer(), name);
                                if (target == null) { ctx.getSource().sendFailure(Component.literal("Player not found: " + name)); return 0; }
                                return doBackup(ctx.getSource(), target);
                            })
                        )
                    )

                    // /skills restore <player>
                    .then(Commands.literal("restore")
                        .requires(SkillsCommand::isOp)
                        .then(Commands.argument("player", StringArgumentType.word())
                            .executes(ctx -> {
                                String name = StringArgumentType.getString(ctx, "player");
                                ServerPlayer target = resolvePlayer(ctx.getSource().getServer(), name);
                                if (target == null) { ctx.getSource().sendFailure(Component.literal("Player not found: " + name)); return 0; }
                                return doRestore(ctx.getSource(), target);
                            })
                        )
                    )
                    // /skills fishingevent <status|start|stop>
                    .then(Commands.literal("fishingevent")
                        .requires(SkillsCommand::isOp)
                        .then(Commands.literal("status")
                            .executes(ctx -> {
                                ctx.getSource().sendSuccess(FishingCommunityEventManager::statusText, false);
                                return 1;
                            })
                        )
                        .then(Commands.literal("start")
                            .executes(ctx -> startFishingEvent(ctx.getSource(),
                                PeakConfig.get().defaultCommunityFishingGoal,
                                PeakConfig.get().defaultCommunityFishingMinutes))
                            .then(Commands.argument("goal", IntegerArgumentType.integer(1, 1_000_000))
                                .then(Commands.argument("minutes", IntegerArgumentType.integer(1, 1440))
                                    .executes(ctx -> startFishingEvent(ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "goal"),
                                        IntegerArgumentType.getInteger(ctx, "minutes")))
                                )
                            )
                        )
                        .then(Commands.literal("stop")
                            .executes(ctx -> {
                                boolean stopped = FishingCommunityEventManager.stop(ctx.getSource().getServer());
                                if (!stopped) {
                                    ctx.getSource().sendFailure(Component.literal("No Fishing Event is active."));
                                    return 0;
                                }
                                return 1;
                            })
                        )
                    )
            )
        );

        // /skilltop [count] — top players by combined skill level
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(
                Commands.literal("skilltop")
                    .executes(ctx -> sendSkillTop(ctx.getSource(), 10))
                    .then(Commands.argument("count", IntegerArgumentType.integer(1, 50))
                        .executes(ctx -> sendSkillTop(ctx.getSource(),
                            IntegerArgumentType.getInteger(ctx, "count")))
                    )
            )
        );

        // /skillrank — show the calling player's rank in every skill
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(
                Commands.literal("skillrank")
                    .executes(ctx -> sendSkillRank(ctx.getSource()))
            )
        );
    }

    // ── /skilltop ─────────────────────────────────────────────────────────────

    private static int sendSkillTop(net.minecraft.commands.CommandSourceStack source, int count) {
        var entries = PlayerDataManager.getLeaderboardByLevel(count);
        net.minecraft.server.MinecraftServer server = source.getServer();
        int total = entries.size();

        source.sendSuccess(() -> Component.literal(" "), false);
        source.sendSuccess(() -> Component.literal("  ✦ Skill Leaderboard ✦")
            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        source.sendSuccess(() -> Component.literal("  ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬")
            .withStyle(ChatFormatting.DARK_GRAY), false);

        if (entries.isEmpty()) {
            source.sendSuccess(() -> Component.literal("  No data yet.").withStyle(ChatFormatting.DARK_GRAY), false);
            return 1;
        }

        String[] medals = { "✦", "✦", "✦" };
        ChatFormatting[] rankColors = { ChatFormatting.GOLD, ChatFormatting.GRAY, ChatFormatting.RED };

        for (int i = 0; i < entries.size(); i++) {
            var entry = entries.get(i);
            int rank = i + 1;
            int level = entry.getValue();
            String name = resolveDisplayName(server, entry.getKey());

            Component line;
            if (rank <= 3) {
                ChatFormatting col = rankColors[rank - 1];
                line = Component.literal("  " + medals[rank - 1] + " #" + rank + "  ").withStyle(col, ChatFormatting.BOLD)
                    .append(Component.literal(name).withStyle(ChatFormatting.WHITE))
                    .append(Component.literal("  ").withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.literal("Lvl " + String.format("%,d", level)).withStyle(col));
            } else {
                line = Component.literal("  #" + rank + "  ").withStyle(ChatFormatting.DARK_GRAY)
                    .append(Component.literal(name).withStyle(ChatFormatting.WHITE))
                    .append(Component.literal("  Lvl " + String.format("%,d", level)).withStyle(ChatFormatting.DARK_GRAY));
            }
            Component finalLine = line;
            source.sendSuccess(() -> finalLine, false);
        }
        source.sendSuccess(() -> Component.literal("  ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬")
            .withStyle(ChatFormatting.DARK_GRAY), false);
        source.sendSuccess(() -> Component.literal(" "), false);
        return 1;
    }

    // ── /skillrank ────────────────────────────────────────────────────────────

    private static int sendSkillRank(net.minecraft.commands.CommandSourceStack source) {
        ServerPlayer player;
        try { player = source.getPlayerOrException(); } catch (Exception e) { return 0; }
        UUID uuid = player.getUUID();
        net.minecraft.server.MinecraftServer server = source.getServer();

        source.sendSuccess(() -> Component.literal(" "), false);
        source.sendSuccess(() -> Component.literal("  ✦ Your Skill Rankings ✦")
            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        source.sendSuccess(() -> Component.literal("  ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬")
            .withStyle(ChatFormatting.DARK_GRAY), false);

        for (Skill skill : Skill.values()) {
            var board = PlayerDataManager.getSkillLeaderboard(skill);
            int total = board.size();
            int rank = -1;
            int myLevel = 0;
            for (int i = 0; i < board.size(); i++) {
                if (board.get(i).getKey().equals(uuid)) {
                    rank = i + 1;
                    myLevel = board.get(i).getValue();
                    break;
                }
            }
            if (rank == -1) { rank = total + 1; }

            String ordinal = ordinal(rank);
            ChatFormatting rankColor = rank == 1 ? ChatFormatting.GOLD : rank <= 3 ? ChatFormatting.YELLOW : rank <= 10 ? ChatFormatting.GREEN : ChatFormatting.GRAY;
            int finalRank = rank;
            int finalLevel = myLevel;
            source.sendSuccess(() ->
                Component.literal("  " + padRight(skill.getDisplayName(), 12)).withStyle(ChatFormatting.WHITE)
                    .append(Component.literal(ordinal + " / " + total).withStyle(rankColor))
                    .append(Component.literal("  (Lv " + finalLevel + ")").withStyle(ChatFormatting.DARK_GRAY)),
            false);
        }

        source.sendSuccess(() -> Component.literal("  ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬")
            .withStyle(ChatFormatting.DARK_GRAY), false);
        source.sendSuccess(() -> Component.literal(" "), false);
        return 1;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String resolveDisplayName(net.minecraft.server.MinecraftServer server, UUID uuid) {
        ServerPlayer online = server.getPlayerList().getPlayer(uuid);
        if (online != null) return online.getName().getString();
        // Fallback: short UUID prefix (user cache API varies by version)
        return uuid.toString().substring(0, 8);
    }

    private static String ordinal(int n) {
        if (n >= 11 && n <= 13) return n + "th";
        return switch (n % 10) {
            case 1 -> n + "st";
            case 2 -> n + "nd";
            case 3 -> n + "rd";
            default -> n + "th";
        };
    }

    private static String padRight(String s, int length) {
        return s.length() >= length ? s : s + " ".repeat(length - s.length());
    }

    private static int doBackup(net.minecraft.commands.CommandSourceStack src, ServerPlayer target) {
        try {
            java.nio.file.Path file = PlayerDataFailsafe.backup(target.getUUID(), src.getServer());
            src.sendSuccess(() -> Component.literal("Backed up " + target.getName().getString()
                + " → " + file.getFileName()).withStyle(ChatFormatting.GREEN), true);
            return 1;
        } catch (Exception e) {
            src.sendFailure(Component.literal("Backup failed: " + e.getMessage()));
            return 0;
        }
    }

    private static int doRestore(net.minecraft.commands.CommandSourceStack src, ServerPlayer target) {
        try {
            var result = PlayerDataFailsafe.restore(target.getUUID(), src.getServer());
            if (result.isEmpty()) {
                src.sendFailure(Component.literal("No backup found for " + target.getName().getString()));
                return 0;
            }
            StatManager.applyStats(target);
            java.nio.file.Path file = result.get();
            src.sendSuccess(() -> Component.literal("Restored " + target.getName().getString()
                + " from " + file.getFileName()).withStyle(ChatFormatting.GREEN), true);
            return 1;
        } catch (Exception e) {
            src.sendFailure(Component.literal("Restore failed: " + e.getMessage()));
            return 0;
        }
    }

    private static int startFishingEvent(CommandSourceStack source, int goal, int minutes) {
        boolean started = FishingCommunityEventManager.start(source.getServer(), goal, minutes);
        if (!started) {
            source.sendFailure(Component.literal("Fishing Event could not start. It may already be active or disabled."));
            return 0;
        }
        return 1;
    }

    private static boolean isOp(CommandSourceStack src) {
        ServerPlayer player = src.getPlayer();
        if (player == null) return !src.isPlayer();
        NameAndId entry = new NameAndId(player.getGameProfile());
        return src.getServer().getPlayerList().getOps().get(entry) != null;
    }

    private static ServerPlayer resolvePlayer(net.minecraft.server.MinecraftServer server, String name) {
        return server.getPlayerList().getPlayerByName(name);
    }

    private static Skill resolveSkill(String name) {
        try {
            return Skill.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
