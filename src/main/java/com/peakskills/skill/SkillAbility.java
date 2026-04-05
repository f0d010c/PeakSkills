package com.peakskills.skill;

/**
 * A passive ability unlocked at a specific skill level milestone.
 * Pure data — no code logic here. Ability effects are applied in SkillEvents.
 */
public record SkillAbility(
    Skill skill,
    int   minLevel,
    String name,
    String description
) {
    public boolean isUnlocked(int playerLevel) {
        return playerLevel >= minLevel;
    }
}
