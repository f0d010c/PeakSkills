package com.peakskills.collection;

import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Formatting;

/**
 * Every collectible resource category.
 * Grouped by skill that produces them.
 */
public enum CollectionType {

    // ── Mining ────────────────────────────────────────────────────────────────
    COBBLESTONE    ("Cobblestone",   Items.COBBLESTONE,      Formatting.GRAY,        "Mining"),
    COAL           ("Coal",          Items.COAL,             Formatting.DARK_GRAY,   "Mining"),
    IRON           ("Iron",          Items.RAW_IRON,         Formatting.WHITE,       "Mining"),
    GOLD           ("Gold",          Items.RAW_GOLD,         Formatting.GOLD,        "Mining"),
    DIAMOND        ("Diamond",       Items.DIAMOND,          Formatting.AQUA,        "Mining"),
    COPPER         ("Copper",        Items.RAW_COPPER,       Formatting.GOLD,        "Mining"),
    LAPIS          ("Lapis Lazuli",  Items.LAPIS_LAZULI,     Formatting.BLUE,        "Mining"),
    REDSTONE       ("Redstone",      Items.REDSTONE,         Formatting.RED,         "Mining"),
    EMERALD        ("Emerald",       Items.EMERALD,          Formatting.GREEN,       "Mining"),
    NETHER_QUARTZ  ("Nether Quartz", Items.QUARTZ,           Formatting.WHITE,       "Mining"),
    OBSIDIAN       ("Obsidian",      Items.OBSIDIAN,         Formatting.DARK_PURPLE, "Mining"),
    ANCIENT_DEBRIS ("Ancient Debris",Items.ANCIENT_DEBRIS,  Formatting.DARK_RED,    "Mining"),

    // ── Woodcutting ───────────────────────────────────────────────────────────
    OAK_WOOD      ("Oak",       Items.OAK_LOG,          Formatting.DARK_GREEN,   "Woodcutting"),
    BIRCH_WOOD    ("Birch",     Items.BIRCH_LOG,        Formatting.YELLOW,       "Woodcutting"),
    SPRUCE_WOOD   ("Spruce",    Items.SPRUCE_LOG,       Formatting.WHITE,        "Woodcutting"),
    JUNGLE_WOOD   ("Jungle",    Items.JUNGLE_LOG,       Formatting.GREEN,        "Woodcutting"),
    ACACIA_WOOD   ("Acacia",    Items.ACACIA_LOG,       Formatting.GOLD,         "Woodcutting"),
    DARK_OAK_WOOD ("Dark Oak",  Items.DARK_OAK_LOG,     Formatting.DARK_GREEN,   "Woodcutting"),
    MANGROVE_WOOD ("Mangrove",  Items.MANGROVE_LOG,     Formatting.RED,          "Woodcutting"),
    CHERRY_WOOD   ("Cherry",    Items.CHERRY_LOG,       Formatting.LIGHT_PURPLE, "Woodcutting"),
    BAMBOO_WOOD   ("Bamboo",    Items.BAMBOO,            Formatting.GREEN,        "Farming"),
    CRIMSON_WOOD  ("Crimson",   Items.CRIMSON_STEM,     Formatting.DARK_RED,     "Woodcutting"),
    WARPED_WOOD   ("Warped",    Items.WARPED_STEM,      Formatting.DARK_AQUA,    "Woodcutting"),

    // ── Excavating ────────────────────────────────────────────────────────────
    DIRT        ("Dirt",            Items.DIRT,             Formatting.YELLOW,      "Excavating"),
    SAND        ("Sand",            Items.SAND,             Formatting.YELLOW,      "Excavating"),
    RED_SAND    ("Red Sand",        Items.RED_SAND,         Formatting.GOLD,        "Excavating"),
    GRAVEL      ("Gravel",          Items.GRAVEL,           Formatting.GRAY,        "Excavating"),
    CLAY        ("Clay",            Items.CLAY_BALL,        Formatting.BLUE,        "Excavating"),
    SOUL_SAND   ("Soul Sand",       Items.SOUL_SAND,        Formatting.DARK_GRAY,   "Excavating"),
    MUD         ("Mud",             Items.MUD,              Formatting.DARK_GRAY,   "Excavating"),

    // ── Farming ───────────────────────────────────────────────────────────────
    WHEAT        ("Wheat",          Items.WHEAT,            Formatting.YELLOW,      "Farming"),
    CARROT       ("Carrot",         Items.CARROT,           Formatting.GOLD,        "Farming"),
    POTATO       ("Potato",         Items.POTATO,           Formatting.YELLOW,      "Farming"),
    BEETROOT     ("Beetroot",       Items.BEETROOT,         Formatting.RED,         "Farming"),
    SUGAR_CANE   ("Sugar Cane",     Items.SUGAR_CANE,       Formatting.GREEN,       "Farming"),
    CACTUS       ("Cactus",         Items.CACTUS,           Formatting.GREEN,       "Farming"),
    PUMPKIN      ("Pumpkin",        Items.PUMPKIN,          Formatting.GOLD,        "Farming"),
    MELON        ("Melon",          Items.MELON_SLICE,      Formatting.GREEN,       "Farming"),
    NETHER_WART  ("Nether Wart",    Items.NETHER_WART,      Formatting.RED,         "Farming"),
    COCOA_BEANS  ("Cocoa Beans",    Items.COCOA_BEANS,      Formatting.DARK_RED,    "Farming"),
    SWEET_BERRY  ("Sweet Berry",    Items.SWEET_BERRIES,    Formatting.RED,         "Farming"),
    KELP         ("Kelp",           Items.KELP,             Formatting.DARK_GREEN,  "Farming"),
    MUSHROOM     ("Mushroom",       Items.RED_MUSHROOM,     Formatting.RED,         "Farming"),

    // ── Fishing ───────────────────────────────────────────────────────────────
    COD           ("Cod",           Items.COD,              Formatting.WHITE,       "Fishing"),
    SALMON        ("Salmon",        Items.SALMON,           Formatting.RED,         "Fishing"),
    PUFFERFISH    ("Pufferfish",    Items.PUFFERFISH,       Formatting.YELLOW,      "Fishing"),
    TROPICAL_FISH ("Tropical Fish", Items.TROPICAL_FISH,    Formatting.GOLD,        "Fishing"),
    LILY_PAD      ("Lily Pad",      Items.LILY_PAD,         Formatting.GREEN,       "Fishing"),
    INK_SAC       ("Ink Sac",       Items.INK_SAC,          Formatting.DARK_GRAY,   "Fishing"),
    NAUTILUS_SHELL("Nautilus Shell",Items.NAUTILUS_SHELL,   Formatting.AQUA,        "Fishing"),
    PRISMARINE    ("Prismarine",    Items.PRISMARINE_SHARD, Formatting.AQUA,        "Fishing"),

    // ── Combat ────────────────────────────────────────────────────────────────
    ZOMBIE          ("Zombie",          Items.ROTTEN_FLESH,          Formatting.DARK_GREEN,  "Combat"),
    SKELETON        ("Skeleton",        Items.BONE,                  Formatting.WHITE,       "Combat"),
    SPIDER          ("Spider",          Items.STRING,                Formatting.GRAY,        "Combat"),
    CREEPER         ("Creeper",         Items.GUNPOWDER,             Formatting.GREEN,       "Combat"),
    ENDERMAN        ("Enderman",        Items.ENDER_PEARL,           Formatting.DARK_PURPLE, "Combat"),
    BLAZE           ("Blaze",           Items.BLAZE_ROD,             Formatting.GOLD,        "Combat"),
    GHAST           ("Ghast",           Items.GHAST_TEAR,            Formatting.WHITE,       "Combat"),
    WITHER_SKELETON ("Wither Skeleton", Items.WITHER_SKELETON_SKULL, Formatting.DARK_GRAY,   "Combat"),
    PIGLIN          ("Piglin",          Items.GOLD_NUGGET,           Formatting.GOLD,        "Combat"),
    WITCH           ("Witch",           Items.GLASS_BOTTLE,          Formatting.DARK_PURPLE, "Combat"),
    PHANTOM         ("Phantom",         Items.PHANTOM_MEMBRANE,      Formatting.DARK_PURPLE, "Combat"),
    SLIME           ("Slime",           Items.SLIME_BALL,            Formatting.GREEN,       "Combat"),
    MAGMA_CUBE      ("Magma Cube",      Items.MAGMA_CREAM,           Formatting.DARK_RED,    "Combat"),
    PILLAGER        ("Pillager",        Items.CROSSBOW,              Formatting.GRAY,        "Combat");

    public final String    displayName;
    public final Item      icon;
    public final Formatting color;
    public final String    category;

    CollectionType(String displayName, Item icon, Formatting color, String category) {
        this.displayName = displayName;
        this.icon        = icon;
        this.color       = color;
        this.category    = category;
    }
}
