package com.peakskills.pet;

import net.minecraft.ChatFormatting;

public enum PetRarity {
    COMMON(20,    ChatFormatting.WHITE,  "Common"),
    UNCOMMON(40,  ChatFormatting.GREEN,  "Uncommon"),
    RARE(60,      ChatFormatting.AQUA,   "Rare"),
    EPIC(80,      ChatFormatting.DARK_PURPLE, "Epic"),
    LEGENDARY(99, ChatFormatting.GOLD,   "Legendary");

    /** Maximum pet level at this rarity before upgrade is required. */
    public final int levelCap;
    public final ChatFormatting color;
    public final String displayName;

    PetRarity(int levelCap, ChatFormatting color, String displayName) {
        this.levelCap = levelCap;
        this.color = color;
        this.displayName = displayName;
    }

    public PetRarity next() {
        PetRarity[] vals = values();
        int idx = ordinal();
        return idx + 1 < vals.length ? vals[idx + 1] : null;
    }

    public boolean isMax() {
        return this == LEGENDARY;
    }
}
