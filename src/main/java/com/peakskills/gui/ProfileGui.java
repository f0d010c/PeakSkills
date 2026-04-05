package com.peakskills.gui;

import com.peakskills.pet.PetAbility;
import com.peakskills.pet.PetAbilityRegistry;
import com.peakskills.player.PlayerData;
import com.peakskills.player.PlayerDataManager;
import com.peakskills.skill.Skill;
import com.peakskills.skill.XPTable;
import com.peakskills.stat.Stat;
import com.peakskills.stat.StatRegistry;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public static void open(ServerPlayerEntity viewer) {
        open(viewer, PlayerDataManager.get(viewer.getUuid()), viewer.getName().getString());
    }

    public static void open(ServerPlayerEntity viewer, PlayerData data, String ownerName) {
        SimpleInventory inv = new SimpleInventory(54);
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

        viewer.openHandledScreen(new SimpleNamedScreenHandlerFactory(
            (syncId, playerInv, p) -> new SkillsScreenHandler(syncId, playerInv, inv, handlers),
            Text.literal("✦ " + ownerName + "'s Profile").formatted(Formatting.GOLD)
        ));
    }

    // ── Build ─────────────────────────────────────────────────────────────────

    private static void populate(SimpleInventory inv, PlayerData data, String ownerName) {
        ItemStack bg = pane(Items.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) inv.setStack(i, bg.copy());

        // ── Header (slot 4) ──────────────────────────────────────────────────
        int total = data.getTotalLevel();
        int max   = TOTAL_SKILLS * Skill.MAX_LEVEL;
        ItemStack header = new ItemStack(Items.PLAYER_HEAD);
        header.set(DataComponentTypes.CUSTOM_NAME,
            Text.literal("✦ " + ownerName).formatted(Formatting.GOLD, Formatting.BOLD));
        header.set(DataComponentTypes.LORE, new LoreComponent(List.of(
            Text.literal("  Total Level  ").formatted(Formatting.GRAY)
                .append(Text.literal(total + " / " + max).formatted(Formatting.AQUA, Formatting.BOLD)),
            Text.literal("  " + bar(total, max, 20)).formatted(Formatting.AQUA)
                .append(Text.literal("  " + String.format("%.1f%%", total * 100.0 / max)).formatted(Formatting.WHITE)),
            Text.literal("  ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬").formatted(Formatting.DARK_GRAY),
            Text.literal("  Click a skill to view its details").formatted(Formatting.DARK_GRAY)
        )));
        inv.setStack(4, header);

        // ── Stat summary items (left column: 9, 18, 27, 36) ─────────────────
        Map<Stat, Double> statTotals = computeStatTotals(data);

        inv.setStack(9,  statItem(Items.APPLE, "Health", Formatting.RED,
            statTotals, Stat.HEALTH));
        inv.setStack(18, statItem(Items.SHIELD, "Defense", Formatting.WHITE,
            statTotals, Stat.DEFENSE));

        // ── Skill icons ──────────────────────────────────────────────────────
        for (int i = 0; i < GATHERING.length; i++)
            inv.setStack(GATHER_SLOTS[i],  compactSkillIcon(GATHERING[i], data));
        for (int i = 0; i < COMBAT.length; i++)
            inv.setStack(COMBAT_SLOTS[i],  compactSkillIcon(COMBAT[i],    data));
        for (int i = 0; i < MASTERY.length; i++)
            inv.setStack(MASTERY_SLOTS[i], compactSkillIcon(MASTERY[i],   data));

        // ── Total level (slot 40) ────────────────────────────────────────────
        ItemStack totalItem = new ItemStack(Items.EXPERIENCE_BOTTLE);
        totalItem.set(DataComponentTypes.CUSTOM_NAME,
            Text.literal("Total Level  " + total + " / " + max)
                .formatted(Formatting.AQUA, Formatting.BOLD));
        totalItem.set(DataComponentTypes.LORE, new LoreComponent(List.of(
            Text.literal("  ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬").formatted(Formatting.DARK_GRAY),
            Text.literal("  " + bar(total, max, 20) + "  ").formatted(Formatting.AQUA)
                .append(Text.literal(String.format("%.1f%%", total * 100.0 / max)).formatted(Formatting.WHITE)),
            Text.literal("  ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬").formatted(Formatting.DARK_GRAY)
        )));
        inv.setStack(40, totalItem);

        // ── Skills GUI button (slot 49) ──────────────────────────────────────
        ItemStack skillsBtn = new ItemStack(Items.BOOK);
        skillsBtn.set(DataComponentTypes.CUSTOM_NAME,
            Text.literal("View Full Skills").formatted(Formatting.YELLOW, Formatting.BOLD));
        skillsBtn.set(DataComponentTypes.LORE, new LoreComponent(List.of(
            Text.literal("  Opens the detailed skills menu").formatted(Formatting.DARK_GRAY),
            Text.literal("  with XP progress and bonuses").formatted(Formatting.DARK_GRAY)
        )));
        inv.setStack(49, skillsBtn);
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

    private static ItemStack statItem(Item item, String title, Formatting color,
                                      Map<Stat, Double> totals, Stat... stats) {
        ItemStack s = new ItemStack(item);
        s.set(DataComponentTypes.CUSTOM_NAME,
            Text.literal(title).formatted(color, Formatting.BOLD));
        List<Text> lore = new ArrayList<>();
        lore.add(Text.literal("  ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬").formatted(Formatting.DARK_GRAY));
        for (Stat stat : stats) {
            double raw = totals.getOrDefault(stat, 0.0);
            double display = stat.toDisplay(raw);
            String num = display < 10
                ? String.format("%.1f", display).replaceAll("0+$", "").replaceAll("\\.$", "")
                : String.format("%.0f", display);
            lore.add(Text.literal("  " + stat.getIcon() + " ").formatted(Formatting.GRAY)
                .append(Text.literal(num).formatted(Formatting.WHITE, Formatting.BOLD))
                .append(Text.literal("  " + stat.getDisplayName()).formatted(Formatting.GRAY)));
        }
        lore.add(Text.literal("  ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬").formatted(Formatting.DARK_GRAY));
        s.set(DataComponentTypes.LORE, new LoreComponent(lore));
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
        if (level > 1) stack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);

        stack.set(DataComponentTypes.CUSTOM_NAME,
            Text.literal(skill.getDisplayName()).formatted(nameColor(skill), Formatting.BOLD));

        List<Text> lore = new ArrayList<>();
        lore.add(Text.literal("  ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬").formatted(Formatting.DARK_GRAY));
        lore.add(Text.literal("  Level  ").formatted(Formatting.GRAY)
            .append(Text.literal(String.valueOf(level)).formatted(Formatting.WHITE, Formatting.BOLD))
            .append(Text.literal(" / " + Skill.MAX_LEVEL).formatted(Formatting.DARK_GRAY)));

        if (maxed) {
            lore.add(Text.literal("  ✦ MAX LEVEL ✦").formatted(Formatting.GOLD, Formatting.BOLD));
        } else {
            long progress = xp - floor;
            float pct = (float) progress / span * 100f;
            lore.add(Text.literal("  " + bar(progress, span, 14) + "  ").formatted(Formatting.GREEN)
                .append(Text.literal(String.format("%.1f%%", pct)).formatted(Formatting.GRAY)));
        }

        lore.add(Text.literal("  ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬").formatted(Formatting.DARK_GRAY));
        lore.add(Text.literal("  Click to view details").formatted(Formatting.DARK_GRAY));
        stack.set(DataComponentTypes.LORE, new LoreComponent(lore));
        return stack;
    }

    // ── Shared helpers ────────────────────────────────────────────────────────

    private static ItemStack pane(Item item, String name) {
        ItemStack s = new ItemStack(item);
        s.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name));
        return s;
    }

    private static String bar(long value, long max, int length) {
        int filled = max > 0 ? (int) Math.min(length, (value * length) / max) : length;
        return "█".repeat(filled) + "░".repeat(length - filled);
    }

    private static Formatting nameColor(Skill skill) {
        return switch (skill) {
            case MINING      -> Formatting.GRAY;
            case WOODCUTTING -> Formatting.GREEN;
            case EXCAVATING  -> Formatting.YELLOW;
            case FARMING     -> Formatting.DARK_GREEN;
            case FISHING     -> Formatting.AQUA;
            case DEFENSE     -> Formatting.WHITE;
            case SLAYING     -> Formatting.RED;
            case RANGED      -> Formatting.GOLD;
            case ENCHANTING  -> Formatting.LIGHT_PURPLE;
            case ALCHEMY     -> Formatting.DARK_PURPLE;
            case SMITHING    -> Formatting.DARK_GRAY;
            case COOKING     -> Formatting.YELLOW;
            case CRAFTING    -> Formatting.WHITE;
            case AGILITY     -> Formatting.BLUE;
            case TAMING      -> Formatting.GREEN;
            case TRADING     -> Formatting.GREEN;
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
