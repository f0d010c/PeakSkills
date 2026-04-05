package com.peakskills.pet;

import java.util.UUID;

/**
 * A player-owned pet instance. Each pet has its own XP and rarity independent of others.
 */
public class PetInstance {

    private final UUID id;         // unique ID for this pet instance
    private final PetType type;
    private PetRarity rarity;
    private long xp;
    private boolean active;        // is this the currently deployed pet?

    public PetInstance(PetType type) {
        this.id = UUID.randomUUID();
        this.type = type;
        this.rarity = PetRarity.COMMON;
        this.xp = 0;
        this.active = false;
    }

    // Constructor for deserialization
    public PetInstance(UUID id, PetType type, PetRarity rarity, long xp) {
        this.id = id;
        this.type = type;
        this.rarity = rarity;
        this.xp = xp;
        this.active = false;
    }

    // --- XP & Level ---

    public int getLevel() {
        return PetXPTable.levelForXp(xp, rarity);
    }

    /**
     * Adds XP to pet. Returns true if leveled up.
     */
    public boolean addXp(long amount) {
        int before = getLevel();
        xp += amount;
        // Clamp XP at the level cap for current rarity
        long max = PetXPTable.xpForLevel(rarity.levelCap, rarity);
        if (xp > max) xp = max;
        return getLevel() > before;
    }

    public boolean isAtLevelCap() {
        return getLevel() >= rarity.levelCap;
    }

    // --- Rarity upgrade ---

    public boolean canUpgrade() {
        return isAtLevelCap() && !rarity.isMax();
    }

    /**
     * Upgrades rarity. Resets XP to 0. Returns true if successful.
     */
    public boolean upgrade() {
        if (!canUpgrade()) return false;
        rarity = rarity.next();
        xp = 0;
        return true;
    }

    /** Force-sets rarity without level cap check (used when hatching eggs at a specific rarity). */
    public void forceUpgradeRarity() {
        PetRarity next = rarity.next();
        if (next != null) { rarity = next; xp = 0; }
    }

    // --- Getters ---

    public UUID getId() { return id; }
    public PetType getType() { return type; }
    public PetRarity getRarity() { return rarity; }
    public long getXp() { return xp; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
