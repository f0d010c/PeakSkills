package com.peakskills.collection;

import com.peakskills.stat.Stat;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.Identifier;

import java.util.*;

/**
 * Defines every collection's tiers, thresholds, and rewards.
 *
 * Reward philosophy:
 *   - Stat bonuses are the primary reward and appear on most tiers.
 *   - Item rewards appear at tier 4 (mid-game boost), tier 7 (valuable), and tier 9 (capstone).
 *   - Capstone items are calibrated to collection difficulty:
 *       Easy (50k cobblestone): Golden Apple x2
 *       Medium (10k diamond, 7.5k obsidian): Netherite Scrap
 *       Hard (750 ancient debris, 5k wither skeleton): Netherite Ingot / Nether Star
 *   - Never give back large quantities of the same resource the player just farmed.
 */
public class CollectionRegistry {

    private static final Map<CollectionType, List<CollectionTier>> TIERS =
        new EnumMap<>(CollectionType.class);

    static {
        // ── Mining ────────────────────────────────────────────────────────────
        // Cobblestone: easy, high volume. Stats are the reward.
        reg(CollectionType.COBBLESTONE,
            new long[]{ 50, 150, 500, 1_000, 2_500, 5_000, 10_000, 25_000, 50_000 },
            stat(Stat.DEFENSE, 1),          stat(Stat.TOUGHNESS, 1),        stat(Stat.DEFENSE, 2),
            item(Items.EXPERIENCE_BOTTLE, 16), stat(Stat.TOUGHNESS, 2),     stat(Stat.DEFENSE, 3),
            item(Items.DIAMOND, 4),         stat(Stat.DEFENSE, 4),          item(Items.GOLDEN_APPLE, 2));

        // Coal: easy. XP bottles (enchanting fuel) are fitting.
        reg(CollectionType.COAL,
            new long[]{ 50, 150, 300, 600, 1_000, 2_500, 5_000, 10_000, 25_000 },
            stat(Stat.LUCK, 1),             stat(Stat.STRENGTH, 1),         stat(Stat.LUCK, 2),
            item(Items.EXPERIENCE_BOTTLE, 16), stat(Stat.STRENGTH, 2),      stat(Stat.LUCK, 2),
            item(Items.DIAMOND, 4),         stat(Stat.LUCK, 3),             item(Items.GOLDEN_APPLE, 2));

        // Iron: moderate. Netherite scrap at capstone — you've smelted a fortune of iron.
        reg(CollectionType.IRON,
            new long[]{ 50, 150, 300, 600, 1_000, 2_500, 5_000, 10_000, 25_000 },
            stat(Stat.DEFENSE, 1),          stat(Stat.STRENGTH, 1),         stat(Stat.DEFENSE, 2),
            item(Items.EXPERIENCE_BOTTLE, 16), stat(Stat.STRENGTH, 2),      stat(Stat.DEFENSE, 2),
            item(Items.DIAMOND, 8),         stat(Stat.STRENGTH, 3),         item(Items.NETHERITE_SCRAP, 2));

        // Gold: moderate. Gold apples fit the gold theme.
        reg(CollectionType.GOLD,
            new long[]{ 50, 150, 300, 600, 1_000, 2_500, 5_000, 10_000, 25_000 },
            stat(Stat.LUCK, 1),             stat(Stat.LUCK, 1),             stat(Stat.LUCK, 2),
            item(Items.EXPERIENCE_BOTTLE, 16), stat(Stat.LUCK, 2),          stat(Stat.LUCK, 2),
            item(Items.GOLDEN_APPLE, 2),    stat(Stat.LUCK, 3),             item(Items.GOLDEN_APPLE, 4));

        // Diamond: hard grind. 10k diamonds = exceptional effort. Netherite ingot capstone.
        reg(CollectionType.DIAMOND,
            new long[]{ 10, 50, 100, 250, 500, 1_000, 2_500, 5_000, 10_000 },
            stat(Stat.TOUGHNESS, 1),        stat(Stat.TOUGHNESS, 1),        stat(Stat.TOUGHNESS, 2),
            item(Items.DIAMOND, 4),         stat(Stat.STRENGTH, 2),         stat(Stat.TOUGHNESS, 3),
            item(Items.NETHERITE_SCRAP, 2), stat(Stat.STRENGTH, 3),         item(Items.NETHERITE_INGOT, 2));

        // Copper: easy. XP bottles mid, diamond capstone.
        reg(CollectionType.COPPER,
            new long[]{ 50, 150, 300, 600, 1_000, 2_500, 5_000, 10_000, 25_000 },
            stat(Stat.DEFENSE, 1),          stat(Stat.TOUGHNESS, 1),        stat(Stat.DEFENSE, 1),
            item(Items.EXPERIENCE_BOTTLE, 16), stat(Stat.TOUGHNESS, 2),     stat(Stat.DEFENSE, 2),
            item(Items.DIAMOND, 4),         stat(Stat.TOUGHNESS, 2),        item(Items.GOLDEN_APPLE, 2));

        // Lapis: moderate. XP bottles are thematic (enchanting material).
        reg(CollectionType.LAPIS,
            new long[]{ 50, 150, 300, 600, 1_000, 2_500, 5_000, 10_000, 25_000 },
            stat(Stat.LUCK, 1),             stat(Stat.LUCK, 1),             stat(Stat.LUCK, 2),
            item(Items.EXPERIENCE_BOTTLE, 32), stat(Stat.LUCK, 2),          stat(Stat.LUCK, 2),
            item(Items.EXPERIENCE_BOTTLE, 64), stat(Stat.LUCK, 3),          item(Items.DIAMOND, 16));

        // Redstone: moderate. Swiftness theme. XP bottles at mid, diamond capstone.
        reg(CollectionType.REDSTONE,
            new long[]{ 50, 150, 300, 600, 1_000, 2_500, 5_000, 10_000, 25_000 },
            stat(Stat.SWIFTNESS, 1),        stat(Stat.SWIFTNESS, 1),        stat(Stat.SWIFTNESS, 2),
            item(Items.EXPERIENCE_BOTTLE, 16), stat(Stat.SWIFTNESS, 2),     stat(Stat.SWIFTNESS, 2),
            item(Items.DIAMOND, 4),         stat(Stat.SWIFTNESS, 4),        item(Items.GOLDEN_APPLE, 2));

        // Emerald: hard (7.5k emeralds = enormous trading/mining grind). Netherite ingot capstone.
        reg(CollectionType.EMERALD,
            new long[]{ 10, 30, 75, 150, 300, 600, 1_500, 3_000, 7_500 },
            stat(Stat.LUCK, 2),             stat(Stat.LUCK, 2),             stat(Stat.LUCK, 3),
            item(Items.EMERALD, 8),         stat(Stat.LUCK, 3),             stat(Stat.LUCK, 3),
            item(Items.GOLDEN_APPLE, 4),    stat(Stat.LUCK, 4),             item(Items.NETHERITE_INGOT, 2));

        // Nether Quartz: moderate nether grind.
        reg(CollectionType.NETHER_QUARTZ,
            new long[]{ 25, 75, 200, 400, 750, 1_500, 3_000, 7_500, 15_000 },
            stat(Stat.LUCK, 1),             stat(Stat.TOUGHNESS, 1),        stat(Stat.LUCK, 2),
            item(Items.EXPERIENCE_BOTTLE, 16), stat(Stat.TOUGHNESS, 2),     stat(Stat.LUCK, 2),
            item(Items.DIAMOND, 4),         stat(Stat.TOUGHNESS, 3),        item(Items.GOLDEN_APPLE, 4));

        // Obsidian: hard to break (7.5k = significant time). Enchanting table + netherite scrap.
        reg(CollectionType.OBSIDIAN,
            new long[]{ 10, 30, 75, 150, 300, 750, 1_500, 3_000, 7_500 },
            stat(Stat.DEFENSE, 1),          stat(Stat.TOUGHNESS, 1),        stat(Stat.DEFENSE, 2),
            item(Items.ENCHANTING_TABLE, 1), stat(Stat.TOUGHNESS, 2),       stat(Stat.DEFENSE, 3),
            item(Items.DIAMOND, 8),         stat(Stat.TOUGHNESS, 3),        item(Items.NETHERITE_SCRAP, 4));

        // Ancient Debris: extremely hard (750 = massive nether grind). Nether Star capstone.
        reg(CollectionType.ANCIENT_DEBRIS,
            new long[]{ 1, 3, 7, 15, 30, 75, 150, 300, 750 },
            stat(Stat.STRENGTH, 2),         stat(Stat.DEFENSE, 2),          stat(Stat.STRENGTH, 3),
            item(Items.NETHERITE_SCRAP, 1), stat(Stat.DEFENSE, 3),          stat(Stat.STRENGTH, 3),
            item(Items.NETHERITE_INGOT, 1), stat(Stat.DEFENSE, 4),          item(Items.NETHER_STAR, 1));

        // ── Woodcutting ───────────────────────────────────────────────────────
        // Common wood types: stats + XP bottles mid, diamond late, golden apple cap.
        reg(CollectionType.OAK_WOOD,
            new long[]{ 50, 150, 300, 600, 1_000, 2_500, 5_000, 10_000, 25_000 },
            stat(Stat.STRENGTH, 1),         stat(Stat.STRENGTH, 1),         stat(Stat.STRENGTH, 2),
            item(Items.EXPERIENCE_BOTTLE, 16), stat(Stat.STRENGTH, 2),      stat(Stat.STRENGTH, 2),
            item(Items.DIAMOND, 4),         stat(Stat.STRENGTH, 3),         item(Items.GOLDEN_APPLE, 2));

        reg(CollectionType.BIRCH_WOOD,
            new long[]{ 50, 150, 300, 600, 1_000, 2_500, 5_000, 10_000, 25_000 },
            stat(Stat.STRENGTH, 1),         stat(Stat.LUCK, 1),             stat(Stat.STRENGTH, 2),
            item(Items.EXPERIENCE_BOTTLE, 16), stat(Stat.LUCK, 2),          stat(Stat.STRENGTH, 2),
            item(Items.DIAMOND, 4),         stat(Stat.LUCK, 3),             item(Items.GOLDEN_APPLE, 2));

        reg(CollectionType.SPRUCE_WOOD,
            new long[]{ 50, 150, 300, 600, 1_000, 2_500, 5_000, 10_000, 25_000 },
            stat(Stat.STRENGTH, 1),         stat(Stat.TOUGHNESS, 1),        stat(Stat.STRENGTH, 2),
            item(Items.EXPERIENCE_BOTTLE, 16), stat(Stat.TOUGHNESS, 2),     stat(Stat.STRENGTH, 2),
            item(Items.DIAMOND, 4),         stat(Stat.TOUGHNESS, 3),        item(Items.GOLDEN_APPLE, 2));

        // Jungle: rarer biome. Slightly better capstone.
        reg(CollectionType.JUNGLE_WOOD,
            new long[]{ 25, 75, 200, 400, 750, 1_500, 3_000, 7_500, 15_000 },
            stat(Stat.STRENGTH, 1),         stat(Stat.HEALTH, 1),           stat(Stat.STRENGTH, 2),
            item(Items.EXPERIENCE_BOTTLE, 16), stat(Stat.HEALTH, 2),        stat(Stat.STRENGTH, 2),
            item(Items.DIAMOND, 6),         stat(Stat.HEALTH, 3),           item(Items.GOLDEN_APPLE, 4));

        reg(CollectionType.ACACIA_WOOD,
            new long[]{ 25, 75, 200, 400, 750, 1_500, 3_000, 7_500, 15_000 },
            stat(Stat.STRENGTH, 1),         stat(Stat.STRENGTH, 1),         stat(Stat.STRENGTH, 2),
            item(Items.EXPERIENCE_BOTTLE, 16), stat(Stat.STRENGTH, 2),      stat(Stat.STRENGTH, 2),
            item(Items.DIAMOND, 6),         stat(Stat.STRENGTH, 4),         item(Items.GOLDEN_APPLE, 4));

        reg(CollectionType.DARK_OAK_WOOD,
            new long[]{ 25, 75, 200, 400, 750, 1_500, 3_000, 7_500, 15_000 },
            stat(Stat.STRENGTH, 1),         stat(Stat.DEFENSE, 1),          stat(Stat.STRENGTH, 2),
            item(Items.EXPERIENCE_BOTTLE, 16), stat(Stat.DEFENSE, 2),       stat(Stat.STRENGTH, 2),
            item(Items.DIAMOND, 6),         stat(Stat.DEFENSE, 3),          item(Items.GOLDEN_APPLE, 4));

        reg(CollectionType.MANGROVE_WOOD,
            new long[]{ 25, 75, 200, 400, 750, 1_500, 3_000, 7_500, 15_000 },
            stat(Stat.HEALTH, 1),           stat(Stat.HEALTH, 1),           stat(Stat.HEALTH, 2),
            item(Items.EXPERIENCE_BOTTLE, 16), stat(Stat.HEALTH, 2),        stat(Stat.HEALTH, 2),
            item(Items.DIAMOND, 6),         stat(Stat.HEALTH, 3),           item(Items.GOLDEN_APPLE, 4));

        // Cherry: rarer biome, lower thresholds.
        reg(CollectionType.CHERRY_WOOD,
            new long[]{ 10, 30, 75, 150, 300, 750, 1_500, 3_000, 7_500 },
            stat(Stat.LUCK, 1),             stat(Stat.HEALTH, 1),           stat(Stat.LUCK, 2),
            item(Items.EXPERIENCE_BOTTLE, 16), stat(Stat.HEALTH, 2),        stat(Stat.LUCK, 2),
            item(Items.DIAMOND, 6),         stat(Stat.HEALTH, 3),           item(Items.NETHERITE_SCRAP, 2));

        reg(CollectionType.BAMBOO_WOOD,
            new long[]{ 50, 150, 300, 600, 1_000, 2_500, 5_000, 10_000, 25_000 },
            stat(Stat.SWIFTNESS, 1),        stat(Stat.SWIFTNESS, 1),        stat(Stat.SWIFTNESS, 2),
            item(Items.EXPERIENCE_BOTTLE, 16), stat(Stat.SWIFTNESS, 2),     stat(Stat.LUCK, 2),
            item(Items.DIAMOND, 4),         stat(Stat.SWIFTNESS, 3),        item(Items.GOLDEN_APPLE, 2));

        // Nether wood: harder to reach, better items.
        reg(CollectionType.CRIMSON_WOOD,
            new long[]{ 25, 75, 200, 400, 750, 1_500, 3_000, 7_500, 15_000 },
            stat(Stat.STRENGTH, 1),         stat(Stat.TOUGHNESS, 1),        stat(Stat.STRENGTH, 2),
            item(Items.EXPERIENCE_BOTTLE, 16), stat(Stat.TOUGHNESS, 2),     stat(Stat.STRENGTH, 2),
            item(Items.DIAMOND, 8),         stat(Stat.TOUGHNESS, 3),        item(Items.NETHERITE_SCRAP, 2));

        reg(CollectionType.WARPED_WOOD,
            new long[]{ 25, 75, 200, 400, 750, 1_500, 3_000, 7_500, 15_000 },
            stat(Stat.SWIFTNESS, 1),        stat(Stat.LUCK, 1),             stat(Stat.SWIFTNESS, 2),
            item(Items.EXPERIENCE_BOTTLE, 16), stat(Stat.LUCK, 2),          stat(Stat.SWIFTNESS, 2),
            item(Items.DIAMOND, 8),         stat(Stat.LUCK, 3),             item(Items.NETHERITE_SCRAP, 2));

        // ── Excavating ────────────────────────────────────────────────────────
        // Dirt/sand/gravel: very easy, high volume. Pure stats + XP bottles.
        reg(CollectionType.MUD,
            new long[]{ 100, 250, 500, 1_000, 2_500, 5_000, 10_000, 25_000, 50_000 },
            stat(Stat.HEALTH, 1),           stat(Stat.TOUGHNESS, 1),        stat(Stat.HEALTH, 2),
            item(Items.EXPERIENCE_BOTTLE, 16), stat(Stat.TOUGHNESS, 2),     stat(Stat.HEALTH, 2),
            item(Items.DIAMOND, 4),         stat(Stat.TOUGHNESS, 3),        item(Items.GOLDEN_APPLE, 2));

        reg(CollectionType.DIRT,
            new long[]{ 100, 250, 500, 1_000, 2_500, 5_000, 10_000, 25_000, 50_000 },
            stat(Stat.TOUGHNESS, 1),        stat(Stat.HEALTH, 1),           stat(Stat.TOUGHNESS, 2),
            item(Items.EXPERIENCE_BOTTLE, 16), stat(Stat.HEALTH, 2),        stat(Stat.TOUGHNESS, 2),
            item(Items.DIAMOND, 4),         stat(Stat.HEALTH, 3),           item(Items.GOLDEN_APPLE, 2));

        reg(CollectionType.SAND,
            new long[]{ 100, 250, 500, 1_000, 2_500, 5_000, 10_000, 25_000, 50_000 },
            stat(Stat.LUCK, 1),             stat(Stat.LUCK, 1),             stat(Stat.LUCK, 2),
            item(Items.EXPERIENCE_BOTTLE, 16), stat(Stat.LUCK, 2),          stat(Stat.LUCK, 2),
            item(Items.DIAMOND, 4),         stat(Stat.LUCK, 3),             item(Items.GOLDEN_APPLE, 2));

        reg(CollectionType.RED_SAND,
            new long[]{ 100, 250, 500, 1_000, 2_500, 5_000, 10_000, 25_000, 50_000 },
            stat(Stat.LUCK, 1),             stat(Stat.LUCK, 1),             stat(Stat.LUCK, 2),
            item(Items.EXPERIENCE_BOTTLE, 16), stat(Stat.LUCK, 2),          stat(Stat.LUCK, 2),
            item(Items.DIAMOND, 4),         stat(Stat.LUCK, 3),             item(Items.GOLDEN_APPLE, 2));

        reg(CollectionType.GRAVEL,
            new long[]{ 100, 250, 500, 1_000, 2_500, 5_000, 10_000, 25_000, 50_000 },
            stat(Stat.DEFENSE, 1),          stat(Stat.DEFENSE, 1),          stat(Stat.TOUGHNESS, 1),
            item(Items.EXPERIENCE_BOTTLE, 16), stat(Stat.DEFENSE, 2),       stat(Stat.TOUGHNESS, 2),
            item(Items.DIAMOND, 4),         stat(Stat.DEFENSE, 3),          item(Items.GOLDEN_APPLE, 2));

        reg(CollectionType.CLAY,
            new long[]{ 50, 150, 300, 600, 1_000, 2_500, 5_000, 10_000, 25_000 },
            stat(Stat.DEFENSE, 1),          stat(Stat.HEALTH, 1),           stat(Stat.DEFENSE, 2),
            item(Items.EXPERIENCE_BOTTLE, 16), stat(Stat.HEALTH, 2),        stat(Stat.DEFENSE, 2),
            item(Items.DIAMOND, 4),         stat(Stat.HEALTH, 3),           item(Items.GOLDEN_APPLE, 2));

        // Soul Sand: nether effort. Better rewards.
        reg(CollectionType.SOUL_SAND,
            new long[]{ 50, 150, 300, 600, 1_000, 2_500, 5_000, 10_000, 25_000 },
            stat(Stat.TOUGHNESS, 1),        stat(Stat.SWIFTNESS, 1),        stat(Stat.TOUGHNESS, 2),
            item(Items.EXPERIENCE_BOTTLE, 16), stat(Stat.SWIFTNESS, 2),     stat(Stat.TOUGHNESS, 2),
            item(Items.DIAMOND, 6),         stat(Stat.SWIFTNESS, 3),        item(Items.NETHERITE_SCRAP, 2));

        // ── Farming ───────────────────────────────────────────────────────────
        // Farming crops: easy volume. Health stats + golden food capstones.
        reg(CollectionType.BEETROOT,
            new long[]{ 50, 150, 300, 600, 1_000, 2_500, 5_000, 10_000, 25_000 },
            stat(Stat.HEALTH, 1),           stat(Stat.LUCK, 1),             stat(Stat.HEALTH, 2),
            item(Items.EXPERIENCE_BOTTLE, 16), stat(Stat.LUCK, 2),          stat(Stat.HEALTH, 2),
            item(Items.DIAMOND, 4),         stat(Stat.LUCK, 3),             item(Items.GOLDEN_APPLE, 2));

        reg(CollectionType.CACTUS,
            new long[]{ 50, 150, 300, 600, 1_000, 2_500, 5_000, 10_000, 25_000 },
            stat(Stat.DEFENSE, 1),          stat(Stat.TOUGHNESS, 1),        stat(Stat.DEFENSE, 2),
            item(Items.EXPERIENCE_BOTTLE, 16), stat(Stat.TOUGHNESS, 2),     stat(Stat.DEFENSE, 2),
            item(Items.DIAMOND, 4),         stat(Stat.TOUGHNESS, 3),        item(Items.GOLDEN_APPLE, 2));

        // Nether Wart: nether-exclusive crop. Better rewards.
        reg(CollectionType.NETHER_WART,
            new long[]{ 25, 75, 200, 400, 750, 1_500, 3_000, 7_500, 15_000 },
            stat(Stat.LUCK, 1),             stat(Stat.HEALTH, 1),           stat(Stat.LUCK, 2),
            item(Items.BREWING_STAND, 1),   stat(Stat.HEALTH, 2),           stat(Stat.LUCK, 2),
            item(Items.DIAMOND, 6),         stat(Stat.HEALTH, 3),           item(Items.NETHERITE_SCRAP, 2));

        reg(CollectionType.COCOA_BEANS,
            new long[]{ 25, 75, 200, 400, 750, 1_500, 3_000, 7_500, 15_000 },
            stat(Stat.LUCK, 1),             stat(Stat.LUCK, 1),             stat(Stat.LUCK, 2),
            item(Items.EXPERIENCE_BOTTLE, 16), stat(Stat.LUCK, 2),          stat(Stat.LUCK, 2),
            item(Items.DIAMOND, 6),         stat(Stat.LUCK, 3),             item(Items.GOLDEN_APPLE, 4));

        reg(CollectionType.SWEET_BERRY,
            new long[]{ 25, 75, 150, 300, 600, 1_500, 3_000, 7_500, 15_000 },
            stat(Stat.HEALTH, 1),           stat(Stat.HEALTH, 1),           stat(Stat.HEALTH, 2),
            item(Items.EXPERIENCE_BOTTLE, 16), stat(Stat.HEALTH, 2),        stat(Stat.HEALTH, 2),
            item(Items.GOLDEN_APPLE, 2),    stat(Stat.LUCK, 3),             item(Items.GOLDEN_APPLE, 4));

        reg(CollectionType.KELP,
            new long[]{ 100, 300, 600, 1_200, 2_500, 5_000, 10_000, 25_000, 50_000 },
            stat(Stat.HEALTH, 1),           stat(Stat.SWIFTNESS, 1),        stat(Stat.HEALTH, 2),
            item(Items.EXPERIENCE_BOTTLE, 16), stat(Stat.SWIFTNESS, 2),     stat(Stat.HEALTH, 2),
            item(Items.DIAMOND, 4),         stat(Stat.SWIFTNESS, 3),        item(Items.GOLDEN_APPLE, 2));

        reg(CollectionType.MUSHROOM,
            new long[]{ 25, 75, 200, 400, 750, 1_500, 3_000, 7_500, 15_000 },
            stat(Stat.HEALTH, 1),           stat(Stat.LUCK, 1),             stat(Stat.HEALTH, 2),
            item(Items.EXPERIENCE_BOTTLE, 16), stat(Stat.LUCK, 2),          stat(Stat.HEALTH, 2),
            item(Items.GOLDEN_APPLE, 2),    stat(Stat.LUCK, 3),             item(Items.NETHERITE_SCRAP, 2));

        reg(CollectionType.WHEAT,
            new long[]{ 50, 150, 300, 600, 1_000, 2_500, 5_000, 10_000, 25_000 },
            stat(Stat.HEALTH, 1),           stat(Stat.HEALTH, 1),           stat(Stat.HEALTH, 2),
            item(Items.EXPERIENCE_BOTTLE, 16), stat(Stat.HEALTH, 2),        stat(Stat.HEALTH, 2),
            item(Items.DIAMOND, 4),         stat(Stat.HEALTH, 4),           item(Items.GOLDEN_APPLE, 2));

        reg(CollectionType.CARROT,
            new long[]{ 50, 150, 300, 600, 1_000, 2_500, 5_000, 10_000, 25_000 },
            stat(Stat.HEALTH, 1),           stat(Stat.LUCK, 1),             stat(Stat.HEALTH, 2),
            item(Items.GOLDEN_CARROT, 4),   stat(Stat.LUCK, 2),             stat(Stat.HEALTH, 2),
            item(Items.GOLDEN_APPLE, 2),    stat(Stat.LUCK, 3),             item(Items.GOLDEN_APPLE, 4));

        reg(CollectionType.POTATO,
            new long[]{ 50, 150, 300, 600, 1_000, 2_500, 5_000, 10_000, 25_000 },
            stat(Stat.HEALTH, 1),           stat(Stat.HEALTH, 1),           stat(Stat.HEALTH, 2),
            item(Items.EXPERIENCE_BOTTLE, 16), stat(Stat.HEALTH, 2),        stat(Stat.HEALTH, 2),
            item(Items.GOLDEN_CARROT, 8),   stat(Stat.HEALTH, 3),           item(Items.GOLDEN_APPLE, 2));

        reg(CollectionType.SUGAR_CANE,
            new long[]{ 50, 150, 300, 600, 1_000, 2_500, 5_000, 10_000, 25_000 },
            stat(Stat.LUCK, 1),             stat(Stat.LUCK, 1),             stat(Stat.LUCK, 2),
            item(Items.EXPERIENCE_BOTTLE, 16), stat(Stat.LUCK, 2),          stat(Stat.LUCK, 2),
            item(Items.DIAMOND, 4),         stat(Stat.LUCK, 4),             item(Items.GOLDEN_APPLE, 2));

        reg(CollectionType.PUMPKIN,
            new long[]{ 25, 75, 200, 400, 750, 1_500, 3_000, 7_500, 15_000 },
            stat(Stat.HEALTH, 1),           stat(Stat.TOUGHNESS, 1),        stat(Stat.HEALTH, 2),
            item(Items.EXPERIENCE_BOTTLE, 16), stat(Stat.TOUGHNESS, 2),     stat(Stat.HEALTH, 2),
            item(Items.DIAMOND, 6),         stat(Stat.TOUGHNESS, 3),        item(Items.GOLDEN_APPLE, 4));

        reg(CollectionType.MELON,
            new long[]{ 50, 150, 300, 600, 1_000, 2_500, 5_000, 10_000, 25_000 },
            stat(Stat.HEALTH, 1),           stat(Stat.HEALTH, 1),           stat(Stat.HEALTH, 2),
            item(Items.GOLDEN_CARROT, 4),   stat(Stat.HEALTH, 2),           stat(Stat.HEALTH, 2),
            item(Items.GOLDEN_APPLE, 2),    stat(Stat.HEALTH, 4),           item(Items.GOLDEN_APPLE, 4));

        // ── Fishing ───────────────────────────────────────────────────────────
        reg(CollectionType.COD,
            new long[]{ 25, 75, 150, 300, 600, 1_500, 3_000, 7_500, 15_000 },
            stat(Stat.LUCK, 1),             stat(Stat.LUCK, 1),             stat(Stat.LUCK, 2),
            item(Items.EXPERIENCE_BOTTLE, 16), stat(Stat.LUCK, 2),          stat(Stat.LUCK, 2),
            item(Items.DIAMOND, 4),         stat(Stat.LUCK, 3),             item(Items.GOLDEN_APPLE, 2));

        reg(CollectionType.SALMON,
            new long[]{ 25, 75, 150, 300, 600, 1_500, 3_000, 7_500, 15_000 },
            stat(Stat.LUCK, 1),             stat(Stat.LUCK, 1),             stat(Stat.LUCK, 2),
            item(Items.EXPERIENCE_BOTTLE, 16), stat(Stat.LUCK, 2),          stat(Stat.LUCK, 2),
            item(Items.DIAMOND, 4),         stat(Stat.LUCK, 3),             item(Items.GOLDEN_APPLE, 2));

        // Pufferfish: rarer catch. Better mid-tier rewards.
        reg(CollectionType.PUFFERFISH,
            new long[]{ 10, 30, 75, 150, 300, 750, 1_500, 3_000, 7_500 },
            stat(Stat.LUCK, 1),             stat(Stat.HEALTH, 1),           stat(Stat.LUCK, 2),
            item(Items.EXPERIENCE_BOTTLE, 16), stat(Stat.HEALTH, 2),        stat(Stat.LUCK, 2),
            item(Items.DIAMOND, 6),         stat(Stat.HEALTH, 3),           item(Items.NETHERITE_SCRAP, 2));

        // Tropical Fish: very rare catch.
        reg(CollectionType.TROPICAL_FISH,
            new long[]{ 10, 30, 75, 150, 300, 750, 1_500, 3_000, 7_500 },
            stat(Stat.LUCK, 2),             stat(Stat.LUCK, 2),             stat(Stat.LUCK, 3),
            item(Items.EXPERIENCE_BOTTLE, 16), stat(Stat.LUCK, 3),          stat(Stat.SWIFTNESS, 2),
            item(Items.DIAMOND, 8),         stat(Stat.LUCK, 4),             item(Items.NETHERITE_SCRAP, 2));

        reg(CollectionType.LILY_PAD,
            new long[]{ 50, 150, 300, 600, 1_000, 2_500, 5_000, 10_000, 25_000 },
            stat(Stat.LUCK, 1),             stat(Stat.HEALTH, 1),           stat(Stat.LUCK, 2),
            item(Items.EXPERIENCE_BOTTLE, 16), stat(Stat.HEALTH, 2),        stat(Stat.LUCK, 2),
            item(Items.DIAMOND, 4),         stat(Stat.HEALTH, 2),           item(Items.GOLDEN_APPLE, 2));

        // Ink Sac: moderate. Writing/enchanting theme.
        reg(CollectionType.INK_SAC,
            new long[]{ 25, 75, 150, 300, 600, 1_500, 3_000, 7_500, 15_000 },
            stat(Stat.LUCK, 1),             stat(Stat.LUCK, 1),             stat(Stat.LUCK, 2),
            item(Items.EXPERIENCE_BOTTLE, 32), stat(Stat.LUCK, 2),          stat(Stat.LUCK, 2),
            item(Items.DIAMOND, 4),         stat(Stat.LUCK, 3),             item(Items.GOLDEN_APPLE, 4));

        // Nautilus Shell: rare. Heart of the Sea at tier 5 is exceptional (you'd never give this normally).
        reg(CollectionType.NAUTILUS_SHELL,
            new long[]{ 5, 15, 30, 75, 150, 300, 750, 1_500, 3_000 },
            stat(Stat.LUCK, 1),             stat(Stat.TOUGHNESS, 1),        stat(Stat.LUCK, 2),
            item(Items.HEART_OF_THE_SEA, 1), stat(Stat.TOUGHNESS, 2),      stat(Stat.LUCK, 2),
            item(Items.NETHERITE_SCRAP, 2), stat(Stat.TOUGHNESS, 3),        item(Items.NETHER_STAR, 1));

        // Prismarine: ocean monument grind. Decent mid rewards.
        reg(CollectionType.PRISMARINE,
            new long[]{ 25, 75, 150, 300, 600, 1_500, 3_000, 7_500, 15_000 },
            stat(Stat.DEFENSE, 1),          stat(Stat.TOUGHNESS, 1),        stat(Stat.DEFENSE, 2),
            item(Items.EXPERIENCE_BOTTLE, 16), stat(Stat.TOUGHNESS, 2),     stat(Stat.DEFENSE, 2),
            item(Items.DIAMOND, 8),         stat(Stat.TOUGHNESS, 3),        item(Items.NETHERITE_SCRAP, 2));

        // ── Combat ────────────────────────────────────────────────────────────
        // Zombie: easy mob. Stats focus. Diamond sword at tier 7 is the first really good item.
        reg(CollectionType.ZOMBIE,
            new long[]{ 50, 150, 300, 600, 1_000, 2_500, 5_000, 10_000, 25_000 },
            stat(Stat.STRENGTH, 1),         stat(Stat.STRENGTH, 1),         stat(Stat.STRENGTH, 2),
            item(Items.EXPERIENCE_BOTTLE, 16), stat(Stat.STRENGTH, 2),      stat(Stat.STRENGTH, 2),
            item(Items.DIAMOND_SWORD, 1),   stat(Stat.STRENGTH, 4),         item(Items.GOLDEN_APPLE, 4));

        // Skeleton: easy. Bow at tier 4, crossbow at tier 7.
        reg(CollectionType.SKELETON,
            new long[]{ 50, 150, 300, 600, 1_000, 2_500, 5_000, 10_000, 25_000 },
            stat(Stat.LUCK, 1),             stat(Stat.LUCK, 1),             stat(Stat.LUCK, 2),
            item(Items.BOW, 1),             stat(Stat.LUCK, 2),             stat(Stat.LUCK, 2),
            item(Items.CROSSBOW, 1),        stat(Stat.LUCK, 4),             item(Items.GOLDEN_APPLE, 4));

        // Spider: easy. String into fishing rod at tier 4 is thematic.
        reg(CollectionType.SPIDER,
            new long[]{ 50, 150, 300, 600, 1_000, 2_500, 5_000, 10_000, 25_000 },
            stat(Stat.SWIFTNESS, 1),        stat(Stat.SWIFTNESS, 1),        stat(Stat.LUCK, 2),
            item(Items.FISHING_ROD, 1),     stat(Stat.SWIFTNESS, 2),        stat(Stat.LUCK, 2),
            item(Items.DIAMOND, 6),         stat(Stat.SWIFTNESS, 4),        item(Items.GOLDEN_APPLE, 4));

        // Creeper: moderate. TNT access.
        reg(CollectionType.CREEPER,
            new long[]{ 50, 150, 300, 600, 1_000, 2_500, 5_000, 10_000, 25_000 },
            stat(Stat.STRENGTH, 1),         stat(Stat.TOUGHNESS, 1),        stat(Stat.STRENGTH, 2),
            item(Items.TNT, 8),             stat(Stat.TOUGHNESS, 2),        stat(Stat.STRENGTH, 2),
            item(Items.DIAMOND, 8),         stat(Stat.TOUGHNESS, 3),        item(Items.GOLDEN_APPLE, 4));

        // Enderman: hard to kill. Ender Eye at tier 4, netherite scrap capstone.
        reg(CollectionType.ENDERMAN,
            new long[]{ 10, 30, 75, 150, 300, 600, 1_500, 3_000, 7_500 },
            stat(Stat.LUCK, 2),             stat(Stat.STRENGTH, 2),         stat(Stat.LUCK, 3),
            item(Items.ENDER_EYE, 4),       stat(Stat.STRENGTH, 2),         stat(Stat.LUCK, 3),
            item(Items.DIAMOND, 12),        stat(Stat.STRENGTH, 4),         item(Items.NETHERITE_SCRAP, 4));

        // Blaze: nether, harder. Better scaling.
        reg(CollectionType.BLAZE,
            new long[]{ 10, 30, 75, 150, 300, 750, 1_500, 3_000, 7_500 },
            stat(Stat.STRENGTH, 1),         stat(Stat.STRENGTH, 1),         stat(Stat.STRENGTH, 2),
            item(Items.EXPERIENCE_BOTTLE, 16), stat(Stat.STRENGTH, 2),      stat(Stat.TOUGHNESS, 2),
            item(Items.DIAMOND, 8),         stat(Stat.STRENGTH, 4),         item(Items.NETHERITE_SCRAP, 2));

        // Ghast: rare nether. Ghast Tear at tier 4 (genuine use in potions), netherite scrap cap.
        reg(CollectionType.GHAST,
            new long[]{ 5, 15, 35, 75, 150, 400, 800, 2_000, 5_000 },
            stat(Stat.TOUGHNESS, 1),        stat(Stat.TOUGHNESS, 1),        stat(Stat.HEALTH, 2),
            item(Items.GHAST_TEAR, 4),      stat(Stat.TOUGHNESS, 2),        stat(Stat.HEALTH, 2),
            item(Items.DIAMOND, 12),        stat(Stat.TOUGHNESS, 3),        item(Items.NETHERITE_SCRAP, 4));

        // Wither Skeleton: very hard (5k kills). Skull at tier 7, Nether Star capstone.
        reg(CollectionType.WITHER_SKELETON,
            new long[]{ 5, 15, 35, 75, 150, 400, 800, 2_000, 5_000 },
            stat(Stat.STRENGTH, 1),         stat(Stat.STRENGTH, 1),         stat(Stat.STRENGTH, 2),
            item(Items.EXPERIENCE_BOTTLE, 16), stat(Stat.STRENGTH, 2),      stat(Stat.TOUGHNESS, 2),
            item(Items.WITHER_SKELETON_SKULL, 1), stat(Stat.STRENGTH, 4),   item(Items.NETHER_STAR, 1));

        // Piglin: moderate nether.
        reg(CollectionType.PIGLIN,
            new long[]{ 25, 75, 150, 300, 600, 1_500, 3_000, 7_500, 15_000 },
            stat(Stat.LUCK, 1),             stat(Stat.LUCK, 1),             stat(Stat.LUCK, 2),
            item(Items.EXPERIENCE_BOTTLE, 16), stat(Stat.LUCK, 2),          stat(Stat.LUCK, 2),
            item(Items.GOLDEN_APPLE, 4),    stat(Stat.LUCK, 4),             item(Items.NETHERITE_SCRAP, 2));

        // Witch: moderate. Glass bottle → alchemy theme.
        reg(CollectionType.WITCH,
            new long[]{ 25, 75, 150, 300, 600, 1_500, 3_000, 7_500, 15_000 },
            stat(Stat.LUCK, 1),             stat(Stat.HEALTH, 1),           stat(Stat.LUCK, 2),
            item(Items.BREWING_STAND, 1),   stat(Stat.HEALTH, 2),           stat(Stat.LUCK, 2),
            item(Items.DIAMOND, 8),         stat(Stat.HEALTH, 3),           item(Items.NETHERITE_SCRAP, 2));

        // Phantom: hard (only spawns when you don't sleep). Totem capstone.
        reg(CollectionType.PHANTOM,
            new long[]{ 10, 30, 75, 150, 300, 750, 1_500, 3_000, 7_500 },
            stat(Stat.SWIFTNESS, 1),        stat(Stat.SWIFTNESS, 1),        stat(Stat.TOUGHNESS, 2),
            item(Items.EXPERIENCE_BOTTLE, 16), stat(Stat.SWIFTNESS, 2),     stat(Stat.TOUGHNESS, 2),
            item(Items.DIAMOND, 8),         stat(Stat.SWIFTNESS, 4),        item(Items.TOTEM_OF_UNDYING, 1));

        // Slime: moderate.
        reg(CollectionType.SLIME,
            new long[]{ 25, 75, 200, 400, 750, 1_500, 3_000, 7_500, 15_000 },
            stat(Stat.HEALTH, 1),           stat(Stat.TOUGHNESS, 1),        stat(Stat.HEALTH, 2),
            item(Items.EXPERIENCE_BOTTLE, 16), stat(Stat.TOUGHNESS, 2),     stat(Stat.HEALTH, 2),
            item(Items.DIAMOND, 6),         stat(Stat.TOUGHNESS, 3),        item(Items.GOLDEN_APPLE, 4));

        // Magma Cube: nether, harder than slime.
        reg(CollectionType.MAGMA_CUBE,
            new long[]{ 10, 30, 75, 150, 300, 750, 1_500, 3_000, 7_500 },
            stat(Stat.STRENGTH, 1),         stat(Stat.TOUGHNESS, 1),        stat(Stat.STRENGTH, 2),
            item(Items.EXPERIENCE_BOTTLE, 16), stat(Stat.TOUGHNESS, 2),     stat(Stat.STRENGTH, 2),
            item(Items.DIAMOND, 8),         stat(Stat.TOUGHNESS, 3),        item(Items.NETHERITE_SCRAP, 2));

        // Pillager: moderate. Shield at tier 4 (relevant for pillager fighting). Totem capstone.
        reg(CollectionType.PILLAGER,
            new long[]{ 25, 75, 150, 300, 600, 1_500, 3_000, 7_500, 15_000 },
            stat(Stat.STRENGTH, 1),         stat(Stat.LUCK, 1),             stat(Stat.STRENGTH, 2),
            item(Items.SHIELD, 1),          stat(Stat.LUCK, 2),             stat(Stat.STRENGTH, 2),
            item(Items.DIAMOND_SWORD, 1),   stat(Stat.LUCK, 4),             item(Items.TOTEM_OF_UNDYING, 1));
    }

    // ── Block matching ────────────────────────────────────────────────────────

    public static Optional<CollectionType> fromBlock(BlockState state) {
        Block block = state.getBlock();

        if (state.isIn(BlockTags.COAL_ORES))     return Optional.of(CollectionType.COAL);
        if (state.isIn(BlockTags.IRON_ORES))     return Optional.of(CollectionType.IRON);
        if (state.isIn(BlockTags.GOLD_ORES))     return Optional.of(CollectionType.GOLD);
        if (state.isIn(BlockTags.DIAMOND_ORES))  return Optional.of(CollectionType.DIAMOND);
        if (state.isIn(BlockTags.COPPER_ORES))   return Optional.of(CollectionType.COPPER);
        if (state.isIn(BlockTags.LAPIS_ORES))    return Optional.of(CollectionType.LAPIS);
        if (state.isIn(BlockTags.REDSTONE_ORES)) return Optional.of(CollectionType.REDSTONE);
        if (state.isIn(BlockTags.EMERALD_ORES))  return Optional.of(CollectionType.EMERALD);

        if (block == Blocks.NETHER_QUARTZ_ORE)
            return Optional.of(CollectionType.NETHER_QUARTZ);
        if (block == Blocks.ANCIENT_DEBRIS)
            return Optional.of(CollectionType.ANCIENT_DEBRIS);
        if (block == Blocks.OBSIDIAN || block == Blocks.CRYING_OBSIDIAN)
            return Optional.of(CollectionType.OBSIDIAN);

        if (block == Blocks.COBBLESTONE    || block == Blocks.STONE
         || block == Blocks.DEEPSLATE      || block == Blocks.COBBLED_DEEPSLATE
         || block == Blocks.GRANITE        || block == Blocks.DIORITE
         || block == Blocks.ANDESITE       || block == Blocks.SMOOTH_STONE
         || block == Blocks.POLISHED_GRANITE || block == Blocks.POLISHED_DIORITE
         || block == Blocks.POLISHED_ANDESITE)
            return Optional.of(CollectionType.COBBLESTONE);

        if (block == Blocks.CRIMSON_STEM   || block == Blocks.STRIPPED_CRIMSON_STEM
         || block == Blocks.CRIMSON_HYPHAE || block == Blocks.STRIPPED_CRIMSON_HYPHAE)
            return Optional.of(CollectionType.CRIMSON_WOOD);
        if (block == Blocks.WARPED_STEM    || block == Blocks.STRIPPED_WARPED_STEM
         || block == Blocks.WARPED_HYPHAE  || block == Blocks.STRIPPED_WARPED_HYPHAE)
            return Optional.of(CollectionType.WARPED_WOOD);

        if (block == Blocks.OAK_LOG        || block == Blocks.OAK_WOOD
         || block == Blocks.STRIPPED_OAK_LOG || block == Blocks.STRIPPED_OAK_WOOD)
            return Optional.of(CollectionType.OAK_WOOD);
        if (block == Blocks.BIRCH_LOG      || block == Blocks.BIRCH_WOOD
         || block == Blocks.STRIPPED_BIRCH_LOG || block == Blocks.STRIPPED_BIRCH_WOOD)
            return Optional.of(CollectionType.BIRCH_WOOD);
        if (block == Blocks.SPRUCE_LOG     || block == Blocks.SPRUCE_WOOD
         || block == Blocks.STRIPPED_SPRUCE_LOG || block == Blocks.STRIPPED_SPRUCE_WOOD)
            return Optional.of(CollectionType.SPRUCE_WOOD);
        if (block == Blocks.JUNGLE_LOG     || block == Blocks.JUNGLE_WOOD
         || block == Blocks.STRIPPED_JUNGLE_LOG || block == Blocks.STRIPPED_JUNGLE_WOOD)
            return Optional.of(CollectionType.JUNGLE_WOOD);
        if (block == Blocks.ACACIA_LOG     || block == Blocks.ACACIA_WOOD
         || block == Blocks.STRIPPED_ACACIA_LOG || block == Blocks.STRIPPED_ACACIA_WOOD)
            return Optional.of(CollectionType.ACACIA_WOOD);
        if (block == Blocks.DARK_OAK_LOG   || block == Blocks.DARK_OAK_WOOD
         || block == Blocks.STRIPPED_DARK_OAK_LOG || block == Blocks.STRIPPED_DARK_OAK_WOOD)
            return Optional.of(CollectionType.DARK_OAK_WOOD);
        if (block == Blocks.MANGROVE_LOG   || block == Blocks.MANGROVE_WOOD
         || block == Blocks.STRIPPED_MANGROVE_LOG || block == Blocks.STRIPPED_MANGROVE_WOOD)
            return Optional.of(CollectionType.MANGROVE_WOOD);
        if (block == Blocks.CHERRY_LOG     || block == Blocks.CHERRY_WOOD
         || block == Blocks.STRIPPED_CHERRY_LOG || block == Blocks.STRIPPED_CHERRY_WOOD)
            return Optional.of(CollectionType.CHERRY_WOOD);

        if (block == Blocks.DIRT          || block == Blocks.GRASS_BLOCK
         || block == Blocks.PODZOL        || block == Blocks.MYCELIUM
         || block == Blocks.ROOTED_DIRT   || block == Blocks.COARSE_DIRT)
            return Optional.of(CollectionType.DIRT);

        if (block == Blocks.SAND)
            return Optional.of(CollectionType.SAND);
        if (block == Blocks.RED_SAND)
            return Optional.of(CollectionType.RED_SAND);
        if (block == Blocks.MUD || block == Blocks.MUDDY_MANGROVE_ROOTS)
            return Optional.of(CollectionType.MUD);
        if (block == Blocks.GRAVEL)
            return Optional.of(CollectionType.GRAVEL);
        if (block == Blocks.CLAY)
            return Optional.of(CollectionType.CLAY);
        if (block == Blocks.SOUL_SAND || block == Blocks.SOUL_SOIL)
            return Optional.of(CollectionType.SOUL_SAND);

        if (block instanceof CropBlock crop && crop.isMature(state)) {
            if (block == Blocks.WHEAT)     return Optional.of(CollectionType.WHEAT);
            if (block == Blocks.CARROTS)   return Optional.of(CollectionType.CARROT);
            if (block == Blocks.POTATOES)  return Optional.of(CollectionType.POTATO);
            if (block == Blocks.BEETROOTS) return Optional.of(CollectionType.BEETROOT);
        }
        if (block == Blocks.SUGAR_CANE)    return Optional.of(CollectionType.SUGAR_CANE);
        if (block == Blocks.CACTUS)        return Optional.of(CollectionType.CACTUS);
        if (block == Blocks.PUMPKIN)       return Optional.of(CollectionType.PUMPKIN);
        if (block == Blocks.MELON)         return Optional.of(CollectionType.MELON);
        if (block == Blocks.NETHER_WART
                && state.get(net.minecraft.block.NetherWartBlock.AGE) == 3)
            return Optional.of(CollectionType.NETHER_WART);
        if (block == Blocks.COCOA
                && state.get(net.minecraft.block.CocoaBlock.AGE) == 2)
            return Optional.of(CollectionType.COCOA_BEANS);
        if (block == Blocks.SWEET_BERRY_BUSH
                && state.get(net.minecraft.block.SweetBerryBushBlock.AGE) == 3)
            return Optional.of(CollectionType.SWEET_BERRY);
        if (block == Blocks.BAMBOO)
            return Optional.of(CollectionType.BAMBOO_WOOD);
        if (block == Blocks.KELP || block == Blocks.KELP_PLANT)
            return Optional.of(CollectionType.KELP);
        if (block == Blocks.RED_MUSHROOM   || block == Blocks.BROWN_MUSHROOM
         || block == Blocks.RED_MUSHROOM_BLOCK || block == Blocks.BROWN_MUSHROOM_BLOCK)
            return Optional.of(CollectionType.MUSHROOM);

        return Optional.empty();
    }

    public static Optional<CollectionType> fromCombatDrop(Item item) {
        if (item == Items.ROTTEN_FLESH)          return Optional.of(CollectionType.ZOMBIE);
        if (item == Items.BONE)                  return Optional.of(CollectionType.SKELETON);
        if (item == Items.STRING)                return Optional.of(CollectionType.SPIDER);
        if (item == Items.GUNPOWDER)             return Optional.of(CollectionType.CREEPER);
        if (item == Items.ENDER_PEARL)           return Optional.of(CollectionType.ENDERMAN);
        if (item == Items.BLAZE_ROD)             return Optional.of(CollectionType.BLAZE);
        if (item == Items.GHAST_TEAR)            return Optional.of(CollectionType.GHAST);
        if (item == Items.WITHER_SKELETON_SKULL) return Optional.of(CollectionType.WITHER_SKELETON);
        if (item == Items.GOLD_NUGGET)           return Optional.of(CollectionType.PIGLIN);
        if (item == Items.GLASS_BOTTLE)          return Optional.of(CollectionType.WITCH);
        if (item == Items.PHANTOM_MEMBRANE)      return Optional.of(CollectionType.PHANTOM);
        if (item == Items.SLIME_BALL)            return Optional.of(CollectionType.SLIME);
        if (item == Items.MAGMA_CREAM)           return Optional.of(CollectionType.MAGMA_CUBE);
        if (item == Items.CROSSBOW)              return Optional.of(CollectionType.PILLAGER);
        return Optional.empty();
    }

    // ── Public accessors ──────────────────────────────────────────────────────

    public static List<CollectionTier> getTiers(CollectionType type) {
        return TIERS.getOrDefault(type, List.of());
    }

    // ── Registration helpers ──────────────────────────────────────────────────

    private static void reg(CollectionType type, long[] thresholds, CollectionReward... rewards) {
        List<CollectionTier> tiers = new ArrayList<>(thresholds.length);
        for (int i = 0; i < thresholds.length; i++) {
            CollectionReward reward = i < rewards.length ? rewards[i] : null;
            List<CollectionReward> list = reward != null ? List.of(reward) : List.of();
            tiers.add(new CollectionTier(i + 1, thresholds[i], list));
        }
        TIERS.put(type, List.copyOf(tiers));
    }

    private static CollectionReward item(Item item, int count) {
        return new CollectionReward.ItemReward(new ItemStack(item, count));
    }

    private static CollectionReward stat(Stat stat, int levels) {
        return new CollectionReward.StatBonus(stat, levels);
    }

    @SuppressWarnings("unused")
    private static CollectionReward recipe(String id) {
        return new CollectionReward.RecipeUnlock(Identifier.of(id));
    }
}
