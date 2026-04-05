package com.peakskills.fishing;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom fishing loot table.
 *
 * Every reel that catches something rolls this table ONCE.
 * Higher fishing level unlocks rarer entries; Luck stat boosts
 * the effective level by up to ±5 levels.
 *
 * Rarity tiers (match SkyBlock naming):
 *   COMMON, UNCOMMON, RARE, EPIC, LEGENDARY
 */
public class FishingLootTable {

    // ── Loot pool ─────────────────────────────────────────────────────────────

    private static final List<Entry> POOL = new ArrayList<>();

    static {
        // ── COMMON (level 1+) ──────────────────────────────────────────────
        add(1,   200, Items.COD,              1, 3, "Raw Fish",           Formatting.WHITE,        Rarity.COMMON);
        add(1,   180, Items.SALMON,           1, 2, "Raw Salmon",         Formatting.WHITE,        Rarity.COMMON);
        add(1,   150, Items.LILY_PAD,         1, 3, "Lily Pad",           Formatting.WHITE,        Rarity.COMMON);
        add(1,   120, Items.INK_SAC,          1, 4, "Ink Sac",            Formatting.WHITE,        Rarity.COMMON);
        add(1,   100, Items.SEAGRASS,         1, 3, "Seagrass",           Formatting.WHITE,        Rarity.COMMON);

        // ── UNCOMMON (level 10+) ───────────────────────────────────────────
        add(10,   80, Items.TROPICAL_FISH,    1, 1, "Tropical Fish",      Formatting.GREEN,        Rarity.UNCOMMON);
        add(10,   70, Items.PUFFERFISH,       1, 1, "Pufferfish",         Formatting.GREEN,        Rarity.UNCOMMON);
        add(10,   60, Items.BONE,             1, 3, "Fish Bones",         Formatting.GREEN,        Rarity.UNCOMMON);
        add(15,   55, Items.STRING,           1, 4, "Tangled Line",       Formatting.GREEN,        Rarity.UNCOMMON);
        add(20,   50, Items.LEATHER,          1, 2, "Waterlogged Hide",   Formatting.GREEN,        Rarity.UNCOMMON);

        // ── RARE (level 25+) ──────────────────────────────────────────────
        add(25,   35, Items.NAUTILUS_SHELL,   1, 1, "Nautilus Shell",     Formatting.AQUA,         Rarity.RARE);
        add(25,   30, Items.PRISMARINE_SHARD, 2, 5, "Prismarine Shard",   Formatting.AQUA,         Rarity.RARE);
        add(30,   25, Items.IRON_INGOT,       1, 3, "Sunken Scrap",       Formatting.AQUA,         Rarity.RARE);
        add(35,   22, Items.PRISMARINE_CRYSTALS, 1, 3, "Sea Crystal",     Formatting.AQUA,         Rarity.RARE);
        add(40,   18, Items.GOLD_INGOT,       1, 2, "Ancient Coin",       Formatting.AQUA,         Rarity.RARE);

        // ── EPIC (level 50+) ──────────────────────────────────────────────
        add(50,   10, Items.DIAMOND,          1, 1, "Sea Diamond",        Formatting.DARK_PURPLE,  Rarity.EPIC);
        add(55,    8, Items.HEART_OF_THE_SEA, 1, 1, "Deep Treasure",      Formatting.DARK_PURPLE,  Rarity.EPIC);
        add(60,    7, Items.SPONGE,           1, 2, "Ocean Sponge",       Formatting.DARK_PURPLE,  Rarity.EPIC);
        add(65,    5, Items.CONDUIT,          1, 1, "Conduit Fragment",   Formatting.DARK_PURPLE,  Rarity.EPIC);

        // ── LEGENDARY (level 75+) ──────────────────────────────────────────
        add(75,    3, Items.TRIDENT,          1, 1, "Ancient Trident",    Formatting.GOLD,         Rarity.LEGENDARY);
        add(80,    2, Items.TOTEM_OF_UNDYING, 1, 1, "Totem of the Deep",  Formatting.GOLD,         Rarity.LEGENDARY);
        add(90,    1, Items.NETHER_STAR,      1, 1, "Abyssal Star",       Formatting.GOLD,         Rarity.LEGENDARY);
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Rolls the loot table once and returns the ItemStack to award.
     * Returns null if nothing should drop this reel (shouldn't happen for count > 0,
     * but defensive).
     *
     * @param fishingLevel   player's current Fishing skill level
     * @param luckBonus      raw Luck attribute value (e.g. 0.5 = 50 display Luck)
     * @param random         server random
     */
    public static RollResult roll(int fishingLevel, double luckBonus, Random random) {
        // Luck inflates effective level by up to +10 at high values
        int effectiveLevel = fishingLevel + (int)(luckBonus * 20);

        // Build eligible pool weighted by rarity
        List<Entry> eligible = new ArrayList<>();
        int totalWeight = 0;
        for (Entry e : POOL) {
            if (effectiveLevel >= e.minLevel) {
                eligible.add(e);
                totalWeight += e.weight;
            }
        }
        if (eligible.isEmpty() || totalWeight == 0) return null;

        int roll = random.nextInt(totalWeight);
        int cursor = 0;
        for (Entry e : eligible) {
            cursor += e.weight;
            if (roll < cursor) {
                int count = e.minCount == e.maxCount
                    ? e.minCount
                    : e.minCount + random.nextInt(e.maxCount - e.minCount + 1);
                return new RollResult(buildStack(e, count), e.rarity.xp);
            }
        }
        return null; // unreachable
    }

    // ── Rarity ───────────────────────────────────────────────────────────────

    public enum Rarity {
        //                    color                    label         xp
        COMMON    (Formatting.WHITE,       "COMMON",      30),
        UNCOMMON  (Formatting.GREEN,       "UNCOMMON",    80),
        RARE      (Formatting.AQUA,        "RARE",       194),
        EPIC      (Formatting.DARK_PURPLE, "EPIC",       477),
        LEGENDARY (Formatting.GOLD,        "LEGENDARY", 1283);

        public final Formatting color;
        public final String     label;
        public final long       xp;

        Rarity(Formatting color, String label, long xp) {
            this.color = color;
            this.label = label;
            this.xp    = xp;
        }
    }

    /** Result of a loot roll — the item stack and the XP it should grant. */
    public record RollResult(ItemStack stack, long xp) {}

    // ── Internals ─────────────────────────────────────────────────────────────

    private record Entry(int minLevel, int weight, Item item, int minCount, int maxCount,
                         String displayName, Formatting nameColor, Rarity rarity) {}

    private static void add(int minLevel, int weight, Item item,
                            int minCount, int maxCount,
                            String displayName, Formatting nameColor, Rarity rarity) {
        POOL.add(new Entry(minLevel, weight, item, minCount, maxCount,
            displayName, nameColor, rarity));
    }

    private static ItemStack buildStack(Entry e, int count) {
        ItemStack stack = new ItemStack(e.item, count);

        // Custom name coloured by rarity
        stack.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME,
            Text.literal(e.displayName).formatted(e.nameColor));

        // Lore: rarity label
        stack.set(net.minecraft.component.DataComponentTypes.LORE,
            new net.minecraft.component.type.LoreComponent(List.of(
                Text.literal(e.rarity.label).formatted(e.rarity.color, Formatting.BOLD)
            )));

        return stack;
    }
}
