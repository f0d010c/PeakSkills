package com.peakskills.collection;

import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Every collectible resource category.
 * Grouped by skill that produces them.
 */
public enum CollectionType {

    // ── Mining ────────────────────────────────────────────────────────────────
    COBBLESTONE    ("Cobblestone",   Items.COBBLESTONE,      ChatFormatting.GRAY,        "Mining"),
    COAL           ("Coal",          Items.COAL,             ChatFormatting.DARK_GRAY,   "Mining"),
    IRON           ("Iron",          Items.RAW_IRON,         ChatFormatting.WHITE,       "Mining"),
    GOLD           ("Gold",          Items.RAW_GOLD,         ChatFormatting.GOLD,        "Mining"),
    DIAMOND        ("Diamond",       Items.DIAMOND,          ChatFormatting.AQUA,        "Mining"),
    COPPER         ("Copper",        Items.RAW_COPPER,       ChatFormatting.GOLD,        "Mining"),
    LAPIS          ("Lapis Lazuli",  Items.LAPIS_LAZULI,     ChatFormatting.BLUE,        "Mining"),
    REDSTONE       ("Redstone",      Items.REDSTONE,         ChatFormatting.RED,         "Mining"),
    EMERALD        ("Emerald",       Items.EMERALD,          ChatFormatting.GREEN,       "Mining"),
    NETHER_QUARTZ  ("Nether Quartz", Items.QUARTZ,           ChatFormatting.WHITE,       "Mining"),
    OBSIDIAN       ("Obsidian",      Items.OBSIDIAN,         ChatFormatting.DARK_PURPLE, "Mining"),
    ANCIENT_DEBRIS ("Ancient Debris",Items.ANCIENT_DEBRIS,  ChatFormatting.DARK_RED,    "Mining"),

    // ── Woodcutting ───────────────────────────────────────────────────────────
    OAK_WOOD      ("Oak",       Items.OAK_LOG,          ChatFormatting.DARK_GREEN,   "Woodcutting"),
    BIRCH_WOOD    ("Birch",     Items.BIRCH_LOG,        ChatFormatting.YELLOW,       "Woodcutting"),
    SPRUCE_WOOD   ("Spruce",    Items.SPRUCE_LOG,       ChatFormatting.WHITE,        "Woodcutting"),
    JUNGLE_WOOD   ("Jungle",    Items.JUNGLE_LOG,       ChatFormatting.GREEN,        "Woodcutting"),
    ACACIA_WOOD   ("Acacia",    Items.ACACIA_LOG,       ChatFormatting.GOLD,         "Woodcutting"),
    DARK_OAK_WOOD ("Dark Oak",  Items.DARK_OAK_LOG,     ChatFormatting.DARK_GREEN,   "Woodcutting"),
    MANGROVE_WOOD ("Mangrove",  Items.MANGROVE_LOG,     ChatFormatting.RED,          "Woodcutting"),
    CHERRY_WOOD   ("Cherry",    Items.CHERRY_LOG,       ChatFormatting.LIGHT_PURPLE, "Woodcutting"),
    BAMBOO_WOOD   ("Bamboo",    Items.BAMBOO,            ChatFormatting.GREEN,        "Farming"),
    CRIMSON_WOOD  ("Crimson",   Items.CRIMSON_STEM,     ChatFormatting.DARK_RED,     "Woodcutting"),
    WARPED_WOOD   ("Warped",    Items.WARPED_STEM,      ChatFormatting.DARK_AQUA,    "Woodcutting"),

    // ── Excavating ────────────────────────────────────────────────────────────
    DIRT        ("Dirt",            Items.DIRT,             ChatFormatting.YELLOW,      "Excavating"),
    SAND        ("Sand",            Items.SAND,             ChatFormatting.YELLOW,      "Excavating"),
    RED_SAND    ("Red Sand",        Items.RED_SAND,         ChatFormatting.GOLD,        "Excavating"),
    GRAVEL      ("Gravel",          Items.GRAVEL,           ChatFormatting.GRAY,        "Excavating"),
    CLAY        ("Clay",            Items.CLAY_BALL,        ChatFormatting.BLUE,        "Excavating"),
    SOUL_SAND   ("Soul Sand",       Items.SOUL_SAND,        ChatFormatting.DARK_GRAY,   "Excavating"),
    MUD         ("Mud",             Items.MUD,              ChatFormatting.DARK_GRAY,   "Excavating"),

    // ── Farming ───────────────────────────────────────────────────────────────
    WHEAT        ("Wheat",          Items.WHEAT,            ChatFormatting.YELLOW,      "Farming"),
    CARROT       ("Carrot",         Items.CARROT,           ChatFormatting.GOLD,        "Farming"),
    POTATO       ("Potato",         Items.POTATO,           ChatFormatting.YELLOW,      "Farming"),
    BEETROOT     ("Beetroot",       Items.BEETROOT,         ChatFormatting.RED,         "Farming"),
    SUGAR_CANE   ("Sugar Cane",     Items.SUGAR_CANE,       ChatFormatting.GREEN,       "Farming"),
    CACTUS       ("Cactus",         Items.CACTUS,           ChatFormatting.GREEN,       "Farming"),
    PUMPKIN      ("Pumpkin",        Items.PUMPKIN,          ChatFormatting.GOLD,        "Farming"),
    MELON        ("Melon",          Items.MELON_SLICE,      ChatFormatting.GREEN,       "Farming"),
    NETHER_WART  ("Nether Wart",    Items.NETHER_WART,      ChatFormatting.RED,         "Farming"),
    COCOA_BEANS  ("Cocoa Beans",    Items.COCOA_BEANS,      ChatFormatting.DARK_RED,    "Farming"),
    SWEET_BERRY  ("Sweet Berry",    Items.SWEET_BERRIES,    ChatFormatting.RED,         "Farming"),
    KELP         ("Kelp",           Items.KELP,             ChatFormatting.DARK_GREEN,  "Farming"),
    MUSHROOM     ("Mushroom",       Items.RED_MUSHROOM,     ChatFormatting.RED,         "Farming"),

    // ── Fishing ───────────────────────────────────────────────────────────────
    COD           ("Cod",           Items.COD,              ChatFormatting.WHITE,       "Fishing"),
    SALMON        ("Salmon",        Items.SALMON,           ChatFormatting.RED,         "Fishing"),
    PUFFERFISH    ("Pufferfish",    Items.PUFFERFISH,       ChatFormatting.YELLOW,      "Fishing"),
    TROPICAL_FISH ("Tropical Fish", Items.TROPICAL_FISH,    ChatFormatting.GOLD,        "Fishing"),
    LILY_PAD      ("Lily Pad",      Items.LILY_PAD,         ChatFormatting.GREEN,       "Fishing"),
    INK_SAC       ("Ink Sac",       Items.INK_SAC,          ChatFormatting.DARK_GRAY,   "Fishing"),
    NAUTILUS_SHELL("Nautilus Shell",Items.NAUTILUS_SHELL,   ChatFormatting.AQUA,        "Fishing"),
    PRISMARINE    ("Prismarine",    Items.PRISMARINE_SHARD, ChatFormatting.AQUA,        "Fishing"),

    // ── Combat ────────────────────────────────────────────────────────────────
    ZOMBIE          ("Zombie",          Items.ROTTEN_FLESH,          ChatFormatting.DARK_GREEN,  "Combat"),
    SKELETON        ("Skeleton",        Items.BONE,                  ChatFormatting.WHITE,       "Combat"),
    SPIDER          ("Spider",          Items.STRING,                ChatFormatting.GRAY,        "Combat"),
    CREEPER         ("Creeper",         Items.GUNPOWDER,             ChatFormatting.GREEN,       "Combat"),
    ENDERMAN        ("Enderman",        Items.ENDER_PEARL,           ChatFormatting.DARK_PURPLE, "Combat"),
    BLAZE           ("Blaze",           Items.BLAZE_ROD,             ChatFormatting.GOLD,        "Combat"),
    GHAST           ("Ghast",           Items.GHAST_TEAR,            ChatFormatting.WHITE,       "Combat"),
    WITHER_SKELETON ("Wither Skeleton", Items.WITHER_SKELETON_SKULL, ChatFormatting.DARK_GRAY,   "Combat"),
    PIGLIN          ("Piglin",          Items.GOLD_NUGGET,           ChatFormatting.GOLD,        "Combat"),
    WITCH           ("Witch",           Items.GLASS_BOTTLE,          ChatFormatting.DARK_PURPLE, "Combat"),
    PHANTOM         ("Phantom",         Items.PHANTOM_MEMBRANE,      ChatFormatting.DARK_PURPLE, "Combat"),
    SLIME           ("Slime",           Items.SLIME_BALL,            ChatFormatting.GREEN,       "Combat"),
    MAGMA_CUBE      ("Magma Cube",      Items.MAGMA_CREAM,           ChatFormatting.DARK_RED,    "Combat"),
    PILLAGER        ("Pillager",        Items.CROSSBOW,              ChatFormatting.GRAY,        "Combat");

    public final String    displayName;
    public final Item      icon;
    public final ChatFormatting color;
    public final String    category;

    CollectionType(String displayName, Item icon, ChatFormatting color, String category) {
        this.displayName = displayName;
        this.icon        = icon;
        this.color       = color;
        this.category    = category;
    }
}
