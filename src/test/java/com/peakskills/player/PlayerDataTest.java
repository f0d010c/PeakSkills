package com.peakskills.player;

import com.peakskills.MinecraftTestBootstrap;
import com.peakskills.skill.Skill;
import com.peakskills.skill.XPTable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerDataTest {

    @BeforeAll
    static void initializeMinecraft() {
        MinecraftTestBootstrap.initializeRegistries();
    }

    @Test
    void xpCannotUnderflowAndPositiveOverflowSaturates() {
        PlayerData data = new PlayerData(UUID.randomUUID());

        data.addXp(Skill.MINING, -1);
        assertEquals(0, data.getXp(Skill.MINING));

        data.addXp(Skill.MINING, Long.MAX_VALUE - 5);
        data.addXp(Skill.MINING, 10);
        assertEquals(Long.MAX_VALUE, data.getXp(Skill.MINING));
        assertEquals(Skill.MAX_LEVEL, data.getLevel(Skill.MINING));
    }

    @Test
    void levelUpResultOnlyReportsUpwardCrossings() {
        PlayerData data = new PlayerData(UUID.randomUUID());
        long levelTwo = XPTable.xpForLevel(2);

        assertFalse(data.addXp(Skill.FARMING, levelTwo - 1));
        assertTrue(data.addXp(Skill.FARMING, 1));
        assertFalse(data.addXp(Skill.FARMING, 1));
        assertFalse(data.addXp(Skill.FARMING, -data.getXp(Skill.FARMING)));
        assertEquals(1, data.getLevel(Skill.FARMING));
    }

    @Test
    void totalLevelIncludesEverySkill() {
        PlayerData data = new PlayerData(UUID.randomUUID());
        assertEquals(Skill.values().length, data.getTotalLevel());

        data.addXp(Skill.MINING, XPTable.xpForLevel(10));
        data.addXp(Skill.FISHING, XPTable.xpForLevel(20));
        assertEquals(Skill.values().length - 2 + 10 + 20, data.getTotalLevel());
    }

    @Test
    void persistedVersionIsClampedToSupportedRange() {
        PlayerData data = new PlayerData(UUID.randomUUID());
        data.setDataVersion(Integer.MIN_VALUE);
        assertEquals(1, data.getDataVersion());
        data.setDataVersion(Integer.MAX_VALUE);
        assertEquals(PlayerData.DATA_VERSION, data.getDataVersion());
    }
}
