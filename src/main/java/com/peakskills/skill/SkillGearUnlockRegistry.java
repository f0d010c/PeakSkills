package com.peakskills.skill;

import java.util.List;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** Single source of truth for vanilla equipment gates shown in menus and announcements. */
public final class SkillGearUnlockRegistry {
    private static final List<GearUnlock> UNLOCKS = List.of(
        unlock(Skill.MINING, 20, Items.IRON_PICKAXE, "Iron Pickaxe"),
        unlock(Skill.MINING, 50, Items.DIAMOND_PICKAXE, "Diamond Pickaxe"),
        unlock(Skill.MINING, 70, Items.NETHERITE_PICKAXE, "Netherite Pickaxe"),
        unlock(Skill.WOODCUTTING, 20, Items.IRON_AXE, "Iron Axe"),
        unlock(Skill.WOODCUTTING, 50, Items.DIAMOND_AXE, "Diamond Axe"),
        unlock(Skill.WOODCUTTING, 70, Items.NETHERITE_AXE, "Netherite Axe"),
        unlock(Skill.EXCAVATING, 20, Items.IRON_SHOVEL, "Iron Shovel"),
        unlock(Skill.EXCAVATING, 50, Items.DIAMOND_SHOVEL, "Diamond Shovel"),
        unlock(Skill.EXCAVATING, 70, Items.NETHERITE_SHOVEL, "Netherite Shovel"),
        unlock(Skill.FARMING, 20, Items.IRON_HOE, "Iron Hoe"),
        unlock(Skill.FARMING, 50, Items.DIAMOND_HOE, "Diamond Hoe"),
        unlock(Skill.FARMING, 70, Items.NETHERITE_HOE, "Netherite Hoe"),
        unlock(Skill.SLAYING, 20, Items.IRON_SWORD, "Iron Sword"),
        unlock(Skill.SLAYING, 50, Items.DIAMOND_SWORD, "Diamond Sword"),
        unlock(Skill.SLAYING, 70, Items.NETHERITE_SWORD, "Netherite Sword"),
        unlock(Skill.DEFENSE, 20, Items.IRON_CHESTPLATE, "Iron Armor Set"),
        unlock(Skill.DEFENSE, 50, Items.DIAMOND_CHESTPLATE, "Diamond Armor Set"),
        unlock(Skill.DEFENSE, 70, Items.NETHERITE_CHESTPLATE, "Netherite Armor Set")
    );

    public static GearUnlock at(Skill skill, int level) {
        return UNLOCKS.stream().filter(unlock -> unlock.skill() == skill && unlock.level() == level)
            .findFirst().orElse(null);
    }

    public static List<GearUnlock> crossed(Skill skill, int from, int to) {
        return UNLOCKS.stream().filter(unlock -> unlock.skill() == skill
            && unlock.level() > from && unlock.level() <= to).toList();
    }

    private static GearUnlock unlock(Skill skill, int level, Item icon, String name) {
        return new GearUnlock(skill, level, icon, name);
    }

    public record GearUnlock(Skill skill, int level, Item icon, String name) {}
    private SkillGearUnlockRegistry() {}
}
