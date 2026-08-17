package com.peakskills.stat;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Shared stats that skills contribute to.
 * Each stat maps to a vanilla Minecraft attribute.
 */
public enum Stat {

    //                    name                 attribute                              raw/lvl  icon  color                      scale
    STRENGTH            ("Strength",         Attributes.ATTACK_DAMAGE,       0.02,   "⚔",  ChatFormatting.RED,        100.0),
    DEFENSE             ("Defense",          Attributes.ARMOR,                0.04,   "❋",  ChatFormatting.WHITE,      100.0),
    TOUGHNESS           ("Toughness",        Attributes.ARMOR_TOUGHNESS,      0.02,   "◈",  ChatFormatting.GRAY,       100.0),
    HEALTH              ("Health",           Attributes.MAX_HEALTH,           0.1,    "❤",  ChatFormatting.DARK_RED,    10.0),
    SWIFTNESS           ("Swiftness",        Attributes.MOVEMENT_SPEED,       0.0004, "⚡",  ChatFormatting.AQUA,     10000.0),
    KNOCKBACK_RESISTANCE("Knockback Resist", Attributes.KNOCKBACK_RESISTANCE, 0.002,  "⚓",  ChatFormatting.DARK_AQUA, 1000.0),
    LUCK                ("Luck",             Attributes.LUCK,                 0.02,   "✦",  ChatFormatting.GOLD,       100.0);

    private final String displayName;
    private final Holder<Attribute> attribute;
    private final double valuePerLevel;
    private final String icon;
    private final ChatFormatting color;
    /** Multiply raw attribute value by this to get a human-readable display number. */
    private final double displayScale;

    Stat(String displayName, Holder<Attribute> attribute, double valuePerLevel,
         String icon, ChatFormatting color, double displayScale) {
        this.displayName   = displayName;
        this.attribute     = attribute;
        this.valuePerLevel = valuePerLevel;
        this.icon          = icon;
        this.color         = color;
        this.displayScale  = displayScale;
    }

    public String getDisplayName()                       { return displayName; }
    public Holder<Attribute> getAttribute() { return attribute; }
    public double getValuePerLevel()                     { return valuePerLevel; }
    public String getIcon()                              { return icon; }
    public ChatFormatting getColor()                         { return color; }
    public double getDisplayScale()                      { return displayScale; }

    /** Convert a raw attribute value to its display value. */
    public double toDisplay(double rawValue)             { return rawValue * displayScale; }
}
