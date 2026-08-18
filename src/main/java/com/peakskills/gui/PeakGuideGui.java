package com.peakskills.gui;

import com.peakskills.crafting.PeakCraftingGui;
import com.peakskills.guide.PeakGuideExtensions;
import com.peakskills.player.PlayerData;
import com.peakskills.player.PlayerDataManager;
import com.peakskills.skill.Skill;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

/** The single player-facing entry point for PeakSkills and installed PeakMod modules. */
public final class PeakGuideGui {

    public static void open(ServerPlayer player) {
        PlayerData data = PlayerDataManager.get(player.getUUID());
        SimpleContainer inventory = new SimpleContainer(54);
        ItemStack background = named(Items.BLACK_STAINED_GLASS_PANE, " ", ChatFormatting.BLACK);
        for (int slot = 0; slot < 54; slot++) inventory.setItem(slot, background.copy());
        Map<Integer, Runnable> handlers = new HashMap<>();

        ItemStack header = named(Items.NETHER_STAR, "Peak Guide", ChatFormatting.GOLD);
        header.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        header.set(DataComponents.LORE, new ItemLore(List.of(
            Component.literal("Everything in one place").withStyle(ChatFormatting.YELLOW),
            Component.literal("Total skill level: " + data.getTotalLevel() + " / "
                + (Skill.values().length * Skill.MAX_LEVEL)).withStyle(ChatFormatting.AQUA),
            Component.literal("Unlock messages tell you where to look next.").withStyle(ChatFormatting.GRAY)
        )));
        inventory.setItem(4, header);

        put(inventory, handlers, 10, guideItem(Items.WRITABLE_BOOK, "Start Here", ChatFormatting.GREEN,
            "Play normally: gathering, fighting, crafting and exploring all grant progress.",
            "Collections count the real items you obtain.",
            "Hover every icon—requirements, rewards and sources are explained.",
            "New unlocks appear in chat with the menu that contains them."), () -> SkillsGui.open(player));
        put(inventory, handlers, 12, guideItem(Items.PLAYER_HEAD, "Profile", ChatFormatting.GOLD,
            "Your total levels, derived stats and skill overview."), () -> ProfileGui.open(player));
        put(inventory, handlers, 14, guideItem(Items.EXPERIENCE_BOTTLE, "Skills", ChatFormatting.AQUA,
            "How to train every skill, level rewards and future unlocks."), () -> SkillsGui.open(player));
        put(inventory, handlers, 16, guideItem(Items.CHEST, "Collections", ChatFormatting.YELLOW,
            "Exact quantities, tier rewards and the next target for every collection."),
            () -> CollectionsGui.open(player));
        put(inventory, handlers, 20, guideItem(Items.FISHING_ROD, "Fishing & Watersense", ChatFormatting.AQUA,
            "Catches, required levels and depths, water moods, biomes and discoveries."),
            () -> FishingJournalGui.open(player));
        put(inventory, handlers, 22, guideItem(Items.WOLF_SPAWN_EGG, "Pets", ChatFormatting.LIGHT_PURPLE,
            "Obtaining pets, affinities, active bonuses, leveling and rarity upgrades."),
            () -> PetMenuGui.open(player));
        put(inventory, handlers, 24, guideItem(Items.CRAFTING_TABLE, "PeakSkills Recipes", ChatFormatting.GREEN,
            "Visual 3×3 recipes with live ingredient counts and secure crafting."),
            () -> PeakCraftingGui.open(player));
        put(inventory, handlers, 26, guideItem(Items.COMPARATOR, "Settings", ChatFormatting.RED,
            "Change how PeakSkills communicates and displays information."),
            () -> SettingsGui.open(player));

        for (PeakGuideExtensions.GuideEntry entry : PeakGuideExtensions.entries()) {
            ItemStack extensionIcon = entry.icon().get();
            if (extensionIcon == null || extensionIcon.isEmpty()) continue;
            inventory.setItem(entry.slot(), extensionIcon.copy());
            handlers.put(entry.slot(), () -> entry.action().accept(player));
        }

        com.peakskills.gui.core.LegacyContainerGui.open(player,
            Component.literal("Peak Guide").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
            inventory, handlers);
    }

    private static void put(SimpleContainer inventory, Map<Integer, Runnable> handlers, int slot,
                            ItemStack stack, Runnable action) {
        inventory.setItem(slot, stack);
        handlers.put(slot, action);
    }

    private static ItemStack guideItem(Item item, String name, ChatFormatting color, String... lines) {
        ItemStack stack = named(item, name, color);
        stack.set(DataComponents.LORE, new ItemLore(java.util.Arrays.stream(lines)
            .map(line -> (Component) Component.literal(line).withStyle(ChatFormatting.GRAY)).toList()));
        return stack;
    }

    private static ItemStack named(Item item, String name, ChatFormatting color) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME,
            Component.literal(name).withStyle(color, ChatFormatting.BOLD));
        return stack;
    }

    private PeakGuideGui() {}
}
