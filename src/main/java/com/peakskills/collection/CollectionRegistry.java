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
 * Also provides block/entity → CollectionType matching.
 */
public class CollectionRegistry {

    private static final Map<CollectionType, List<CollectionTier>> TIERS =
        new EnumMap<>(CollectionType.class);

    static {
        // ── Mining ────────────────────────────────────────────────────────────
        reg(CollectionType.COBBLESTONE,
            new long[]{ 50, 150, 500, 1_000, 2_500, 5_000, 10_000, 25_000, 50_000 },
            item(Items.COBBLESTONE, 32),    stat(Stat.DEFENSE, 1),          item(Items.COBBLESTONE, 64),
            stat(Stat.TOUGHNESS, 1),        item(Items.STONE_BRICKS, 32),   stat(Stat.DEFENSE, 2),
            recipe("minecraft:cobblestone_wall"), stat(Stat.TOUGHNESS, 2),  stat(Stat.DEFENSE, 4));

        reg(CollectionType.COAL,
            new long[]{ 50, 150, 300, 600, 1_000, 2_500, 5_000, 10_000, 25_000 },
            item(Items.COAL, 16),           stat(Stat.LUCK, 1),             item(Items.COAL, 32),
            stat(Stat.LUCK, 1),             recipe("minecraft:coal_block"),  item(Items.COAL, 64),
            stat(Stat.STRENGTH, 1),         item(Items.COAL_BLOCK, 4),       stat(Stat.LUCK, 3));

        reg(CollectionType.IRON,
            new long[]{ 50, 150, 300, 600, 1_000, 2_500, 5_000, 10_000, 25_000 },
            item(Items.IRON_INGOT, 8),      stat(Stat.DEFENSE, 1),          item(Items.IRON_INGOT, 16),
            stat(Stat.STRENGTH, 1),         recipe("minecraft:iron_block"),  item(Items.IRON_HELMET, 1),
            stat(Stat.DEFENSE, 2),          item(Items.IRON_INGOT, 32),      stat(Stat.STRENGTH, 3));

        reg(CollectionType.GOLD,
            new long[]{ 50, 150, 300, 600, 1_000, 2_500, 5_000, 10_000, 25_000 },
            item(Items.GOLD_INGOT, 8),      stat(Stat.LUCK, 1),             item(Items.GOLD_INGOT, 16),
            stat(Stat.LUCK, 1),             recipe("minecraft:gold_block"),  item(Items.GOLDEN_HELMET, 1),
            stat(Stat.LUCK, 2),             item(Items.GOLD_INGOT, 32),      stat(Stat.LUCK, 4));

        reg(CollectionType.DIAMOND,
            new long[]{ 10, 50, 100, 250, 500, 1_000, 2_500, 5_000, 10_000 },
            item(Items.DIAMOND, 2),         stat(Stat.TOUGHNESS, 1),        item(Items.DIAMOND, 4),
            stat(Stat.TOUGHNESS, 1),        recipe("minecraft:diamond_block"), item(Items.DIAMOND_HELMET, 1),
            stat(Stat.TOUGHNESS, 2),        item(Items.DIAMOND, 16),         stat(Stat.STRENGTH, 3));

        reg(CollectionType.COPPER,
            new long[]{ 50, 150, 300, 600, 1_000, 2_500, 5_000, 10_000, 25_000 },
            item(Items.COPPER_INGOT, 8),    stat(Stat.DEFENSE, 1),          item(Items.COPPER_INGOT, 16),
            stat(Stat.TOUGHNESS, 1),        recipe("minecraft:copper_block"), item(Items.COPPER_INGOT, 32),
            stat(Stat.DEFENSE, 2),          item(Items.COPPER_INGOT, 64),    stat(Stat.TOUGHNESS, 2));

        reg(CollectionType.LAPIS,
            new long[]{ 50, 150, 300, 600, 1_000, 2_500, 5_000, 10_000, 25_000 },
            item(Items.LAPIS_LAZULI, 16),   stat(Stat.LUCK, 1),             item(Items.LAPIS_LAZULI, 32),
            stat(Stat.LUCK, 1),             recipe("minecraft:lapis_block"), item(Items.LAPIS_LAZULI, 64),
            stat(Stat.LUCK, 2),             item(Items.LAPIS_BLOCK, 4),      stat(Stat.LUCK, 4));

        reg(CollectionType.REDSTONE,
            new long[]{ 50, 150, 300, 600, 1_000, 2_500, 5_000, 10_000, 25_000 },
            item(Items.REDSTONE, 16),       stat(Stat.SWIFTNESS, 1),        item(Items.REDSTONE, 32),
            stat(Stat.SWIFTNESS, 1),        recipe("minecraft:redstone_block"), item(Items.REDSTONE, 64),
            stat(Stat.SWIFTNESS, 2),        item(Items.REDSTONE_BLOCK, 4),   stat(Stat.SWIFTNESS, 4));

        reg(CollectionType.EMERALD,
            new long[]{ 10, 30, 75, 150, 300, 600, 1_500, 3_000, 7_500 },
            item(Items.EMERALD, 2),         stat(Stat.LUCK, 2),             item(Items.EMERALD, 4),
            stat(Stat.LUCK, 2),             recipe("minecraft:emerald_block"), item(Items.EMERALD, 8),
            stat(Stat.LUCK, 3),             item(Items.EMERALD, 16),         stat(Stat.LUCK, 5));

        reg(CollectionType.NETHER_QUARTZ,
            new long[]{ 25, 75, 200, 400, 750, 1_500, 3_000, 7_500, 15_000 },
            item(Items.QUARTZ, 16),         stat(Stat.LUCK, 1),             item(Items.QUARTZ, 32),
            stat(Stat.TOUGHNESS, 1),        recipe("minecraft:quartz_block"), item(Items.QUARTZ_BLOCK, 8),
            stat(Stat.LUCK, 2),             item(Items.QUARTZ, 64),          stat(Stat.TOUGHNESS, 3));

        reg(CollectionType.OBSIDIAN,
            new long[]{ 10, 30, 75, 150, 300, 750, 1_500, 3_000, 7_500 },
            item(Items.OBSIDIAN, 4),        stat(Stat.DEFENSE, 1),          item(Items.OBSIDIAN, 8),
            stat(Stat.TOUGHNESS, 1),        recipe("minecraft:enchanting_table"), item(Items.OBSIDIAN, 16),
            stat(Stat.DEFENSE, 2),          item(Items.OBSIDIAN, 32),        stat(Stat.TOUGHNESS, 3));

        reg(CollectionType.ANCIENT_DEBRIS,
            new long[]{ 1, 3, 7, 15, 30, 75, 150, 300, 750 },
            item(Items.ANCIENT_DEBRIS, 1),  stat(Stat.STRENGTH, 2),         item(Items.NETHERITE_SCRAP, 1),
            stat(Stat.DEFENSE, 2),          item(Items.ANCIENT_DEBRIS, 4),   stat(Stat.STRENGTH, 3),
            item(Items.NETHERITE_INGOT, 1), stat(Stat.DEFENSE, 3),           stat(Stat.STRENGTH, 5));

        // ── Woodcutting ───────────────────────────────────────────────────────
        reg(CollectionType.OAK_WOOD,
            new long[]{ 50, 150, 300, 600, 1_000, 2_500, 5_000, 10_000, 25_000 },
            item(Items.OAK_LOG, 32),        stat(Stat.STRENGTH, 1),         item(Items.OAK_LOG, 64),
            stat(Stat.STRENGTH, 1),         recipe("minecraft:stick"),       item(Items.OAK_PLANKS, 64),
            stat(Stat.STRENGTH, 2),         item(Items.WOODEN_AXE, 1),       stat(Stat.STRENGTH, 4));

        reg(CollectionType.BIRCH_WOOD,
            new long[]{ 50, 150, 300, 600, 1_000, 2_500, 5_000, 10_000, 25_000 },
            item(Items.BIRCH_LOG, 32),      stat(Stat.STRENGTH, 1),         item(Items.BIRCH_LOG, 64),
            stat(Stat.LUCK, 1),             recipe("minecraft:birch_planks"), item(Items.BIRCH_PLANKS, 64),
            stat(Stat.STRENGTH, 2),         item(Items.BIRCH_LOG, 64),       stat(Stat.LUCK, 3));

        reg(CollectionType.SPRUCE_WOOD,
            new long[]{ 50, 150, 300, 600, 1_000, 2_500, 5_000, 10_000, 25_000 },
            item(Items.SPRUCE_LOG, 32),     stat(Stat.STRENGTH, 1),         item(Items.SPRUCE_LOG, 64),
            stat(Stat.TOUGHNESS, 1),        recipe("minecraft:spruce_planks"), item(Items.SPRUCE_PLANKS, 64),
            stat(Stat.STRENGTH, 2),         item(Items.SPRUCE_LOG, 64),      stat(Stat.TOUGHNESS, 3));

        reg(CollectionType.JUNGLE_WOOD,
            new long[]{ 25, 75, 200, 400, 750, 1_500, 3_000, 7_500, 15_000 },
            item(Items.JUNGLE_LOG, 16),     stat(Stat.STRENGTH, 1),         item(Items.JUNGLE_LOG, 32),
            stat(Stat.HEALTH, 1),           recipe("minecraft:jungle_planks"), item(Items.JUNGLE_PLANKS, 64),
            stat(Stat.STRENGTH, 2),         item(Items.COCOA_BEANS, 16),     stat(Stat.HEALTH, 3));

        reg(CollectionType.ACACIA_WOOD,
            new long[]{ 25, 75, 200, 400, 750, 1_500, 3_000, 7_500, 15_000 },
            item(Items.ACACIA_LOG, 16),     stat(Stat.STRENGTH, 1),         item(Items.ACACIA_LOG, 32),
            stat(Stat.STRENGTH, 1),         recipe("minecraft:acacia_planks"), item(Items.ACACIA_PLANKS, 64),
            stat(Stat.STRENGTH, 2),         item(Items.ACACIA_LOG, 64),      stat(Stat.STRENGTH, 4));

        reg(CollectionType.DARK_OAK_WOOD,
            new long[]{ 25, 75, 200, 400, 750, 1_500, 3_000, 7_500, 15_000 },
            item(Items.DARK_OAK_LOG, 16),   stat(Stat.STRENGTH, 1),         item(Items.DARK_OAK_LOG, 32),
            stat(Stat.DEFENSE, 1),          recipe("minecraft:dark_oak_planks"), item(Items.DARK_OAK_PLANKS, 64),
            stat(Stat.STRENGTH, 2),         item(Items.DARK_OAK_LOG, 64),    stat(Stat.DEFENSE, 3));

        reg(CollectionType.MANGROVE_WOOD,
            new long[]{ 25, 75, 200, 400, 750, 1_500, 3_000, 7_500, 15_000 },
            item(Items.MANGROVE_LOG, 16),   stat(Stat.HEALTH, 1),           item(Items.MANGROVE_LOG, 32),
            stat(Stat.HEALTH, 1),           recipe("minecraft:mangrove_planks"), item(Items.MANGROVE_PLANKS, 64),
            stat(Stat.HEALTH, 2),           item(Items.MANGROVE_LOG, 64),    stat(Stat.HEALTH, 4));

        reg(CollectionType.CHERRY_WOOD,
            new long[]{ 10, 30, 75, 150, 300, 750, 1_500, 3_000, 7_500 },
            item(Items.CHERRY_LOG, 8),      stat(Stat.LUCK, 1),             item(Items.CHERRY_LOG, 16),
            stat(Stat.HEALTH, 1),           recipe("minecraft:cherry_planks"), item(Items.CHERRY_PLANKS, 64),
            stat(Stat.LUCK, 2),             item(Items.CHERRY_LOG, 32),      stat(Stat.HEALTH, 3));

        reg(CollectionType.BAMBOO_WOOD,
            new long[]{ 50, 150, 300, 600, 1_000, 2_500, 5_000, 10_000, 25_000 },
            item(Items.BAMBOO_BLOCK, 32),   stat(Stat.SWIFTNESS, 1),        item(Items.BAMBOO_BLOCK, 64),
            stat(Stat.SWIFTNESS, 1),        recipe("minecraft:bamboo_planks"), item(Items.BAMBOO_PLANKS, 64),
            stat(Stat.SWIFTNESS, 2),        item(Items.BAMBOO_BLOCK, 64),    stat(Stat.LUCK, 3));

        reg(CollectionType.CRIMSON_WOOD,
            new long[]{ 25, 75, 200, 400, 750, 1_500, 3_000, 7_500, 15_000 },
            item(Items.CRIMSON_STEM, 16),   stat(Stat.STRENGTH, 1),         item(Items.CRIMSON_STEM, 32),
            stat(Stat.TOUGHNESS, 1),        recipe("minecraft:crimson_planks"), item(Items.SHROOMLIGHT, 4),
            stat(Stat.STRENGTH, 2),         item(Items.CRIMSON_STEM, 64),    stat(Stat.TOUGHNESS, 3));

        reg(CollectionType.WARPED_WOOD,
            new long[]{ 25, 75, 200, 400, 750, 1_500, 3_000, 7_500, 15_000 },
            item(Items.WARPED_STEM, 16),    stat(Stat.SWIFTNESS, 1),        item(Items.WARPED_STEM, 32),
            stat(Stat.LUCK, 1),             recipe("minecraft:warped_planks"), item(Items.WARPED_PLANKS, 64),
            stat(Stat.SWIFTNESS, 2),        item(Items.WARPED_STEM, 64),     stat(Stat.LUCK, 3));

        // ── Excavating ────────────────────────────────────────────────────────
        reg(CollectionType.MUD,
            new long[]{ 100, 250, 500, 1_000, 2_500, 5_000, 10_000, 25_000, 50_000 },
            item(Items.MUD, 32),            stat(Stat.HEALTH, 1),           item(Items.MUD, 64),
            stat(Stat.TOUGHNESS, 1),        recipe("minecraft:packed_mud"),  item(Items.PACKED_MUD, 16),
            stat(Stat.HEALTH, 2),           item(Items.MUD_BRICKS, 16),      stat(Stat.TOUGHNESS, 3));

        reg(CollectionType.DIRT,
            new long[]{ 100, 250, 500, 1_000, 2_500, 5_000, 10_000, 25_000, 50_000 },
            item(Items.DIRT, 32),           stat(Stat.TOUGHNESS, 1),        item(Items.DIRT, 64),
            stat(Stat.HEALTH, 1),           recipe("minecraft:mud"),         item(Items.GRASS_BLOCK, 16),
            stat(Stat.TOUGHNESS, 2),        item(Items.COARSE_DIRT, 32),     stat(Stat.HEALTH, 3));

        reg(CollectionType.SAND,
            new long[]{ 100, 250, 500, 1_000, 2_500, 5_000, 10_000, 25_000, 50_000 },
            item(Items.SAND, 32),           stat(Stat.LUCK, 1),             item(Items.SAND, 64),
            stat(Stat.LUCK, 1),             recipe("minecraft:sandstone"),   item(Items.SANDSTONE, 32),
            stat(Stat.LUCK, 2),             item(Items.GLASS, 32),           stat(Stat.LUCK, 3));

        reg(CollectionType.RED_SAND,
            new long[]{ 100, 250, 500, 1_000, 2_500, 5_000, 10_000, 25_000, 50_000 },
            item(Items.RED_SAND, 32),       stat(Stat.LUCK, 1),             item(Items.RED_SAND, 64),
            stat(Stat.LUCK, 1),             recipe("minecraft:red_sandstone"), item(Items.RED_SANDSTONE, 32),
            stat(Stat.LUCK, 2),             item(Items.RED_STAINED_GLASS, 16), stat(Stat.LUCK, 3));

        reg(CollectionType.GRAVEL,
            new long[]{ 100, 250, 500, 1_000, 2_500, 5_000, 10_000, 25_000, 50_000 },
            item(Items.GRAVEL, 32),         stat(Stat.DEFENSE, 1),          item(Items.FLINT, 16),
            stat(Stat.DEFENSE, 1),          recipe("minecraft:flint_and_steel"), item(Items.FLINT, 32),
            stat(Stat.TOUGHNESS, 1),        item(Items.GRAVEL, 64),          stat(Stat.TOUGHNESS, 2));

        reg(CollectionType.CLAY,
            new long[]{ 50, 150, 300, 600, 1_000, 2_500, 5_000, 10_000, 25_000 },
            item(Items.CLAY_BALL, 16),      stat(Stat.DEFENSE, 1),          item(Items.CLAY_BALL, 32),
            stat(Stat.HEALTH, 1),           recipe("minecraft:terracotta"),  item(Items.TERRACOTTA, 16),
            stat(Stat.DEFENSE, 2),          item(Items.CLAY_BALL, 64),       stat(Stat.HEALTH, 3));

        reg(CollectionType.SOUL_SAND,
            new long[]{ 50, 150, 300, 600, 1_000, 2_500, 5_000, 10_000, 25_000 },
            item(Items.SOUL_SAND, 16),      stat(Stat.TOUGHNESS, 1),        item(Items.SOUL_SOIL, 16),
            stat(Stat.SWIFTNESS, 1),        recipe("minecraft:soul_lantern"), item(Items.SOUL_TORCH, 16),
            stat(Stat.TOUGHNESS, 2),        item(Items.SOUL_SAND, 64),       stat(Stat.SWIFTNESS, 3));

        // ── Farming ───────────────────────────────────────────────────────────
        reg(CollectionType.BEETROOT,
            new long[]{ 50, 150, 300, 600, 1_000, 2_500, 5_000, 10_000, 25_000 },
            item(Items.BEETROOT, 16),       stat(Stat.HEALTH, 1),           item(Items.BEETROOT, 32),
            stat(Stat.LUCK, 1),             recipe("minecraft:beetroot_soup"), item(Items.BEETROOT_SOUP, 4),
            stat(Stat.HEALTH, 2),           item(Items.BEETROOT, 64),        stat(Stat.LUCK, 3));

        reg(CollectionType.CACTUS,
            new long[]{ 50, 150, 300, 600, 1_000, 2_500, 5_000, 10_000, 25_000 },
            item(Items.CACTUS, 16),         stat(Stat.DEFENSE, 1),          item(Items.CACTUS, 32),
            stat(Stat.TOUGHNESS, 1),        recipe("minecraft:green_dye"),   item(Items.GREEN_DYE, 16),
            stat(Stat.DEFENSE, 2),          item(Items.CACTUS, 64),          stat(Stat.TOUGHNESS, 3));

        reg(CollectionType.NETHER_WART,
            new long[]{ 25, 75, 200, 400, 750, 1_500, 3_000, 7_500, 15_000 },
            item(Items.NETHER_WART, 8),     stat(Stat.LUCK, 1),             item(Items.NETHER_WART, 16),
            stat(Stat.HEALTH, 1),           recipe("minecraft:brewing_stand"), item(Items.NETHER_WART, 32),
            stat(Stat.LUCK, 2),             item(Items.NETHER_BRICK, 16),    stat(Stat.HEALTH, 3));

        reg(CollectionType.COCOA_BEANS,
            new long[]{ 25, 75, 200, 400, 750, 1_500, 3_000, 7_500, 15_000 },
            item(Items.COCOA_BEANS, 16),    stat(Stat.LUCK, 1),             item(Items.COCOA_BEANS, 32),
            stat(Stat.LUCK, 1),             recipe("minecraft:cookie"),      item(Items.COOKIE, 16),
            stat(Stat.LUCK, 2),             item(Items.BROWN_DYE, 16),       stat(Stat.LUCK, 4));

        reg(CollectionType.SWEET_BERRY,
            new long[]{ 25, 75, 150, 300, 600, 1_500, 3_000, 7_500, 15_000 },
            item(Items.SWEET_BERRIES, 16),  stat(Stat.HEALTH, 1),           item(Items.SWEET_BERRIES, 32),
            stat(Stat.HEALTH, 1),           item(Items.SWEET_BERRIES, 64),   stat(Stat.HEALTH, 2),
            item(Items.SWEET_BERRIES, 64),  stat(Stat.HEALTH, 2),            stat(Stat.LUCK, 3));

        reg(CollectionType.KELP,
            new long[]{ 100, 300, 600, 1_200, 2_500, 5_000, 10_000, 25_000, 50_000 },
            item(Items.KELP, 32),           stat(Stat.HEALTH, 1),           item(Items.DRIED_KELP, 32),
            stat(Stat.SWIFTNESS, 1),        recipe("minecraft:dried_kelp_block"), item(Items.DRIED_KELP_BLOCK, 8),
            stat(Stat.HEALTH, 2),           item(Items.KELP, 64),            stat(Stat.SWIFTNESS, 3));

        reg(CollectionType.MUSHROOM,
            new long[]{ 25, 75, 200, 400, 750, 1_500, 3_000, 7_500, 15_000 },
            item(Items.RED_MUSHROOM, 8),    stat(Stat.HEALTH, 1),           item(Items.BROWN_MUSHROOM, 8),
            stat(Stat.LUCK, 1),             recipe("minecraft:mushroom_stew"), item(Items.MUSHROOM_STEW, 4),
            stat(Stat.HEALTH, 2),           item(Items.RED_MUSHROOM_BLOCK, 4), stat(Stat.LUCK, 3));

        reg(CollectionType.WHEAT,
            new long[]{ 50, 150, 300, 600, 1_000, 2_500, 5_000, 10_000, 25_000 },
            item(Items.WHEAT, 16),          stat(Stat.HEALTH, 1),           item(Items.BREAD, 8),
            stat(Stat.HEALTH, 1),           recipe("minecraft:hay_block"),   item(Items.HAY_BLOCK, 4),
            stat(Stat.HEALTH, 2),           item(Items.WHEAT, 64),           stat(Stat.HEALTH, 4));

        reg(CollectionType.CARROT,
            new long[]{ 50, 150, 300, 600, 1_000, 2_500, 5_000, 10_000, 25_000 },
            item(Items.CARROT, 16),         stat(Stat.HEALTH, 1),           item(Items.GOLDEN_CARROT, 1),
            stat(Stat.LUCK, 1),             recipe("minecraft:golden_carrot"), item(Items.GOLDEN_CARROT, 4),
            stat(Stat.HEALTH, 2),           item(Items.CARROT, 64),          stat(Stat.LUCK, 3));

        reg(CollectionType.POTATO,
            new long[]{ 50, 150, 300, 600, 1_000, 2_500, 5_000, 10_000, 25_000 },
            item(Items.POTATO, 16),         stat(Stat.HEALTH, 1),           item(Items.BAKED_POTATO, 8),
            stat(Stat.HEALTH, 1),           recipe("minecraft:baked_potato"), item(Items.BAKED_POTATO, 16),
            stat(Stat.HEALTH, 2),           item(Items.POTATO, 64),          stat(Stat.HEALTH, 3));

        reg(CollectionType.SUGAR_CANE,
            new long[]{ 50, 150, 300, 600, 1_000, 2_500, 5_000, 10_000, 25_000 },
            item(Items.SUGAR_CANE, 16),     stat(Stat.LUCK, 1),             item(Items.SUGAR, 16),
            stat(Stat.LUCK, 1),             recipe("minecraft:paper"),       item(Items.SUGAR_CANE, 64),
            stat(Stat.LUCK, 2),             item(Items.BOOK, 8),             stat(Stat.LUCK, 4));

        reg(CollectionType.PUMPKIN,
            new long[]{ 25, 75, 200, 400, 750, 1_500, 3_000, 7_500, 15_000 },
            item(Items.PUMPKIN, 4),         stat(Stat.HEALTH, 1),           item(Items.PUMPKIN_PIE, 4),
            stat(Stat.TOUGHNESS, 1),        recipe("minecraft:pumpkin_pie"), item(Items.CARVED_PUMPKIN, 4),
            stat(Stat.HEALTH, 2),           item(Items.PUMPKIN, 32),         stat(Stat.TOUGHNESS, 3));

        reg(CollectionType.MELON,
            new long[]{ 50, 150, 300, 600, 1_000, 2_500, 5_000, 10_000, 25_000 },
            item(Items.MELON_SLICE, 16),    stat(Stat.HEALTH, 1),           item(Items.MELON_SLICE, 32),
            stat(Stat.HEALTH, 1),           recipe("minecraft:melon_block"), item(Items.GLISTERING_MELON_SLICE, 2),
            stat(Stat.HEALTH, 2),           item(Items.MELON_SLICE, 64),     stat(Stat.HEALTH, 4));

        // ── Fishing ───────────────────────────────────────────────────────────
        reg(CollectionType.COD,
            new long[]{ 25, 75, 150, 300, 600, 1_500, 3_000, 7_500, 15_000 },
            item(Items.COD, 8),             stat(Stat.LUCK, 1),             item(Items.COD, 16),
            stat(Stat.LUCK, 1),             recipe("minecraft:cooked_cod"),  item(Items.COOKED_COD, 16),
            stat(Stat.LUCK, 2),             item(Items.COD, 32),             stat(Stat.LUCK, 3));

        reg(CollectionType.SALMON,
            new long[]{ 25, 75, 150, 300, 600, 1_500, 3_000, 7_500, 15_000 },
            item(Items.SALMON, 8),          stat(Stat.LUCK, 1),             item(Items.SALMON, 16),
            stat(Stat.LUCK, 1),             recipe("minecraft:cooked_salmon"), item(Items.COOKED_SALMON, 16),
            stat(Stat.LUCK, 2),             item(Items.SALMON, 32),          stat(Stat.LUCK, 3));

        reg(CollectionType.PUFFERFISH,
            new long[]{ 10, 30, 75, 150, 300, 750, 1_500, 3_000, 7_500 },
            item(Items.PUFFERFISH, 4),      stat(Stat.LUCK, 1),             item(Items.PUFFERFISH, 8),
            stat(Stat.HEALTH, 1),           recipe("minecraft:potion"),      item(Items.PUFFERFISH, 16),
            stat(Stat.LUCK, 2),             item(Items.PUFFERFISH, 32),      stat(Stat.HEALTH, 3));

        reg(CollectionType.TROPICAL_FISH,
            new long[]{ 10, 30, 75, 150, 300, 750, 1_500, 3_000, 7_500 },
            item(Items.TROPICAL_FISH, 4),   stat(Stat.LUCK, 2),             item(Items.TROPICAL_FISH, 8),
            stat(Stat.LUCK, 2),             item(Items.TROPICAL_FISH, 16),   stat(Stat.LUCK, 3),
            item(Items.TROPICAL_FISH, 32),  stat(Stat.LUCK, 4),              stat(Stat.SWIFTNESS, 2));

        reg(CollectionType.LILY_PAD,
            new long[]{ 50, 150, 300, 600, 1_000, 2_500, 5_000, 10_000, 25_000 },
            item(Items.LILY_PAD, 16),       stat(Stat.LUCK, 1),             item(Items.LILY_PAD, 32),
            stat(Stat.HEALTH, 1),           item(Items.LILY_PAD, 64),        stat(Stat.LUCK, 1),
            item(Items.WATER_BUCKET, 1),    stat(Stat.HEALTH, 2),            stat(Stat.LUCK, 2));

        reg(CollectionType.INK_SAC,
            new long[]{ 25, 75, 150, 300, 600, 1_500, 3_000, 7_500, 15_000 },
            item(Items.INK_SAC, 8),         stat(Stat.LUCK, 1),             item(Items.INK_SAC, 16),
            stat(Stat.LUCK, 1),             recipe("minecraft:black_dye"),   item(Items.INK_SAC, 32),
            stat(Stat.LUCK, 2),             item(Items.WRITABLE_BOOK, 2),    stat(Stat.LUCK, 3));

        reg(CollectionType.NAUTILUS_SHELL,
            new long[]{ 5, 15, 30, 75, 150, 300, 750, 1_500, 3_000 },
            item(Items.NAUTILUS_SHELL, 2),  stat(Stat.LUCK, 1),             item(Items.NAUTILUS_SHELL, 4),
            stat(Stat.TOUGHNESS, 1),        recipe("minecraft:conduit"),     item(Items.NAUTILUS_SHELL, 8),
            stat(Stat.LUCK, 2),             item(Items.HEART_OF_THE_SEA, 1), stat(Stat.TOUGHNESS, 3));

        reg(CollectionType.PRISMARINE,
            new long[]{ 25, 75, 150, 300, 600, 1_500, 3_000, 7_500, 15_000 },
            item(Items.PRISMARINE_SHARD, 8), stat(Stat.DEFENSE, 1),         item(Items.PRISMARINE_SHARD, 16),
            stat(Stat.TOUGHNESS, 1),        recipe("minecraft:prismarine"),  item(Items.PRISMARINE_CRYSTALS, 8),
            stat(Stat.DEFENSE, 2),          item(Items.PRISMARINE_BRICK_SLAB, 16), stat(Stat.TOUGHNESS, 3));

        // ── Combat ────────────────────────────────────────────────────────────
        reg(CollectionType.ZOMBIE,
            new long[]{ 50, 150, 300, 600, 1_000, 2_500, 5_000, 10_000, 25_000 },
            item(Items.ROTTEN_FLESH, 16),   stat(Stat.STRENGTH, 1),         item(Items.IRON_SWORD, 1),
            stat(Stat.STRENGTH, 1),         recipe("minecraft:iron_sword"),  item(Items.ROTTEN_FLESH, 64),
            stat(Stat.STRENGTH, 2),         item(Items.IRON_HELMET, 1),      stat(Stat.STRENGTH, 4));

        reg(CollectionType.SKELETON,
            new long[]{ 50, 150, 300, 600, 1_000, 2_500, 5_000, 10_000, 25_000 },
            item(Items.BONE, 16),           stat(Stat.LUCK, 1),             item(Items.BOW, 1),
            stat(Stat.LUCK, 1),             recipe("minecraft:bow"),         item(Items.ARROW, 64),
            stat(Stat.LUCK, 2),             item(Items.BONE_BLOCK, 4),       stat(Stat.LUCK, 4));

        reg(CollectionType.SPIDER,
            new long[]{ 50, 150, 300, 600, 1_000, 2_500, 5_000, 10_000, 25_000 },
            item(Items.STRING, 16),         stat(Stat.SWIFTNESS, 1),        item(Items.STRING, 32),
            stat(Stat.SWIFTNESS, 1),        recipe("minecraft:fishing_rod"), item(Items.SPIDER_EYE, 8),
            stat(Stat.LUCK, 2),             item(Items.STRING, 64),          stat(Stat.SWIFTNESS, 3));

        reg(CollectionType.CREEPER,
            new long[]{ 50, 150, 300, 600, 1_000, 2_500, 5_000, 10_000, 25_000 },
            item(Items.GUNPOWDER, 16),      stat(Stat.STRENGTH, 1),         item(Items.GUNPOWDER, 32),
            stat(Stat.TOUGHNESS, 1),        recipe("minecraft:tnt"),         item(Items.TNT, 4),
            stat(Stat.STRENGTH, 2),         item(Items.GUNPOWDER, 64),       stat(Stat.TOUGHNESS, 3));

        reg(CollectionType.ENDERMAN,
            new long[]{ 10, 30, 75, 150, 300, 600, 1_500, 3_000, 7_500 },
            item(Items.ENDER_PEARL, 2),     stat(Stat.LUCK, 2),             item(Items.ENDER_PEARL, 4),
            stat(Stat.STRENGTH, 2),         recipe("minecraft:ender_eye"),   item(Items.ENDER_PEARL, 8),
            stat(Stat.LUCK, 3),             item(Items.ENDER_PEARL, 16),     stat(Stat.STRENGTH, 4));

        reg(CollectionType.BLAZE,
            new long[]{ 10, 30, 75, 150, 300, 750, 1_500, 3_000, 7_500 },
            item(Items.BLAZE_ROD, 4),       stat(Stat.STRENGTH, 1),         item(Items.BLAZE_ROD, 8),
            stat(Stat.STRENGTH, 1),         recipe("minecraft:blaze_powder"), item(Items.BLAZE_POWDER, 16),
            stat(Stat.STRENGTH, 2),         item(Items.BLAZE_ROD, 32),       stat(Stat.TOUGHNESS, 3));

        reg(CollectionType.GHAST,
            new long[]{ 5, 15, 35, 75, 150, 400, 800, 2_000, 5_000 },
            item(Items.GUNPOWDER, 8),       stat(Stat.TOUGHNESS, 1),        item(Items.GHAST_TEAR, 2),
            stat(Stat.TOUGHNESS, 1),        recipe("minecraft:glass_bottle"), item(Items.GHAST_TEAR, 4),
            stat(Stat.HEALTH, 2),           item(Items.GHAST_TEAR, 8),       stat(Stat.TOUGHNESS, 3));

        reg(CollectionType.WITHER_SKELETON,
            new long[]{ 5, 15, 35, 75, 150, 400, 800, 2_000, 5_000 },
            item(Items.COAL, 8),            stat(Stat.STRENGTH, 1),         item(Items.BONE, 8),
            stat(Stat.STRENGTH, 1),         recipe("minecraft:bone_block"),  item(Items.WITHER_SKELETON_SKULL, 1),
            stat(Stat.STRENGTH, 2),         item(Items.COAL, 32),            stat(Stat.TOUGHNESS, 3));

        reg(CollectionType.PIGLIN,
            new long[]{ 25, 75, 150, 300, 600, 1_500, 3_000, 7_500, 15_000 },
            item(Items.GOLD_NUGGET, 16),    stat(Stat.LUCK, 1),             item(Items.GOLD_INGOT, 4),
            stat(Stat.LUCK, 1),             recipe("minecraft:gold_block"),  item(Items.GOLD_INGOT, 8),
            stat(Stat.LUCK, 2),             item(Items.GOLD_INGOT, 16),      stat(Stat.LUCK, 4));

        reg(CollectionType.WITCH,
            new long[]{ 25, 75, 150, 300, 600, 1_500, 3_000, 7_500, 15_000 },
            item(Items.GLASS_BOTTLE, 8),    stat(Stat.LUCK, 1),             item(Items.SUGAR, 8),
            stat(Stat.HEALTH, 1),           recipe("minecraft:glass_bottle"), item(Items.GLOWSTONE_DUST, 8),
            stat(Stat.LUCK, 2),             item(Items.SPIDER_EYE, 8),       stat(Stat.HEALTH, 3));

        reg(CollectionType.PHANTOM,
            new long[]{ 10, 30, 75, 150, 300, 750, 1_500, 3_000, 7_500 },
            item(Items.PHANTOM_MEMBRANE, 2), stat(Stat.SWIFTNESS, 1),       item(Items.PHANTOM_MEMBRANE, 4),
            stat(Stat.SWIFTNESS, 1),        recipe("minecraft:phantom_membrane"), item(Items.PHANTOM_MEMBRANE, 8),
            stat(Stat.SWIFTNESS, 2),        item(Items.PHANTOM_MEMBRANE, 16), stat(Stat.TOUGHNESS, 3));

        reg(CollectionType.SLIME,
            new long[]{ 25, 75, 200, 400, 750, 1_500, 3_000, 7_500, 15_000 },
            item(Items.SLIME_BALL, 8),      stat(Stat.HEALTH, 1),           item(Items.SLIME_BALL, 16),
            stat(Stat.TOUGHNESS, 1),        recipe("minecraft:slime_block"), item(Items.SLIME_BLOCK, 4),
            stat(Stat.HEALTH, 2),           item(Items.SLIME_BALL, 64),      stat(Stat.TOUGHNESS, 3));

        reg(CollectionType.MAGMA_CUBE,
            new long[]{ 10, 30, 75, 150, 300, 750, 1_500, 3_000, 7_500 },
            item(Items.MAGMA_CREAM, 4),     stat(Stat.STRENGTH, 1),         item(Items.MAGMA_CREAM, 8),
            stat(Stat.TOUGHNESS, 1),        recipe("minecraft:magma_block"), item(Items.MAGMA_BLOCK, 4),
            stat(Stat.STRENGTH, 2),         item(Items.MAGMA_CREAM, 32),     stat(Stat.TOUGHNESS, 3));

        reg(CollectionType.PILLAGER,
            new long[]{ 25, 75, 150, 300, 600, 1_500, 3_000, 7_500, 15_000 },
            item(Items.ARROW, 16),          stat(Stat.STRENGTH, 1),         item(Items.CROSSBOW, 1),
            stat(Stat.LUCK, 1),             recipe("minecraft:crossbow"),    item(Items.ARROW, 64),
            stat(Stat.STRENGTH, 2),         item(Items.TOTEM_OF_UNDYING, 1), stat(Stat.LUCK, 4));
    }

    // ── Block matching ────────────────────────────────────────────────────────

    /**
     * Maps a broken block to a CollectionType, or empty if it belongs to no collection.
     * Ore tags are checked before plain stone so a coal ore never counts as cobblestone.
     * Farming collections only count mature crops.
     */
    public static Optional<CollectionType> fromBlock(BlockState state) {
        Block block = state.getBlock();

        // Ores — check tags first
        if (state.isIn(BlockTags.COAL_ORES))     return Optional.of(CollectionType.COAL);
        if (state.isIn(BlockTags.IRON_ORES))     return Optional.of(CollectionType.IRON);
        if (state.isIn(BlockTags.GOLD_ORES))     return Optional.of(CollectionType.GOLD);
        if (state.isIn(BlockTags.DIAMOND_ORES))  return Optional.of(CollectionType.DIAMOND);
        if (state.isIn(BlockTags.COPPER_ORES))   return Optional.of(CollectionType.COPPER);
        if (state.isIn(BlockTags.LAPIS_ORES))    return Optional.of(CollectionType.LAPIS);
        if (state.isIn(BlockTags.REDSTONE_ORES)) return Optional.of(CollectionType.REDSTONE);
        if (state.isIn(BlockTags.EMERALD_ORES))  return Optional.of(CollectionType.EMERALD);

        // Mining — new types
        if (block == Blocks.NETHER_QUARTZ_ORE)
            return Optional.of(CollectionType.NETHER_QUARTZ);
        if (block == Blocks.ANCIENT_DEBRIS)
            return Optional.of(CollectionType.ANCIENT_DEBRIS);
        if (block == Blocks.OBSIDIAN || block == Blocks.CRYING_OBSIDIAN)
            return Optional.of(CollectionType.OBSIDIAN);

        // Plain stone / cobblestone
        if (block == Blocks.COBBLESTONE    || block == Blocks.STONE
         || block == Blocks.DEEPSLATE      || block == Blocks.COBBLED_DEEPSLATE
         || block == Blocks.GRANITE        || block == Blocks.DIORITE
         || block == Blocks.ANDESITE       || block == Blocks.SMOOTH_STONE
         || block == Blocks.POLISHED_GRANITE || block == Blocks.POLISHED_DIORITE
         || block == Blocks.POLISHED_ANDESITE)
            return Optional.of(CollectionType.COBBLESTONE);

        // Nether wood (before general logs check)
        if (block == Blocks.CRIMSON_STEM   || block == Blocks.STRIPPED_CRIMSON_STEM
         || block == Blocks.CRIMSON_HYPHAE || block == Blocks.STRIPPED_CRIMSON_HYPHAE)
            return Optional.of(CollectionType.CRIMSON_WOOD);
        if (block == Blocks.WARPED_STEM    || block == Blocks.STRIPPED_WARPED_STEM
         || block == Blocks.WARPED_HYPHAE  || block == Blocks.STRIPPED_WARPED_HYPHAE)
            return Optional.of(CollectionType.WARPED_WOOD);

        // Individual overworld wood types
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
        // Bamboo block (crafted) — woodcutting context only; stalk handled in farming below

        // Excavating
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

        // Farming — only mature crops / harvestable plants
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

    /**
     * Maps a picked-up item to a combat CollectionType.
     * Called on item pickup so collection counts items obtained, not kills.
     */
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

    /**
     * Maps a killed entity to a CollectionType, or empty if it belongs to no collection.
     * @deprecated Use fromCombatDrop for item-based tracking instead.
     */
    public static Optional<CollectionType> fromEntity(LivingEntity entity) {
        EntityType<?> type = entity.getType();
        if (type == EntityType.ZOMBIE         || type == EntityType.ZOMBIE_VILLAGER
         || type == EntityType.DROWNED        || type == EntityType.HUSK)
            return Optional.of(CollectionType.ZOMBIE);

        if (type == EntityType.SKELETON       || type == EntityType.STRAY)
            return Optional.of(CollectionType.SKELETON);

        if (type == EntityType.WITHER_SKELETON)
            return Optional.of(CollectionType.WITHER_SKELETON);

        if (type == EntityType.BLAZE)
            return Optional.of(CollectionType.BLAZE);

        if (type == EntityType.GHAST)
            return Optional.of(CollectionType.GHAST);

        if (type == EntityType.PIGLIN       || type == EntityType.PIGLIN_BRUTE
         || type == EntityType.ZOMBIFIED_PIGLIN)
            return Optional.of(CollectionType.PIGLIN);

        if (type == EntityType.SPIDER         || type == EntityType.CAVE_SPIDER)
            return Optional.of(CollectionType.SPIDER);

        if (type == EntityType.CREEPER)
            return Optional.of(CollectionType.CREEPER);

        if (type == EntityType.ENDERMAN)
            return Optional.of(CollectionType.ENDERMAN);

        if (type == EntityType.WITCH)
            return Optional.of(CollectionType.WITCH);

        if (type == EntityType.PHANTOM)
            return Optional.of(CollectionType.PHANTOM);

        if (type == EntityType.SLIME)
            return Optional.of(CollectionType.SLIME);

        if (type == EntityType.MAGMA_CUBE)
            return Optional.of(CollectionType.MAGMA_CUBE);

        if (type == EntityType.PILLAGER  || type == EntityType.VINDICATOR
         || type == EntityType.RAVAGER   || type == EntityType.ILLUSIONER)
            return Optional.of(CollectionType.PILLAGER);

        return Optional.empty();
    }

    // ── Public accessors ──────────────────────────────────────────────────────

    public static List<CollectionTier> getTiers(CollectionType type) {
        return TIERS.getOrDefault(type, List.of());
    }

    // ── Registration helpers ──────────────────────────────────────────────────

    /** Register exactly 9 tiers (one reward each) for a collection. */
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

    private static CollectionReward recipe(String id) {
        return new CollectionReward.RecipeUnlock(Identifier.of(id));
    }
}
