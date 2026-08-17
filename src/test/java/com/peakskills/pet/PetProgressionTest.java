package com.peakskills.pet;

import com.peakskills.MinecraftTestBootstrap;
import com.peakskills.skill.Skill;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PetProgressionTest {

    @BeforeAll
    static void initializeMinecraft() {
        MinecraftTestBootstrap.initializeRegistries();
    }

    @Test
    void xpTablesAreMonotonicAndRespectEveryRarityCap() {
        for (PetRarity rarity : PetRarity.values()) {
            long previous = 0;
            for (int level = 2; level <= rarity.levelCap; level++) {
                long threshold = PetXPTable.xpForLevel(level, rarity);
                assertTrue(threshold > previous, rarity + " level " + level + " must increase XP");
                assertEquals(level, PetXPTable.levelForXp(threshold, rarity));
                previous = threshold;
            }
            assertEquals(rarity.levelCap, PetXPTable.levelForXp(Long.MAX_VALUE, rarity));
            assertEquals(0, PetXPTable.xpToNextLevel(rarity.levelCap, rarity));
        }
    }

    @Test
    void deserializedXpIsClampedAndLiveXpStopsAtCap() {
        PetInstance negative = new PetInstance(
            UUID.randomUUID(), PetType.values()[0], PetRarity.COMMON, Long.MIN_VALUE
        );
        assertEquals(0, negative.getXp());

        PetInstance huge = new PetInstance(
            UUID.randomUUID(), PetType.values()[0], PetRarity.COMMON, Long.MAX_VALUE
        );
        long cap = PetXPTable.xpForLevel(PetRarity.COMMON.levelCap, PetRarity.COMMON);
        assertEquals(cap, huge.getXp());
        huge.addXp(Long.MAX_VALUE);
        assertEquals(cap, huge.getXp());
    }

    @Test
    void upgradeRequiresCapAndResetsProgress() {
        PetInstance pet = new PetInstance(PetType.values()[0]);
        assertFalse(pet.upgrade());

        pet.addXp(Long.MAX_VALUE);
        assertTrue(pet.canUpgrade());
        assertTrue(pet.upgrade());
        assertEquals(PetRarity.UNCOMMON, pet.getRarity());
        assertEquals(0, pet.getXp());
        assertEquals(1, pet.getLevel());
    }

    @Test
    void rosterEnforcesCapacityAndSingleActivePet() {
        PetRoster roster = new PetRoster();
        PetInstance first = null;
        PetInstance last = null;

        for (int i = 0; i < PetRoster.MAX_SLOTS; i++) {
            PetInstance pet = new PetInstance(PetType.values()[i % PetType.values().length]);
            if (first == null) first = pet;
            last = pet;
            assertTrue(roster.addPet(pet));
        }

        assertTrue(roster.isFull());
        assertFalse(roster.addPet(new PetInstance(PetType.values()[0])));

        roster.setActivePet(first.getId());
        assertEquals(first.getId(), roster.getActivePet().orElseThrow().getId());
        roster.setActivePet(last.getId());
        assertEquals(last.getId(), roster.getActivePet().orElseThrow().getId());
        assertEquals(1, roster.getPets().stream().filter(PetInstance::isActive).count());
    }

    @Test
    void nonPositiveSkillXpNeverFeedsAnActivePet() {
        PetRoster roster = new PetRoster();
        PetInstance pet = new PetInstance(PetType.values()[0]);
        roster.addPet(pet);
        roster.setActivePet(pet.getId());

        assertFalse(roster.feedXp(Skill.MINING, 0, 3.0));
        assertFalse(roster.feedXp(Skill.MINING, -100, 3.0));
        assertEquals(0, pet.getXp());
    }
}
