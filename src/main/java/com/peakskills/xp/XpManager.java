package com.peakskills.xp;

import com.peakskills.pet.PetAbility;
import com.peakskills.pet.PetAbilityRegistry;
import com.peakskills.pet.PetInstance;
import java.util.List;
import com.peakskills.player.PlayerData;
import com.peakskills.player.PlayerDataManager;
import com.peakskills.skill.Skill;
import com.peakskills.skill.SkillAbilityRegistry;
import com.peakskills.skill.XPTable;
import com.peakskills.guide.UnlockNotificationManager;
import com.peakskills.stat.SkillStatSource;
import com.peakskills.stat.StatManager;
import com.peakskills.stat.StatRegistry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class XpManager {

    /** Ticks the boss bar stays visible after the last XP gain (~3 s). */
    private static final int BAR_DURATION = 60;
    private static final int LEVEL_UP_SOUND_LIMIT = 5;
    private static final long LEVEL_UP_SOUND_WINDOW_MS = 5 * 60 * 1000L;

    private static final Map<UUID, ServerBossEvent> activeBars   = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer>       barCountdown = new ConcurrentHashMap<>();
    private static final Map<UUID, Deque<Long>>   levelUpSoundWindow = new ConcurrentHashMap<>();

    /** Call once from PeakSkills.onInitialize() to register the tick cleaner. */
    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            // Decrement all countdowns (replaceAll is safe on ConcurrentHashMap)
            barCountdown.replaceAll((uuid, ticks) -> ticks - 1);
            // Remove any that have expired
            barCountdown.entrySet().removeIf(entry -> {
                if (entry.getValue() <= 0) {
                    ServerBossEvent bar = activeBars.remove(entry.getKey());
                    if (bar != null) bar.removeAllPlayers();
                    return true;
                }
                return false;
            });

            // Refresh stat action bar every 2 seconds for all online players
            if (server.getTickCount() % 40 == 0) {
                for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                    sendStatBar(p);
                }
            }
        });
    }

    private static void sendStatBar(ServerPlayer player) {
        long hp    = Math.round(player.getHealth());
        long maxHp = Math.round(player.getMaxHealth());
        long armor = Math.round(player.getAttributeValue(Attributes.ARMOR) * 10.0);

        Component bar = Component.literal("❤ ").withStyle(ChatFormatting.RED)
            .append(Component.literal(hp + " / " + maxHp + "   ").withStyle(ChatFormatting.GREEN))
            .append(Component.literal("❋ ").withStyle(ChatFormatting.WHITE))
            .append(Component.literal(String.valueOf(armor)).withStyle(ChatFormatting.GREEN));

        player.sendOverlayMessage(bar);
    }

    private static String fmt1(double v) {
        return v == Math.floor(v) ? String.valueOf((int) v) : String.format("%.1f", v);
    }

    // -------------------------------------------------------------------------

    public static void addXp(ServerPlayer player, Skill skill, long amount) {
        PlayerData data   = PlayerDataManager.get(player.getUUID());

        // Apply active pet XP bonus multiplier
        var activePet = data.getPetRoster().getActivePet();
        if (activePet.isPresent()) {
            PetInstance pet = activePet.get();
            double bonus = PetAbilityRegistry.getAbilities(pet.getType()).stream()
                .filter(a -> a.type == PetAbility.Type.XP_BONUS && a.skill == skill)
                .mapToDouble(a -> a.compute(pet.getLevel(), pet.getRarity()))
                .sum();
            if (bonus > 0) {
                // Guarantee at least +1 XP so low-level pets are always visible.
                // Without this, (long)(5 * 1.008) = 5 — the bonus silently disappears.
                long bonused = (long)(amount * (1.0 + bonus));
                amount = Math.max(bonused, amount + 1);
            }
        }

        int before = data.getLevel(skill);
        boolean leveledUp = data.addXp(skill, amount);
        int        after  = data.getLevel(skill);

        if (leveledUp) {
            onLevelUp(player, skill, before, after);
        }
        // Always refresh the boss bar (shows level-up state too)
        sendXpBar(player, data, skill, amount, leveledUp);

        // Taming XP: having an active pet earns passive Taming XP alongside any skill action
        if (skill != Skill.TAMING && amount > 0 && data.getPetRoster().getActivePet().isPresent()) {
            long tamingGain = Math.max(1, amount / 10);
            int tamingBefore = data.getLevel(Skill.TAMING);
            boolean tamingLeveled = data.addXp(Skill.TAMING, tamingGain);
            if (tamingLeveled) {
                onLevelUp(player, Skill.TAMING, tamingBefore, data.getLevel(Skill.TAMING));
            }
        }

        // Feed active pet — apply Beast Bond / Pet Whisperer taming multiplier
        int tamingLevel = data.getLevel(Skill.TAMING);
        double petMult = SkillAbilityRegistry.getPetXpMultiplier(tamingLevel);
        // Capture level before XP is added so multi-level gains show the full range
        int petLevelBefore = data.getPetRoster().getActivePet()
            .map(com.peakskills.pet.PetInstance::getLevel).orElse(0);
        boolean petUp = data.getPetRoster().feedXp(skill, amount, petMult);
        if (petUp) {
            data.getPetRoster().getActivePet().ifPresent(pet -> {
                int petLevelAfter = pet.getLevel();
                // Reapply stats so new ability values take effect immediately
                StatManager.applyStats(player);

                // Subtle pet level-up sound — soft XP ding, much quieter than skill level-up
                player.connection.send(new ClientboundSoundPacket(
                    Holder.direct(SoundEvents.EXPERIENCE_ORB_PICKUP),
                    SoundSource.PLAYERS,
                    player.getX(), player.getY(), player.getZ(),
                    0.4f, 1.6f, 0L
                ));

                // Chat message — shows full range if multiple levels were gained at once
                player.sendSystemMessage(
                    Component.literal("⬆ ").withStyle(ChatFormatting.GOLD)
                        .append(Component.literal(pet.getRarity().displayName + " " + pet.getType().displayName)
                            .withStyle(pet.getRarity().color, ChatFormatting.BOLD))
                        .append(Component.literal(" leveled up! ").withStyle(ChatFormatting.WHITE))
                        .append(Component.literal("Level " + petLevelBefore + " → " + petLevelAfter)
                            .withStyle(ChatFormatting.GREEN)),
                    false);

                // Show updated ability values in chat
                List<PetAbility> abilities = PetAbilityRegistry.getAbilities(pet.getType());
                if (!abilities.isEmpty()) {
                    for (PetAbility ability : abilities) {
                        player.sendSystemMessage(
                            Component.literal("  ✦ " + ability.displayLine(petLevelAfter, pet.getRarity()))
                                .withStyle(ChatFormatting.GREEN),
                            false);
                    }
                }

                // Action bar flash
                player.sendSystemMessage(
                    Component.literal("⬆ " + pet.getType().displayName + " is now level " + petLevelAfter + "!")
                        .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
                    true);
            });
        }
    }

    // -------------------------------------------------------------------------
    // Boss-bar XP display
    // -------------------------------------------------------------------------

    private static void sendXpBar(ServerPlayer player, PlayerData data,
                                   Skill skill, long gained, boolean leveledUp) {
        int  level    = data.getLevel(skill);
        long currentXp = data.getXp(skill);

        long floor  = XPTable.xpForLevel(level);
        boolean maxed = level >= Skill.MAX_LEVEL;
        long ceil   = maxed ? floor : XPTable.xpForLevel(level + 1);
        long span   = maxed ? 1 : ceil - floor;
        long prog   = maxed ? 1 : currentXp - floor;
        float pct   = maxed ? 1f : (span > 0 ? (float) prog / span : 1f);

        // Title line:  Mining   +6 XP   127 / 332   Lv.2
        ChatFormatting nameColor = skillFormatting(skill);
        Component title = Component.literal(skill.getDisplayName() + "  ")
                         .withStyle(nameColor, ChatFormatting.BOLD)
                .append(Component.literal((leveledUp ? "▲ LEVEL UP!  " : "+" + gained + " XP  "))
                         .withStyle(leveledUp ? ChatFormatting.GOLD : ChatFormatting.GREEN))
                .append(maxed
                    ? Component.literal("MAX LEVEL").withStyle(ChatFormatting.GOLD)
                    : Component.literal(String.format("%,d / %,d", prog, span)).withStyle(ChatFormatting.WHITE))
                .append(Component.literal("  Lv." + level)
                         .withStyle(ChatFormatting.AQUA));

        UUID uuid = player.getUUID();

        ServerBossEvent bar = activeBars.computeIfAbsent(uuid, k -> {
            ServerBossEvent b = new ServerBossEvent(UUID.randomUUID(), title,
                skillBossColor(skill), BossEvent.BossBarOverlay.PROGRESS);
            b.addPlayer(player);
            return b;
        });

        bar.setName(title);
        bar.setColor(skillBossColor(skill));
        bar.setProgress(Math.min(1f, Math.max(0f, pct)));

        // Reset countdown
        barCountdown.put(uuid, BAR_DURATION);
    }

    // -------------------------------------------------------------------------
    // Level-up chat message
    // -------------------------------------------------------------------------

    private static void onLevelUp(ServerPlayer player, Skill skill, int from, int to) {
        StatManager.applyStats(player);

        if (shouldPlayLevelUpSound(player, from, to)) {
            // Level-up sound (plays only for this player)
            player.connection.send(new ClientboundSoundPacket(
                Holder.direct(SoundEvents.PLAYER_LEVELUP),
                SoundSource.PLAYERS,
                player.getX(), player.getY(), player.getZ(),
                1.0f, 1.0f, 0L
            ));
        }

        ChatFormatting skillColor = skillFormatting(skill);
        String sep = "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬";

        // Top separator
        player.sendSystemMessage(Component.literal(sep).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);

        // Header: "SKILL LEVEL UP  Mining  4 → 5"
        player.sendSystemMessage(
            Component.literal(" SKILL LEVEL UP  ").withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD)
                .append(Component.literal(skill.getDisplayName()).withStyle(skillColor, ChatFormatting.BOLD))
                .append(Component.literal("  " + from + " ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("▶").withStyle(ChatFormatting.GOLD))
                .append(Component.literal(" " + to).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)),
            false);

        // REWARDS header
        player.sendSystemMessage(Component.literal(" REWARDS").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD), false);

        // One line per stat this skill grants
        List<SkillStatSource> sources = StatRegistry.SOURCES.stream()
            .filter(s -> s.skill() == skill)
            .toList();

        for (SkillStatSource src : sources) {
            double gained = src.stat().toDisplay(src.valuePerLevel());
            double total  = src.stat().toDisplay(src.compute(to));

            String gainStr  = formatVal(gained);
            String totalStr = formatVal(total);

            player.sendSystemMessage(
                Component.literal("  +").withStyle(ChatFormatting.GREEN)
                    .append(Component.literal(gainStr + " ").withStyle(ChatFormatting.GREEN))
                    .append(Component.literal(src.stat().getIcon() + " " + src.stat().getDisplayName())
                        .withStyle(src.stat().getColor(), ChatFormatting.BOLD))
                    .append(Component.literal("  (Total: " + totalStr + ")")
                        .withStyle(ChatFormatting.DARK_GRAY)),
                false);
        }

        UnlockNotificationManager.announce(player, skill, from, to);

        // Replenish recipe unlock at Farming 30
        if (skill == Skill.FARMING) {
            com.peakskills.enchantment.ReplenishEnchantment.onFarmingLevelUp(player, from, to);
        }

        // Milestone item reward
        grantMilestoneReward(player, skill, to);

        // Bottom separator
        player.sendSystemMessage(Component.literal(sep).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
    }

    private static void grantMilestoneReward(ServerPlayer player, Skill skill, int level) {
        if (level != 25 && level != 50 && level != 75 && level != 99) return;

        net.minecraft.world.item.ItemStack reward = milestoneReward(skill, level);
        if (reward == null || reward.isEmpty()) return;

        // ServerPlayerEntity.getEntityWorld() returns ServerWorld directly
        net.minecraft.server.level.ServerLevel sw = player.level();
        sw.addFreshEntity(new ItemEntity(
            sw,
            player.getX(), player.getY(), player.getZ(),
            reward.copy()
        ));

        player.sendSystemMessage(
            Component.literal("  ✦ Milestone Reward: ").withStyle(ChatFormatting.GOLD)
                .append(reward.getHoverName().copy().withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" x" + reward.getCount()).withStyle(ChatFormatting.WHITE)),
            false);
    }

    private static boolean shouldPlayLevelUpSound(ServerPlayer player, int from, int to) {
        PlayerData data = PlayerDataManager.get(player.getUUID());
        if (!data.shouldLimitBurstLevelUpSounds()) return true;

        int levelsGained = Math.max(1, to - from);
        long now = System.currentTimeMillis();
        Deque<Long> timestamps = levelUpSoundWindow.computeIfAbsent(player.getUUID(),
            uuid -> new ArrayDeque<>());

        while (!timestamps.isEmpty() && now - timestamps.peekFirst() > LEVEL_UP_SOUND_WINDOW_MS) {
            timestamps.removeFirst();
        }

        for (int i = 0; i < levelsGained; i++) {
            timestamps.addLast(now);
        }

        return timestamps.size() <= LEVEL_UP_SOUND_LIMIT;
    }

    private static net.minecraft.world.item.ItemStack milestoneReward(Skill skill, int level) {
        // Each entry: [item, count] — counts are tuned per item type so nothing absurd drops.
        // Armor/tools = always 1. Materials scale with rarity.
        record R(Item item, int count) {}
        // Milestone philosophy: give things OUTSIDE the skill's own resource pool.
        // Lv.25 = helpful cross-skill boost. Lv.50 = significant. Lv.75 = rare/powerful.
        // Lv.99 = legendary — something that reflects mastery and cannot be casually obtained.
        R r = switch (skill) {
            case MINING -> switch (level) {
                case 25 -> new R(Items.EXPERIENCE_BOTTLE,     32);
                case 50 -> new R(Items.DIAMOND,               16);
                case 75 -> new R(Items.NETHERITE_SCRAP,        6);
                default -> new R(Items.NETHERITE_INGOT,        4); // 4 ingots = significant
            };
            case WOODCUTTING -> switch (level) {
                case 25 -> new R(Items.EXPERIENCE_BOTTLE,     32);
                case 50 -> new R(Items.DIAMOND_AXE,            1);
                case 75 -> new R(Items.NETHERITE_SCRAP,        4);
                default -> new R(Items.NETHERITE_AXE,          1);
            };
            case EXCAVATING -> switch (level) {
                case 25 -> new R(Items.EXPERIENCE_BOTTLE,     32);
                case 50 -> new R(Items.DIAMOND,                8);
                case 75 -> new R(Items.DIAMOND_SHOVEL,         1);
                default -> new R(Items.NETHERITE_SHOVEL,       1);
            };
            case FARMING -> switch (level) {
                case 25 -> new R(Items.GOLDEN_CARROT,         16);
                case 50 -> new R(Items.GOLDEN_APPLE,           4);
                case 75 -> new R(Items.ENCHANTED_GOLDEN_APPLE, 1);
                default -> new R(Items.ENCHANTED_GOLDEN_APPLE, 2); // pinnacle of food mastery
            };
            case FISHING -> switch (level) {
                case 25 -> new R(Items.NAUTILUS_SHELL,         4);
                case 50 -> new R(Items.HEART_OF_THE_SEA,       1);
                case 75 -> new R(Items.TRIDENT,                1);
                default -> new R(Items.NETHER_STAR,            1);
            };
            case SLAYING -> switch (level) {
                case 25 -> new R(Items.DIAMOND_SWORD,          1);
                case 50 -> new R(Items.TOTEM_OF_UNDYING,       1);
                case 75 -> new R(Items.WITHER_SKELETON_SKULL,  2);
                default -> new R(Items.NETHER_STAR,            1);
            };
            case RANGED -> switch (level) {
                case 25 -> new R(Items.SPECTRAL_ARROW,        64);
                case 50 -> new R(Items.CROSSBOW,               1);
                case 75 -> new R(Items.TOTEM_OF_UNDYING,       1);
                default -> new R(Items.NETHER_STAR,            1);
            };
            case DEFENSE -> switch (level) {
                case 25 -> new R(Items.IRON_CHESTPLATE,        1);
                case 50 -> new R(Items.DIAMOND_CHESTPLATE,     1);
                case 75 -> new R(Items.NETHERITE_SCRAP,        4);
                default -> new R(Items.NETHERITE_CHESTPLATE,   1);
            };
            case ENCHANTING -> switch (level) {
                case 25 -> new R(Items.LAPIS_LAZULI,          64);
                case 50 -> new R(Items.BOOKSHELF,             15); // exactly fills max enchanting table
                case 75 -> new R(Items.EXPERIENCE_BOTTLE,     64);
                default -> new R(Items.NETHER_STAR,            1);
            };
            case ALCHEMY -> switch (level) {
                case 25 -> new R(Items.BREWING_STAND,          1);
                case 50 -> new R(Items.GHAST_TEAR,             8);
                case 75 -> new R(Items.DRAGON_BREATH,          4);
                default -> new R(Items.NETHER_STAR,            1);
            };
            case SMITHING -> switch (level) {
                case 25 -> new R(Items.DIAMOND,                8);
                case 50 -> new R(Items.NETHERITE_SCRAP,        4);
                case 75 -> new R(Items.NETHERITE_INGOT,        2);
                default -> new R(Items.NETHERITE_INGOT,        6);
            };
            case COOKING -> switch (level) {
                case 25 -> new R(Items.GOLDEN_CARROT,         16);
                case 50 -> new R(Items.GOLDEN_APPLE,           4);
                case 75 -> new R(Items.ENCHANTED_GOLDEN_APPLE, 1);
                default -> new R(Items.ENCHANTED_GOLDEN_APPLE, 2);
            };
            case CRAFTING -> switch (level) {
                case 25 -> new R(Items.EXPERIENCE_BOTTLE,     32);
                case 50 -> new R(Items.DIAMOND,                8);
                case 75 -> new R(Items.CHEST,                 32);
                default -> new R(Items.SHULKER_BOX,            4);
            };
            case AGILITY -> switch (level) {
                case 25 -> new R(Items.EXPERIENCE_BOTTLE,     32);
                case 50 -> new R(Items.DIAMOND_BOOTS,          1);
                case 75 -> new R(Items.NETHERITE_SCRAP,        4);
                default -> new R(Items.ELYTRA,                 1);
            };
            case TAMING -> switch (level) {
                case 25 -> new R(Items.GOLDEN_APPLE,           2);
                case 50 -> new R(Items.SADDLE,                 1);
                case 75 -> new R(Items.GOLDEN_APPLE,           8);
                default -> new R(Items.TOTEM_OF_UNDYING,       2); // your bond with your pet protects you
            };
            case TRADING -> switch (level) {
                case 25 -> new R(Items.EMERALD,               64);
                case 50 -> new R(Items.EMERALD_BLOCK,         16);
                case 75 -> new R(Items.DIAMOND,               16);
                default -> new R(Items.NETHER_STAR,            1);
            };
        };

        return new net.minecraft.world.item.ItemStack(r.item(), r.count());
    }

    private static String formatVal(double v) {
        if (v < 0.01) return String.format("%.4f", v).replaceAll("0+$", "").replaceAll("\\.$", "");
        if (v < 1)    return String.format("%.3f", v).replaceAll("0+$", "").replaceAll("\\.$", "");
        return String.format("%.2f", v).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    // -------------------------------------------------------------------------
    // Skill → color mappings
    // -------------------------------------------------------------------------

    private static BossEvent.BossBarColor skillBossColor(Skill skill) {
        return switch (skill) {
            case MINING      -> BossEvent.BossBarColor.WHITE;
            case WOODCUTTING -> BossEvent.BossBarColor.GREEN;
            case EXCAVATING  -> BossEvent.BossBarColor.YELLOW;
            case FARMING     -> BossEvent.BossBarColor.GREEN;
            case FISHING     -> BossEvent.BossBarColor.BLUE;
            case DEFENSE     -> BossEvent.BossBarColor.WHITE;
            case SLAYING     -> BossEvent.BossBarColor.RED;
            case RANGED      -> BossEvent.BossBarColor.YELLOW;
            case ENCHANTING  -> BossEvent.BossBarColor.PURPLE;
            case ALCHEMY     -> BossEvent.BossBarColor.PURPLE;
            case SMITHING    -> BossEvent.BossBarColor.WHITE;
            case COOKING     -> BossEvent.BossBarColor.PINK;
            case CRAFTING    -> BossEvent.BossBarColor.WHITE;
            case AGILITY     -> BossEvent.BossBarColor.BLUE;
            case TAMING      -> BossEvent.BossBarColor.GREEN;
            case TRADING     -> BossEvent.BossBarColor.GREEN;
        };
    }

    private static ChatFormatting skillFormatting(Skill skill) {
        return switch (skill) {
            case MINING      -> ChatFormatting.GRAY;
            case WOODCUTTING -> ChatFormatting.GREEN;
            case EXCAVATING  -> ChatFormatting.YELLOW;
            case FARMING     -> ChatFormatting.DARK_GREEN;
            case FISHING     -> ChatFormatting.AQUA;
            case DEFENSE     -> ChatFormatting.WHITE;
            case SLAYING     -> ChatFormatting.RED;
            case RANGED      -> ChatFormatting.GOLD;
            case ENCHANTING  -> ChatFormatting.LIGHT_PURPLE;
            case ALCHEMY     -> ChatFormatting.DARK_PURPLE;
            case SMITHING    -> ChatFormatting.DARK_GRAY;
            case COOKING     -> ChatFormatting.YELLOW;
            case CRAFTING    -> ChatFormatting.WHITE;
            case AGILITY     -> ChatFormatting.BLUE;
            case TAMING      -> ChatFormatting.DARK_GREEN;
            case TRADING     -> ChatFormatting.GREEN;
        };
    }
}
