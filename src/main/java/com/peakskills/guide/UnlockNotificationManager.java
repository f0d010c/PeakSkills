package com.peakskills.guide;

import com.peakskills.fishing.FishingLootTable;
import com.peakskills.skill.Skill;
import com.peakskills.skill.SkillAbilityRegistry;
import com.peakskills.skill.SkillGearUnlockRegistry;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Announces newly crossed content gates once, at the moment they become available. */
public final class UnlockNotificationManager {

    public static void announce(ServerPlayer player, Skill skill, int from, int to) {
        List<Component> unlocks = new ArrayList<>();

        SkillAbilityRegistry.getAbilities(skill).stream()
            .filter(ability -> ability.minLevel() > from && ability.minLevel() <= to)
            .forEach(ability -> unlocks.add(line("Ability", ability.name(), ability.description(), "peak skills")));

        SkillGearUnlockRegistry.crossed(skill, from, to).forEach(unlock ->
            unlocks.add(line("Equipment", unlock.name(), "You can now use this gear.", "peak skills")));

        if (skill == Skill.FISHING) {
            FishingLootTable.entries().stream()
                .filter(entry -> entry.minLevel() > from && entry.minLevel() <= to)
                .forEach(entry -> unlocks.add(line("Catch", entry.displayName(),
                    "Requires " + entry.minDepth().displayName + " water.", "peak fishing")));
        }

        PeakGuideExtensions.crossedUnlocks(skill, from, to).forEach(unlock ->
            unlocks.add(line("Content", unlock.name(), unlock.description(), unlock.guideCommand())));

        if (unlocks.isEmpty()) return;
        player.sendSystemMessage(Component.literal(" NEW UNLOCKS").withStyle(
            ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        unlocks.forEach(component -> player.sendSystemMessage(component, false));

        String command = skill == Skill.FISHING ? "peak fishing" : "peak skills";
        String label = skill == Skill.FISHING ? "[Open Fishing Guide]" : "[Open Skill Guide]";
        player.sendSystemMessage(Component.literal("  " + label)
            .withStyle(style -> style.withColor(ChatFormatting.AQUA).withUnderlined(true)
                .withClickEvent(new ClickEvent.RunCommand(command))), false);
    }

    private static Component line(String type, String name, String description, String command) {
        Component result = Component.literal("  ✦ " + type + ": ").withStyle(ChatFormatting.YELLOW)
            .append(Component.literal(name).withStyle(style -> style.withColor(ChatFormatting.GREEN)
                .withBold(true).withUnderlined(true)
                .withClickEvent(new ClickEvent.RunCommand(command))));
        if (description != null && !description.isBlank()) {
            result = result.copy().append(Component.literal(" — " + description).withStyle(ChatFormatting.GRAY));
        }
        return result;
    }

    private UnlockNotificationManager() {}
}
