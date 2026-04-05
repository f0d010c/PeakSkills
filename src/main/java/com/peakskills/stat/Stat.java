package com.peakskills.stat;

import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Formatting;

/**
 * Shared stats that skills contribute to.
 * Each stat maps to a vanilla Minecraft attribute.
 */
public enum Stat {

    //                    name                 attribute                              raw/lvl  icon  color                      scale
    STRENGTH            ("Strength",         EntityAttributes.ATTACK_DAMAGE,       0.02,   "⚔",  Formatting.RED,        100.0),
    DEFENSE             ("Defense",          EntityAttributes.ARMOR,                0.04,   "❋",  Formatting.WHITE,      100.0),
    TOUGHNESS           ("Toughness",        EntityAttributes.ARMOR_TOUGHNESS,      0.02,   "◈",  Formatting.GRAY,       100.0),
    HEALTH              ("Health",           EntityAttributes.MAX_HEALTH,           0.1,    "❤",  Formatting.DARK_RED,    10.0),
    SWIFTNESS           ("Swiftness",        EntityAttributes.MOVEMENT_SPEED,       0.0004, "⚡",  Formatting.AQUA,     10000.0),
    KNOCKBACK_RESISTANCE("Knockback Resist", EntityAttributes.KNOCKBACK_RESISTANCE, 0.002,  "⚓",  Formatting.DARK_AQUA, 1000.0),
    LUCK                ("Luck",             EntityAttributes.LUCK,                 0.02,   "✦",  Formatting.GOLD,       100.0);

    private final String displayName;
    private final RegistryEntry<EntityAttribute> attribute;
    private final double valuePerLevel;
    private final String icon;
    private final Formatting color;
    /** Multiply raw attribute value by this to get a human-readable display number. */
    private final double displayScale;

    Stat(String displayName, RegistryEntry<EntityAttribute> attribute, double valuePerLevel,
         String icon, Formatting color, double displayScale) {
        this.displayName   = displayName;
        this.attribute     = attribute;
        this.valuePerLevel = valuePerLevel;
        this.icon          = icon;
        this.color         = color;
        this.displayScale  = displayScale;
    }

    public String getDisplayName()                       { return displayName; }
    public RegistryEntry<EntityAttribute> getAttribute() { return attribute; }
    public double getValuePerLevel()                     { return valuePerLevel; }
    public String getIcon()                              { return icon; }
    public Formatting getColor()                         { return color; }
    public double getDisplayScale()                      { return displayScale; }

    /** Convert a raw attribute value to its display value. */
    public double toDisplay(double rawValue)             { return rawValue * displayScale; }
}
