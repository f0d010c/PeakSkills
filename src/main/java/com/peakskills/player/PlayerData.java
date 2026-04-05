package com.peakskills.player;

import com.peakskills.collection.CollectionData;
import com.peakskills.pet.PetRoster;
import com.peakskills.skill.Skill;
import com.peakskills.skill.XPTable;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

public class PlayerData {

    private final UUID uuid;
    private final Map<Skill, Long> xp = new EnumMap<>(Skill.class);
    private final PetRoster petRoster = new PetRoster();
    private final CollectionData collections = new CollectionData();
    private double mana;
    private double maxMana = 100;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        for (Skill skill : Skill.values()) {
            xp.put(skill, 0L);
        }
    }

    // --- XP & Levels ---

    public long getXp(Skill skill) {
        return xp.getOrDefault(skill, 0L);
    }

    public int getLevel(Skill skill) {
        return XPTable.levelForXp(getXp(skill));
    }

    /**
     * Adds XP to a skill. Returns true if a level-up occurred.
     */
    public boolean addXp(Skill skill, long amount) {
        int before = getLevel(skill);
        long current = getXp(skill);
        xp.put(skill, Math.max(0, current + amount));
        int after = getLevel(skill);
        return after > before;
    }

    public int getTotalLevel() {
        int total = 0;
        for (Skill skill : Skill.values()) {
            total += getLevel(skill);
        }
        return total;
    }

    // --- Mana ---

    public double getMana() { return mana; }
    public double getMaxMana() { return maxMana; }

    public void setMaxMana(double maxMana) { this.maxMana = maxMana; }

    public boolean consumeMana(double amount) {
        if (mana < amount) return false;
        mana -= amount;
        return true;
    }

    public void regenMana(double amount) {
        mana = Math.min(maxMana, mana + amount);
    }

    // --- Pets ---

    public PetRoster getPetRoster() { return petRoster; }

    // --- Collections ---

    public CollectionData getCollections() { return collections; }

    // --- Serialization helpers ---

    public UUID getUuid() { return uuid; }

    public Map<Skill, Long> getXpMap() { return xp; }
}
