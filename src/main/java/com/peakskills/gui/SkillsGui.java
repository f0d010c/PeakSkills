package com.peakskills.gui;

import com.peakskills.pet.PetAbility;
import com.peakskills.pet.PetAbilityRegistry;
import com.peakskills.player.PlayerData;
import com.peakskills.player.PlayerDataManager;
import com.peakskills.skill.Skill;
import com.peakskills.skill.XPTable;
import com.peakskills.stat.SkillStatSource;
import com.peakskills.stat.Stat;
import com.peakskills.stat.StatRegistry;
import java.util.ArrayList;
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

public class SkillsGui {

    // ── Layout ────────────────────────────────────────────────────────────────
    //
    //  Row 0 │ bg  bg  bg  bg  TITLE  bg  bg  bg  bg
    //  Row 1 │ [GATHERING]  bg  Mine WC  Exc Farm Fish  bg  bg
    //  Row 2 │ [COMBAT  ]   bg  Def  Slay Rng  Smth Cook  bg  bg
    //  Row 3 │ [MASTERY ]   bg  Cft  Ench Alch Agil Tame  bg  bg
    //  Row 4 │ bg  bg  bg  bg  TOTAL  bg  bg  bg  bg
    //  Row 5 │ bg  bg  bg  bg  bg     bg  bg  bg  bg
    //
    private static final Skill[] GATHERING = { Skill.MINING, Skill.WOODCUTTING, Skill.EXCAVATING, Skill.FARMING,   Skill.FISHING };
    private static final Skill[] COMBAT    = { Skill.DEFENSE, Skill.SLAYING,    Skill.RANGED,     Skill.SMITHING,  Skill.COOKING };
    private static final Skill[] MASTERY   = { Skill.CRAFTING, Skill.ENCHANTING, Skill.ALCHEMY,   Skill.AGILITY,   Skill.TAMING,  Skill.TRADING };

    private static final int[] GATHER_SLOTS  = { 11, 12, 13, 14, 15 };
    private static final int[] COMBAT_SLOTS  = { 20, 21, 22, 23, 24 };
    private static final int[] MASTERY_SLOTS = { 29, 30, 31, 32, 33, 34 };

    private static final int TOTAL_SKILLS = 16;

    // ── Open ──────────────────────────────────────────────────────────────────

    public static void open(ServerPlayer viewer) {
        open(viewer, PlayerDataManager.get(viewer.getUUID()), viewer.getName().getString());
    }

    public static void open(ServerPlayer viewer, PlayerData data, String ownerName) {
        SimpleContainer inv = new SimpleContainer(54);
        populate(inv, data, ownerName);

        // Build click handlers: each skill slot opens its detail page
        Map<Integer, Runnable> handlers = new HashMap<>();
        for (int i = 0; i < GATHERING.length; i++) {
            Skill skill = GATHERING[i];
            handlers.put(GATHER_SLOTS[i], () -> SkillDetailGui.open(viewer, data, skill));
        }
        for (int i = 0; i < COMBAT.length; i++) {
            Skill skill = COMBAT[i];
            handlers.put(COMBAT_SLOTS[i], () -> SkillDetailGui.open(viewer, data, skill));
        }
        for (int i = 0; i < MASTERY.length; i++) {
            Skill skill = MASTERY[i];
            handlers.put(MASTERY_SLOTS[i], () -> SkillDetailGui.open(viewer, data, skill));
        }

        // Bottom shortcut row.
        handlers.put(38, () -> ProfileGui.open(viewer, PlayerDataManager.get(viewer.getUUID()), ownerName));
        handlers.put(39, () -> CollectionsGui.open(viewer, PlayerDataManager.get(viewer.getUUID())));
        handlers.put(40, () -> PetMenuGui.open(viewer));
        handlers.put(41, () -> SettingsGui.open(viewer));
        handlers.put(42, () -> populate(inv, PlayerDataManager.get(viewer.getUUID()), ownerName));

        com.peakskills.gui.core.LegacyContainerGui.open(viewer,
            Component.literal("Your Skills").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), inv, handlers);
    }

    // ── Build ─────────────────────────────────────────────────────────────────

    private static void populate(SimpleContainer inv, PlayerData data, String ownerName) {
        ItemStack border = pane(Items.GRAY_STAINED_GLASS_PANE, " ");
        ItemStack bg = pane(Items.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) inv.setItem(i, bg.copy());
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, border.copy());
            inv.setItem(45 + i, border.copy());
        }
        for (int row = 1; row <= 4; row++) {
            inv.setItem(row * 9, border.copy());
            inv.setItem(row * 9 + 8, border.copy());
        }

        // Header title (slot 4) — diamond sword as the "skills" icon
        ItemStack header = new ItemStack(Items.NETHER_STAR);
        header.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        header.set(DataComponents.CUSTOM_NAME,
            Component.literal(ownerName + "'s Skills").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        int total = data.getTotalLevel();
        int max   = TOTAL_SKILLS * Skill.MAX_LEVEL;
        header.set(DataComponents.LORE, new ItemLore(List.of(
            Component.literal("  Total Level: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(total + " / " + max).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)),
            Component.literal("  " + bar(total, max, 20)).withStyle(ChatFormatting.AQUA)
                .append(Component.literal("  " + String.format("%.1f%%", total * 100.0 / max)).withStyle(ChatFormatting.WHITE)),
            separator(),
            Component.literal("  Click a skill to view its leveling path").withStyle(ChatFormatting.DARK_GRAY)
        )));
        inv.setItem(4, header);

        // Skill icons
        for (int i = 0; i < GATHERING.length; i++)
            inv.setItem(GATHER_SLOTS[i],  skillIcon(GATHERING[i], data));
        for (int i = 0; i < COMBAT.length; i++)
            inv.setItem(COMBAT_SLOTS[i],  skillIcon(COMBAT[i],    data));
        for (int i = 0; i < MASTERY.length; i++)
            inv.setItem(MASTERY_SLOTS[i], skillIcon(MASTERY[i],   data));

        ItemStack profile = new ItemStack(Items.PLAYER_HEAD);
        profile.set(DataComponents.CUSTOM_NAME,
            Component.literal("Profile").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        profile.set(DataComponents.LORE, new ItemLore(List.of(
            Component.literal("  View stats and skill totals").withStyle(ChatFormatting.DARK_GRAY)
        )));
        inv.setItem(38, profile);

        ItemStack collections = new ItemStack(Items.CHEST);
        collections.set(DataComponents.CUSTOM_NAME,
            Component.literal("Collections").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
        collections.set(DataComponents.LORE, new ItemLore(List.of(
            Component.literal("  View collection progress").withStyle(ChatFormatting.DARK_GRAY)
        )));
        inv.setItem(39, collections);

        ItemStack pets = new ItemStack(Items.BONE);
        pets.set(DataComponents.CUSTOM_NAME,
            Component.literal("Pets").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
        pets.set(DataComponents.LORE, new ItemLore(List.of(
            Component.literal("  Open your pet roster").withStyle(ChatFormatting.DARK_GRAY)
        )));
        inv.setItem(40, pets);

        ItemStack settings = new ItemStack(Items.COMPARATOR);
        settings.set(DataComponents.CUSTOM_NAME,
            Component.literal("Settings").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        settings.set(DataComponents.LORE, new ItemLore(List.of(
            Component.literal("  Open your PeakSkills settings").withStyle(ChatFormatting.DARK_GRAY)
        )));
        inv.setItem(41, settings);

        ItemStack refresh = new ItemStack(Items.ARROW);
        refresh.set(DataComponents.CUSTOM_NAME,
            Component.literal("Refresh").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
        refresh.set(DataComponents.LORE, new ItemLore(List.of(
            Component.literal("  Click to reload your skill data").withStyle(ChatFormatting.DARK_GRAY)
        )));
        inv.setItem(42, refresh);
    }

    // ── Skill icon ────────────────────────────────────────────────────────────

    private static ItemStack skillIcon(Skill skill, PlayerData data) {
        int  level    = data.getLevel(skill);
        long xp       = data.getXp(skill);
        long floor    = XPTable.xpForLevel(level);
        long ceil     = level < Skill.MAX_LEVEL ? XPTable.xpForLevel(level + 1) : floor;
        long span     = Math.max(1, ceil - floor);
        long progress = xp - floor;
        boolean maxed = level >= Skill.MAX_LEVEL;

        ItemStack stack = new ItemStack(iconFor(skill));

        // Enchant glow once any real leveling has happened
        if (level > 1) stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);

        // Skill name
        stack.set(DataComponents.CUSTOM_NAME,
            Component.literal(skill.getDisplayName()).withStyle(nameColor(skill), ChatFormatting.BOLD));

        // Lore
        List<Component> lore = new ArrayList<>();
        lore.add(separator());

        // Description
        lore.add(Component.literal("  " + skill.getDescription()).withStyle(ChatFormatting.GRAY));
        lore.add(Component.empty());

        // Level
        lore.add(Component.literal("  Level  ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(String.valueOf(level)).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD))
            .append(Component.literal(" / " + Skill.MAX_LEVEL).withStyle(ChatFormatting.DARK_GRAY)));

        // Progress bar
        if (maxed) {
            lore.add(Component.literal("  ✦ MAX LEVEL ✦").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        } else {
            float pct = (float) progress / span * 100f;
            lore.add(Component.literal("  " + bar(progress, span, 16) + "  ").withStyle(ChatFormatting.GREEN)
                .append(Component.literal(String.format("%.1f%%", pct)).withStyle(ChatFormatting.GRAY)));
            lore.add(Component.literal(String.format("  %,d / %,d XP", progress, span))
                .withStyle(ChatFormatting.DARK_GRAY));
        }

        // Stat bonuses
        List<SkillStatSource> sources = StatRegistry.SOURCES.stream()
            .filter(s -> s.skill() == skill)
            .toList();

        if (!sources.isEmpty()) {
            lore.add(Component.empty());
            lore.add(Component.literal("  Stat Bonuses:").withStyle(ChatFormatting.YELLOW));
            for (SkillStatSource src : sources) {
                double current = src.compute(level);
                lore.add(
                    Component.literal("   +" + statValue(src.stat(), current) + "  ").withStyle(ChatFormatting.GREEN)
                        .append(Component.literal("(" + statValue(src.stat(), src.valuePerLevel()) + "/lvl)")
                            .withStyle(ChatFormatting.DARK_GRAY)));
            }
        } else {
            lore.add(Component.empty());
            lore.add(Component.literal("  No stat bonuses").withStyle(ChatFormatting.DARK_GRAY));
        }

        // Active pet XP bonus for this skill
        data.getPetRoster().getActivePet().ifPresent(pet -> {
            double xpBonus = PetAbilityRegistry.getAbilities(pet.getType()).stream()
                .filter(a -> a.type == PetAbility.Type.XP_BONUS && a.skill == skill)
                .mapToDouble(a -> a.compute(pet.getLevel(), pet.getRarity()))
                .sum();
            if (xpBonus > 0) {
                lore.add(Component.empty());
                lore.add(Component.literal("  Active Pet Bonus:").withStyle(ChatFormatting.GOLD));
                lore.add(Component.literal("   +" + String.format("%.1f%%", xpBonus * 100) + " XP  ")
                    .withStyle(ChatFormatting.GREEN)
                    .append(Component.literal("(" + pet.getRarity().displayName + " " + pet.getType().displayName + ")")
                        .withStyle(pet.getRarity().color)));
            }
        });

        lore.add(separator());
        stack.set(DataComponents.LORE, new ItemLore(lore));
        return stack;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static ItemStack pane(Item item, String name) {
        ItemStack s = new ItemStack(item);
        s.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        return s;
    }

    private static ItemStack sectionItem(Item item, String name, ChatFormatting color, String subtitle) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME,
            Component.literal(name).withStyle(color, ChatFormatting.BOLD));
        stack.set(DataComponents.LORE, new ItemLore(List.of(
            Component.literal("  " + subtitle).withStyle(ChatFormatting.DARK_GRAY)
        )));
        return stack;
    }

    private static Component separator() {
        return Component.literal("  ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬").withStyle(ChatFormatting.DARK_GRAY);
    }

    private static String bar(long value, long max, int length) {
        int filled = max > 0 ? (int) Math.min(length, (value * length) / max) : length;
        return "█".repeat(filled) + "░".repeat(length - filled);
    }

    private static String statValue(Stat stat, double rawValue) {
        double v = stat.toDisplay(rawValue);
        String num = v < 10
            ? String.format("%.1f", v).replaceAll("0+$", "").replaceAll("\\.$", "")
            : String.format("%.0f", v);
        return stat.getIcon() + " " + num + " " + stat.getDisplayName();
    }

    private static ChatFormatting nameColor(Skill skill) {
        return switch (skill) {
            case MINING      -> ChatFormatting.GRAY;
            case WOODCUTTING -> ChatFormatting.GREEN;
            case EXCAVATING  -> ChatFormatting.YELLOW;
            case FARMING     -> ChatFormatting.DARK_GREEN;
            case FISHING     -> ChatFormatting.AQUA;
            case DEFENSE     -> ChatFormatting.WHITE;
            case SLAYING     -> ChatFormatting.RED;
            case RANGED      -> ChatFormatting.GOLD;
            case ENCHANTING  -> ChatFormatting.LIGHT_PURPLE;
            case ALCHEMY     -> ChatFormatting.DARK_PURPLE;
            case SMITHING    -> ChatFormatting.DARK_GRAY;
            case COOKING     -> ChatFormatting.YELLOW;
            case CRAFTING    -> ChatFormatting.WHITE;
            case AGILITY     -> ChatFormatting.BLUE;
            case TAMING      -> ChatFormatting.GREEN;
            case TRADING     -> ChatFormatting.GREEN;
        };
    }

    private static Item iconFor(Skill skill) {
        return switch (skill) {
            case MINING      -> Items.IRON_PICKAXE;
            case WOODCUTTING -> Items.IRON_AXE;
            case EXCAVATING  -> Items.IRON_SHOVEL;
            case FARMING     -> Items.IRON_HOE;
            case FISHING     -> Items.FISHING_ROD;
            case DEFENSE     -> Items.SHIELD;
            case SLAYING     -> Items.IRON_SWORD;
            case RANGED      -> Items.BOW;
            case ENCHANTING  -> Items.ENCHANTED_BOOK;
            case ALCHEMY     -> Items.POTION;
            case SMITHING    -> Items.ANVIL;
            case COOKING     -> Items.FURNACE;
            case CRAFTING    -> Items.CRAFTING_TABLE;
            case AGILITY     -> Items.FEATHER;
            case TAMING      -> Items.LEAD;
            case TRADING     -> Items.EMERALD;
        };
    }
}
