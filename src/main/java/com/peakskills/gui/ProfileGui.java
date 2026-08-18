package com.peakskills.gui;

import com.peakskills.pet.PetAbility;
import com.peakskills.pet.PetAbilityRegistry;
import com.peakskills.player.PlayerData;
import com.peakskills.player.PlayerDataManager;
import com.peakskills.skill.Skill;
import com.peakskills.skill.XPTable;
import com.peakskills.stat.Stat;
import com.peakskills.stat.StatRegistry;
import java.util.ArrayList;
import java.util.EnumMap;
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

/**
 * /profile [player] — a compact read-only overview of a player's skills and stats.
 *
 * Layout (54 slots, 6 rows):
 *   Row 0 │ bg bg bg bg  HEADER  bg bg bg bg
 *   Row 1 │ HEALTH  bg  Mine WC  Exc Farm Fish  bg bg
 *   Row 2 │ STRENGTH bg  Def Slay Rng  Smth Cook  bg bg
 *   Row 3 │ DEFENSE  bg  Cft Ench Alch Agil Tame Trad bg
 *   Row 4 │ LUCK     bg  bg  bg  TOTAL  bg  bg  bg bg
 *   Row 5 │ bg bg bg bg  SKILLS_BTN  bg bg bg bg
 */
public class ProfileGui {

    private static final Skill[] GATHERING = { Skill.MINING, Skill.WOODCUTTING, Skill.EXCAVATING, Skill.FARMING,   Skill.FISHING };
    private static final Skill[] COMBAT    = { Skill.DEFENSE, Skill.SLAYING,    Skill.RANGED,     Skill.SMITHING,  Skill.COOKING };
    private static final Skill[] MASTERY   = { Skill.CRAFTING, Skill.ENCHANTING, Skill.ALCHEMY,   Skill.AGILITY,   Skill.TAMING,  Skill.TRADING };

    private static final int[] GATHER_SLOTS  = { 11, 12, 13, 14, 15 };
    private static final int[] COMBAT_SLOTS  = { 20, 21, 22, 23, 24 };
    private static final int[] MASTERY_SLOTS = { 29, 30, 31, 32, 33, 34 };

    private static final int TOTAL_SKILLS = 16;

    public static void open(ServerPlayer viewer) {
        open(viewer, PlayerDataManager.get(viewer.getUUID()), viewer.getName().getString());
    }

    public static void open(ServerPlayer viewer, PlayerData data, String ownerName) {
        SimpleContainer inv = new SimpleContainer(54);
        populate(inv, data, ownerName);

        Map<Integer, Runnable> handlers = new HashMap<>();

        // Skill slots open the detail page
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

        // Slot 49 — open full skills GUI
        handlers.put(49, () -> SkillsGui.open(viewer, data, ownerName));
        handlers.put(48, () -> PeakGuideGui.open(viewer));

        com.peakskills.gui.core.LegacyContainerGui.open(viewer,
            Component.literal("✦ " + ownerName + "'s Profile").withStyle(ChatFormatting.GOLD), inv, handlers);
    }

    // ── Build ─────────────────────────────────────────────────────────────────

    private static void populate(SimpleContainer inv, PlayerData data, String ownerName) {
        ItemStack bg = pane(Items.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) inv.setItem(i, bg.copy());

        // ── Header (slot 4) ──────────────────────────────────────────────────
        int total = data.getTotalLevel();
        int max   = TOTAL_SKILLS * Skill.MAX_LEVEL;
        ItemStack header = new ItemStack(Items.PLAYER_HEAD);
        header.set(DataComponents.CUSTOM_NAME,
            Component.literal("✦ " + ownerName).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        header.set(DataComponents.LORE, new ItemLore(List.of(
            Component.literal("  Total Level  ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(total + " / " + max).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)),
            Component.literal("  " + bar(total, max, 20)).withStyle(ChatFormatting.AQUA)
                .append(Component.literal("  " + String.format("%.1f%%", total * 100.0 / max)).withStyle(ChatFormatting.WHITE)),
            Component.literal("  ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬").withStyle(ChatFormatting.DARK_GRAY),
            Component.literal("  Click a skill to view its details").withStyle(ChatFormatting.DARK_GRAY)
        )));
        inv.setItem(4, header);

        // ── Stat summary items (left column: 9, 18, 27, 36) ─────────────────
        Map<Stat, Double> statTotals = computeStatTotals(data);

        inv.setItem(9,  statItem(Items.APPLE, "Health & Sustain", ChatFormatting.RED,
            statTotals, Stat.HEALTH));
        inv.setItem(18, statItem(Items.IRON_SWORD, "Offense", ChatFormatting.YELLOW,
            statTotals, Stat.STRENGTH, Stat.SWIFTNESS));
        inv.setItem(27, statItem(Items.SHIELD, "Defense", ChatFormatting.WHITE,
            statTotals, Stat.DEFENSE, Stat.TOUGHNESS, Stat.KNOCKBACK_RESISTANCE));
        inv.setItem(36, statItem(Items.RABBIT_FOOT, "Luck", ChatFormatting.GREEN,
            statTotals, Stat.LUCK));

        // ── Skill icons ──────────────────────────────────────────────────────
        for (int i = 0; i < GATHERING.length; i++)
            inv.setItem(GATHER_SLOTS[i],  compactSkillIcon(GATHERING[i], data));
        for (int i = 0; i < COMBAT.length; i++)
            inv.setItem(COMBAT_SLOTS[i],  compactSkillIcon(COMBAT[i],    data));
        for (int i = 0; i < MASTERY.length; i++)
            inv.setItem(MASTERY_SLOTS[i], compactSkillIcon(MASTERY[i],   data));

        // ── Total level (slot 40) ────────────────────────────────────────────
        ItemStack totalItem = new ItemStack(Items.EXPERIENCE_BOTTLE);
        totalItem.set(DataComponents.CUSTOM_NAME,
            Component.literal("Total Level  " + total + " / " + max)
                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        totalItem.set(DataComponents.LORE, new ItemLore(List.of(
            Component.literal("  ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬").withStyle(ChatFormatting.DARK_GRAY),
            Component.literal("  " + bar(total, max, 20) + "  ").withStyle(ChatFormatting.AQUA)
                .append(Component.literal(String.format("%.1f%%", total * 100.0 / max)).withStyle(ChatFormatting.WHITE)),
            Component.literal("  ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬").withStyle(ChatFormatting.DARK_GRAY)
        )));
        inv.setItem(40, totalItem);

        // ── Skills GUI button (slot 49) ──────────────────────────────────────
        ItemStack skillsBtn = new ItemStack(Items.BOOK);
        skillsBtn.set(DataComponents.CUSTOM_NAME,
            Component.literal("View Full Skills").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
        skillsBtn.set(DataComponents.LORE, new ItemLore(List.of(
            Component.literal("  Opens the detailed skills menu").withStyle(ChatFormatting.DARK_GRAY),
            Component.literal("  with XP progress and bonuses").withStyle(ChatFormatting.DARK_GRAY)
        )));
        inv.setItem(49, skillsBtn);

        ItemStack guide = new ItemStack(Items.KNOWLEDGE_BOOK);
        guide.set(DataComponents.CUSTOM_NAME,
            Component.literal("Peak Guide").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        inv.setItem(48, guide);
    }

    // ── Stat helpers ──────────────────────────────────────────────────────────

    private static Map<Stat, Double> computeStatTotals(PlayerData data) {
        Map<Stat, Double> totals = new EnumMap<>(Stat.class);

        // Skill contributions
        for (var src : StatRegistry.SOURCES) {
            int level = data.getLevel(src.skill());
            totals.merge(src.stat(), src.compute(level), Double::sum);
        }

        // Collection bonuses
        data.getCollections().computeStatBonuses()
            .forEach((stat, value) -> totals.merge(stat, value, Double::sum));

        // Active pet stat bonuses
        data.getPetRoster().getActivePet().ifPresent(pet ->
            PetAbilityRegistry.getAbilities(pet.getType()).stream()
                .filter(a -> a.type == PetAbility.Type.STAT_BONUS)
                .forEach(a -> totals.merge(a.stat, a.compute(pet.getLevel(), pet.getRarity()), Double::sum))
        );

        // Include vanilla base max health (20) so the displayed value matches the actual health bar
        totals.merge(Stat.HEALTH, 20.0, Double::sum);

        return totals;
    }

    private static ItemStack statItem(Item item, String title, ChatFormatting color,
                                      Map<Stat, Double> totals, Stat... stats) {
        ItemStack s = new ItemStack(item);
        s.set(DataComponents.CUSTOM_NAME,
            Component.literal(title).withStyle(color, ChatFormatting.BOLD));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.literal("  ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬").withStyle(ChatFormatting.DARK_GRAY));
        for (Stat stat : stats) {
            double raw = totals.getOrDefault(stat, 0.0);
            double display = stat.toDisplay(raw);
            String num = display < 10
                ? String.format("%.1f", display).replaceAll("0+$", "").replaceAll("\\.$", "")
                : String.format("%.0f", display);
            lore.add(Component.literal("  " + stat.getIcon() + " ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(num).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD))
                .append(Component.literal("  " + stat.getDisplayName()).withStyle(ChatFormatting.GRAY)));
        }
        lore.add(Component.literal("  ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬").withStyle(ChatFormatting.DARK_GRAY));
        s.set(DataComponents.LORE, new ItemLore(lore));
        return s;
    }

    // ── Compact skill icon ────────────────────────────────────────────────────

    private static ItemStack compactSkillIcon(Skill skill, PlayerData data) {
        int level  = data.getLevel(skill);
        long xp    = data.getXp(skill);
        long floor = XPTable.xpForLevel(level);
        long ceil  = level < Skill.MAX_LEVEL ? XPTable.xpForLevel(level + 1) : floor;
        long span  = Math.max(1, ceil - floor);
        boolean maxed = level >= Skill.MAX_LEVEL;

        ItemStack stack = new ItemStack(iconFor(skill));
        if (level > 1) stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);

        stack.set(DataComponents.CUSTOM_NAME,
            Component.literal(skill.getDisplayName()).withStyle(nameColor(skill), ChatFormatting.BOLD));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.literal("  ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬").withStyle(ChatFormatting.DARK_GRAY));
        lore.add(Component.literal("  Level  ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(String.valueOf(level)).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD))
            .append(Component.literal(" / " + Skill.MAX_LEVEL).withStyle(ChatFormatting.DARK_GRAY)));

        if (maxed) {
            lore.add(Component.literal("  ✦ MAX LEVEL ✦").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        } else {
            long progress = xp - floor;
            float pct = (float) progress / span * 100f;
            lore.add(Component.literal("  " + bar(progress, span, 14) + "  ").withStyle(ChatFormatting.GREEN)
                .append(Component.literal(String.format("%.1f%%", pct)).withStyle(ChatFormatting.GRAY)));
        }

        lore.add(Component.literal("  ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬").withStyle(ChatFormatting.DARK_GRAY));
        lore.add(Component.literal("  Click to view details").withStyle(ChatFormatting.DARK_GRAY));
        stack.set(DataComponents.LORE, new ItemLore(lore));
        return stack;
    }

    // ── Shared helpers ────────────────────────────────────────────────────────

    private static ItemStack pane(Item item, String name) {
        ItemStack s = new ItemStack(item);
        s.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        return s;
    }

    private static String bar(long value, long max, int length) {
        int filled = max > 0 ? (int) Math.min(length, (value * length) / max) : length;
        return "█".repeat(filled) + "░".repeat(length - filled);
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
