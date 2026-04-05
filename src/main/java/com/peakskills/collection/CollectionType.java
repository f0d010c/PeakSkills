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
    COBBLESTONE ("Cobblestone",  Items.COBBLESTONE,   Formatting.GRAY,        "Mining"),
    COAL        ("Coal",         Items.COAL,          Formatting.DARK_GRAY,   "Mining"),
    IRON        ("Iron",         Items.RAW_IRON,      Formatting.WHITE,       "Mining"),
    GOLD        ("Gold",         Items.RAW_GOLD,      Formatting.GOLD,        "Mining"),
    DIAMOND     ("Diamond",      Items.DIAMOND,       Formatting.AQUA,        "Mining"),
    COPPER      ("Copper",       Items.RAW_COPPER,    Formatting.GOLD,        "Mining"),
    LAPIS       ("Lapis Lazuli", Items.LAPIS_LAZULI,  Formatting.BLUE,        "Mining"),
    REDSTONE    ("Redstone",     Items.REDSTONE,      Formatting.RED,         "Mining"),
    EMERALD     ("Emerald",      Items.EMERALD,       Formatting.GREEN,       "Mining"),

    // ── Woodcutting ───────────────────────────────────────────────────────────
    WOOD        ("Wood",            Items.OAK_LOG,          Formatting.DARK_GREEN,  "Woodcutting"),
    NETHER_WOOD ("Nether Wood",     Items.CRIMSON_STEM,     Formatting.DARK_RED,    "Woodcutting"),

    // ── Excavating ────────────────────────────────────────────────────────────
    DIRT        ("Dirt",            Items.DIRT,             Formatting.YELLOW,      "Excavating"),
    SAND        ("Sand",            Items.SAND,             Formatting.YELLOW,      "Excavating"),
    GRAVEL      ("Gravel",          Items.GRAVEL,           Formatting.GRAY,        "Excavating"),
    CLAY        ("Clay",            Items.CLAY_BALL,        Formatting.BLUE,        "Excavating"),
    SOUL_SAND   ("Soul Sand",       Items.SOUL_SAND,        Formatting.DARK_GRAY,   "Excavating"),

    // ── Farming ───────────────────────────────────────────────────────────────
    WHEAT       ("Wheat",           Items.WHEAT,            Formatting.YELLOW,      "Farming"),
    CARROT      ("Carrot",          Items.CARROT,           Formatting.GOLD,        "Farming"),
    POTATO      ("Potato",          Items.POTATO,           Formatting.YELLOW,      "Farming"),
    SUGAR_CANE  ("Sugar Cane",      Items.SUGAR_CANE,       Formatting.GREEN,       "Farming"),
    PUMPKIN     ("Pumpkin",         Items.PUMPKIN,          Formatting.GOLD,        "Farming"),
    MELON       ("Melon",           Items.MELON_SLICE,      Formatting.GREEN,       "Farming"),

    // ── Fishing ───────────────────────────────────────────────────────────────
    COD         ("Cod",             Items.COD,              Formatting.WHITE,       "Fishing"),
    SALMON      ("Salmon",          Items.SALMON,           Formatting.RED,         "Fishing"),
    PUFFERFISH  ("Pufferfish",      Items.PUFFERFISH,       Formatting.YELLOW,      "Fishing"),
    TROPICAL_FISH("Tropical Fish",  Items.TROPICAL_FISH,    Formatting.GOLD,        "Fishing"),

    // ── Combat ────────────────────────────────────────────────────────────────
    ZOMBIE          ("Zombie",          Items.ROTTEN_FLESH,         Formatting.DARK_GREEN,  "Combat"),
    SKELETON        ("Skeleton",        Items.BONE,                 Formatting.WHITE,       "Combat"),
    SPIDER          ("Spider",          Items.STRING,               Formatting.GRAY,        "Combat"),
    CREEPER         ("Creeper",         Items.GUNPOWDER,            Formatting.GREEN,       "Combat"),
    ENDERMAN        ("Enderman",        Items.ENDER_PEARL,          Formatting.DARK_PURPLE, "Combat"),
    BLAZE           ("Blaze",           Items.BLAZE_ROD,            Formatting.GOLD,        "Combat"),
    GHAST           ("Ghast",           Items.GHAST_TEAR,           Formatting.WHITE,       "Combat"),
    WITHER_SKELETON ("Wither Skeleton", Items.WITHER_SKELETON_SKULL,Formatting.DARK_GRAY,   "Combat"),
    PIGLIN          ("Piglin",          Items.GOLD_NUGGET,          Formatting.GOLD,        "Combat");

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
