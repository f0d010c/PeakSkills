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
import com.peakskills.stat.SkillStatSource;
import com.peakskills.stat.StatManager;
import com.peakskills.stat.StatRegistry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class XpManager {

    /** Ticks the boss bar stays visible after the last XP gain (~3 s). */
    private static final int BAR_DURATION = 60;

    private static final Map<UUID, ServerBossBar> activeBars   = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer>       barCountdown = new ConcurrentHashMap<>();

    /** Call once from PeakSkills.onInitialize() to register the tick cleaner. */
    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            // Decrement all countdowns (replaceAll is safe on ConcurrentHashMap)
            barCountdown.replaceAll((uuid, ticks) -> ticks - 1);
            // Remove any that have expired
            barCountdown.entrySet().removeIf(entry -> {
                if (entry.getValue() <= 0) {
                    ServerBossBar bar = activeBars.remove(entry.getKey());
                    if (bar != null) bar.clearPlayers();
                    return true;
                }
                return false;
            });

            // Refresh stat action bar every 2 seconds for all online players
            if (server.getTicks() % 40 == 0) {
                for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                    sendStatBar(p);
                }
            }
        });
    }

    private static void sendStatBar(ServerPlayerEntity player) {
        long hp    = Math.round(player.getHealth());
        long maxHp = Math.round(player.getMaxHealth());
        long armor = Math.round(player.getAttributeValue(EntityAttributes.ARMOR) * 10.0);

        Text bar = Text.literal("❤ ").formatted(Formatting.RED)
            .append(Text.literal(hp + " / " + maxHp + "   ").formatted(Formatting.GREEN))
            .append(Text.literal("❋ ").formatted(Formatting.WHITE))
            .append(Text.literal(String.valueOf(armor)).formatted(Formatting.GREEN));

        player.sendMessage(bar, true);
    }

    private static String fmt1(double v) {
        return v == Math.floor(v) ? String.valueOf((int) v) : String.format("%.1f", v);
    }

    // -------------------------------------------------------------------------

    public static void addXp(ServerPlayerEntity player, Skill skill, long amount) {
        PlayerData data   = PlayerDataManager.get(player.getUuid());

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
                player.networkHandler.sendPacket(new PlaySoundS2CPacket(
                    RegistryEntry.of(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP),
                    SoundCategory.PLAYERS,
                    player.getX(), player.getY(), player.getZ(),
                    0.4f, 1.6f, 0L
                ));

                // Chat message — shows full range if multiple levels were gained at once
                player.sendMessage(
                    Text.literal("⬆ ").formatted(Formatting.GOLD)
                        .append(Text.literal(pet.getRarity().displayName + " " + pet.getType().displayName)
                            .formatted(pet.getRarity().color, Formatting.BOLD))
                        .append(Text.literal(" leveled up! ").formatted(Formatting.WHITE))
                        .append(Text.literal("Level " + petLevelBefore + " → " + petLevelAfter)
                            .formatted(Formatting.GREEN)),
                    false);

                // Show updated ability values in chat
                List<PetAbility> abilities = PetAbilityRegistry.getAbilities(pet.getType());
                if (!abilities.isEmpty()) {
                    for (PetAbility ability : abilities) {
                        player.sendMessage(
                            Text.literal("  ✦ " + ability.displayLine(petLevelAfter, pet.getRarity()))
                                .formatted(Formatting.GREEN),
                            false);
                    }
                }

                // Action bar flash
                player.sendMessage(
                    Text.literal("⬆ " + pet.getType().displayName + " is now level " + petLevelAfter + "!")
                        .formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD),
                    true);
            });
        }
    }

    // -------------------------------------------------------------------------
    // Boss-bar XP display
    // -------------------------------------------------------------------------

    private static void sendXpBar(ServerPlayerEntity player, PlayerData data,
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
        Formatting nameColor = skillFormatting(skill);
        Text title = Text.literal(skill.getDisplayName() + "  ")
                         .formatted(nameColor, Formatting.BOLD)
                .append(Text.literal((leveledUp ? "▲ LEVEL UP!  " : "+" + gained + " XP  "))
                         .formatted(leveledUp ? Formatting.GOLD : Formatting.GREEN))
                .append(maxed
                    ? Text.literal("MAX LEVEL").formatted(Formatting.GOLD)
                    : Text.literal(String.format("%,d / %,d", prog, span)).formatted(Formatting.WHITE))
                .append(Text.literal("  Lv." + level)
                         .formatted(Formatting.AQUA));

        UUID uuid = player.getUuid();

        ServerBossBar bar = activeBars.computeIfAbsent(uuid, k -> {
            ServerBossBar b = new ServerBossBar(title, skillBossColor(skill), BossBar.Style.PROGRESS);
            b.addPlayer(player);
            return b;
        });

        bar.setName(title);
        bar.setColor(skillBossColor(skill));
        bar.setPercent(Math.min(1f, Math.max(0f, pct)));

        // Reset countdown
        barCountdown.put(uuid, BAR_DURATION);
    }

    // -------------------------------------------------------------------------
    // Level-up chat message
    // -------------------------------------------------------------------------

    private static void onLevelUp(ServerPlayerEntity player, Skill skill, int from, int to) {
        StatManager.applyStats(player);

        // Level-up sound (plays only for this player)
        player.networkHandler.sendPacket(new PlaySoundS2CPacket(
            RegistryEntry.of(SoundEvents.ENTITY_PLAYER_LEVELUP),
            SoundCategory.PLAYERS,
            player.getX(), player.getY(), player.getZ(),
            1.0f, 1.0f, 0L
        ));

        Formatting skillColor = skillFormatting(skill);
        String sep = "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬";

        // Top separator
        player.sendMessage(Text.literal(sep).formatted(Formatting.GOLD, Formatting.BOLD), false);

        // Header: "SKILL LEVEL UP  Mining  4 → 5"
        player.sendMessage(
            Text.literal(" SKILL LEVEL UP  ").formatted(Formatting.WHITE, Formatting.BOLD)
                .append(Text.literal(skill.getDisplayName()).formatted(skillColor, Formatting.BOLD))
                .append(Text.literal("  " + from + " ").formatted(Formatting.GRAY))
                .append(Text.literal("▶").formatted(Formatting.GOLD))
                .append(Text.literal(" " + to).formatted(Formatting.GREEN, Formatting.BOLD)),
            false);

        // REWARDS header
        player.sendMessage(Text.literal(" REWARDS").formatted(Formatting.GREEN, Formatting.BOLD), false);

        // One line per stat this skill grants
        List<SkillStatSource> sources = StatRegistry.SOURCES.stream()
            .filter(s -> s.skill() == skill)
            .toList();

        for (SkillStatSource src : sources) {
            double gained = src.stat().toDisplay(src.valuePerLevel());
            double total  = src.stat().toDisplay(src.compute(to));

            String gainStr  = formatVal(gained);
            String totalStr = formatVal(total);

            player.sendMessage(
                Text.literal("  +").formatted(Formatting.GREEN)
                    .append(Text.literal(gainStr + " ").formatted(Formatting.GREEN))
                    .append(Text.literal(src.stat().getIcon() + " " + src.stat().getDisplayName())
                        .formatted(src.stat().getColor(), Formatting.BOLD))
                    .append(Text.literal("  (Total: " + totalStr + ")")
                        .formatted(Formatting.DARK_GRAY)),
                false);
        }

        // Replenish recipe unlock at Farming 30
        if (skill == Skill.FARMING) {
            com.peakskills.enchantment.ReplenishEnchantment.onFarmingLevelUp(player, from, to);
        }

        // Milestone item reward
        grantMilestoneReward(player, skill, to);

        // Bottom separator
        player.sendMessage(Text.literal(sep).formatted(Formatting.GOLD, Formatting.BOLD), false);
    }

    private static void grantMilestoneReward(ServerPlayerEntity player, Skill skill, int level) {
        if (level != 25 && level != 50 && level != 75 && level != 99) return;

        net.minecraft.item.ItemStack reward = milestoneReward(skill, level);
        if (reward == null || reward.isEmpty()) return;

        // ServerPlayerEntity.getEntityWorld() returns ServerWorld directly
        net.minecraft.server.world.ServerWorld sw = player.getEntityWorld();
        sw.spawnEntity(new ItemEntity(
            sw,
            player.getX(), player.getY(), player.getZ(),
            reward.copy()
        ));

        player.sendMessage(
            Text.literal("  ✦ Milestone Reward: ").formatted(Formatting.GOLD)
                .append(reward.getName().copy().formatted(Formatting.YELLOW))
                .append(Text.literal(" x" + reward.getCount()).formatted(Formatting.WHITE)),
            false);
    }

    private static net.minecraft.item.ItemStack milestoneReward(Skill skill, int level) {
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

        return new net.minecraft.item.ItemStack(r.item(), r.count());
    }

    private static String formatVal(double v) {
        if (v < 0.01) return String.format("%.4f", v).replaceAll("0+$", "").replaceAll("\\.$", "");
        if (v < 1)    return String.format("%.3f", v).replaceAll("0+$", "").replaceAll("\\.$", "");
        return String.format("%.2f", v).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    // -------------------------------------------------------------------------
    // Skill → color mappings
    // -------------------------------------------------------------------------

    private static BossBar.Color skillBossColor(Skill skill) {
        return switch (skill) {
            case MINING      -> BossBar.Color.WHITE;
            case WOODCUTTING -> BossBar.Color.GREEN;
            case EXCAVATING  -> BossBar.Color.YELLOW;
            case FARMING     -> BossBar.Color.GREEN;
            case FISHING     -> BossBar.Color.BLUE;
            case DEFENSE     -> BossBar.Color.WHITE;
            case SLAYING     -> BossBar.Color.RED;
            case RANGED      -> BossBar.Color.YELLOW;
            case ENCHANTING  -> BossBar.Color.PURPLE;
            case ALCHEMY     -> BossBar.Color.PURPLE;
            case SMITHING    -> BossBar.Color.WHITE;
            case COOKING     -> BossBar.Color.PINK;
            case CRAFTING    -> BossBar.Color.WHITE;
            case AGILITY     -> BossBar.Color.BLUE;
            case TAMING      -> BossBar.Color.GREEN;
            case TRADING     -> BossBar.Color.GREEN;
        };
    }

    private static Formatting skillFormatting(Skill skill) {
        return switch (skill) {
            case MINING      -> Formatting.GRAY;
            case WOODCUTTING -> Formatting.GREEN;
            case EXCAVATING  -> Formatting.YELLOW;
            case FARMING     -> Formatting.DARK_GREEN;
            case FISHING     -> Formatting.AQUA;
            case DEFENSE     -> Formatting.WHITE;
            case SLAYING     -> Formatting.RED;
            case RANGED      -> Formatting.GOLD;
            case ENCHANTING  -> Formatting.LIGHT_PURPLE;
            case ALCHEMY     -> Formatting.DARK_PURPLE;
            case SMITHING    -> Formatting.DARK_GRAY;
            case COOKING     -> Formatting.YELLOW;
            case CRAFTING    -> Formatting.WHITE;
            case AGILITY     -> Formatting.BLUE;
            case TAMING      -> Formatting.DARK_GREEN;
            case TRADING     -> Formatting.GREEN;
        };
    }
}
