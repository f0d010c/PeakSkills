package com.peakskills.pet;

import net.minecraft.util.Formatting;

public enum PetRarity {
    COMMON(20,    Formatting.WHITE,  "Common"),
    UNCOMMON(40,  Formatting.GREEN,  "Uncommon"),
    RARE(60,      Formatting.AQUA,   "Rare"),
    EPIC(80,      Formatting.DARK_PURPLE, "Epic"),
    LEGENDARY(99, Formatting.GOLD,   "Legendary");

    /** Maximum pet level at this rarity before upgrade is required. */
    public final int levelCap;
    public final Formatting color;
    public final String displayName;

    PetRarity(int levelCap, Formatting color, String displayName) {
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
