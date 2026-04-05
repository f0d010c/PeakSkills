package com.peakskills.stat;

import com.peakskills.skill.Skill;

/**
 * Defines how much a skill contributes to a stat per level.
 */
public record SkillStatSource(Skill skill, Stat stat, double valuePerLevel) {

    /** Compute the total value this source contributes at the given skill level. */
    public double compute(int skillLevel) {
        return skillLevel * valuePerLevel;
    }
}
