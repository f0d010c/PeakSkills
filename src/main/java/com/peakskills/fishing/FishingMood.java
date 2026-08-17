package com.peakskills.fishing;

import net.minecraft.ChatFormatting;

public enum FishingMood {
    CALM_WATERS("Calm Waters", ChatFormatting.BLUE),
    FEEDING_FRENZY("Feeding Frenzy", ChatFormatting.GREEN),
    TREASURE_RIPPLE("Treasure Ripple", ChatFormatting.GOLD),
    MURKY_WAKE("Murky Wake", ChatFormatting.GRAY),
    ABYSS_STIR("Abyss Stir", ChatFormatting.DARK_PURPLE);

    public final String displayName;
    public final ChatFormatting color;

    FishingMood(String displayName, ChatFormatting color) {
        this.displayName = displayName;
        this.color = color;
    }
}
