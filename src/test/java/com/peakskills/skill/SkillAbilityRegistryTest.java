package com.peakskills.skill;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SkillAbilityRegistryTest {

    @Test
    void flatXpMultiplierMilestonesAreStable() {
        assertEquals(1.0, SkillAbilityRegistry.getFlatXpMultiplier(Skill.FISHING, 49));
        assertEquals(1.25, SkillAbilityRegistry.getFlatXpMultiplier(Skill.FISHING, 50));
        assertEquals(1.50, SkillAbilityRegistry.getFlatXpMultiplier(Skill.FISHING, 99));

        assertEquals(1.0, SkillAbilityRegistry.getFlatXpMultiplier(Skill.SLAYING, 49));
        assertEquals(1.50, SkillAbilityRegistry.getFlatXpMultiplier(Skill.SLAYING, 50));
        assertEquals(2.0, SkillAbilityRegistry.getFlatXpMultiplier(Skill.SLAYING, 99));
    }

    @Test
    void chanceBasedDoubleXpMilestonesAreStable() {
        assertEquals(0.0, SkillAbilityRegistry.getDoubleXpChance(Skill.MINING, 49));
        assertEquals(0.25, SkillAbilityRegistry.getDoubleXpChance(Skill.MINING, 50));
        assertEquals(0.50, SkillAbilityRegistry.getDoubleXpChance(Skill.MINING, 99));

        assertEquals(0.0, SkillAbilityRegistry.getDoubleXpChance(Skill.FISHING, 99));
    }

    @Test
    void petXpMultiplierComesFromTamingMilestones() {
        assertEquals(1.0, SkillAbilityRegistry.getPetXpMultiplier(49));
        assertEquals(2.0, SkillAbilityRegistry.getPetXpMultiplier(50));
        assertEquals(3.0, SkillAbilityRegistry.getPetXpMultiplier(99));
    }
}
