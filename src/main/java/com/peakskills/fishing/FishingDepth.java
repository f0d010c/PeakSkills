package com.peakskills.fishing;

import net.minecraft.ChatFormatting;

public enum FishingDepth {
    SHALLOW("Shallow", ChatFormatting.WHITE),
    RIVERBED("Riverbed", ChatFormatting.GREEN),
    DEEP_WATER("Deep Water", ChatFormatting.AQUA),
    ABYSSAL("Abyssal", ChatFormatting.DARK_PURPLE),
    ANCIENT("Ancient", ChatFormatting.GOLD);

    public final String displayName;
    public final ChatFormatting color;

    FishingDepth(String displayName, ChatFormatting color) {
        this.displayName = displayName;
        this.color = color;
    }
}
