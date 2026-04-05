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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public static void open(ServerPlayerEntity viewer) {
        open(viewer, PlayerDataManager.get(viewer.getUuid()), viewer.getName().getString());
    }

    public static void open(ServerPlayerEntity viewer, PlayerData data, String ownerName) {
        SimpleInventory inv = new SimpleInventory(54);
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

        // Refresh button — slot 49
        handlers.put(49, () -> SkillsGui.open(viewer, PlayerDataManager.get(viewer.getUuid()), ownerName));

        viewer.openHandledScreen(new SimpleNamedScreenHandlerFactory(
            (syncId, playerInv, p) -> new SkillsScreenHandler(syncId, playerInv, inv, handlers),
            Text.literal("Your Skills").formatted(Formatting.GOLD, Formatting.BOLD)
        ));
    }

    // ── Build ─────────────────────────────────────────────────────────────────

    private static void populate(SimpleInventory inv, PlayerData data, String ownerName) {
        // Gray background
        ItemStack bg = pane(Items.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) inv.setStack(i, bg.copy());

        // Header title (slot 4) — diamond sword as the "skills" icon
        ItemStack header = new ItemStack(Items.NETHER_STAR);
        header.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        header.set(DataComponentTypes.CUSTOM_NAME,
            Text.literal("Your Skills").formatted(Formatting.GOLD, Formatting.BOLD));
        int total = data.getTotalLevel();
        int max   = TOTAL_SKILLS * Skill.MAX_LEVEL;
        header.set(DataComponentTypes.LORE, new LoreComponent(List.of(
            Text.literal("  Total Level: ").formatted(Formatting.GRAY)
                .append(Text.literal(total + " / " + max).formatted(Formatting.WHITE, Formatting.BOLD)),
            Text.literal("  Click a skill to view its leveling path").formatted(Formatting.DARK_GRAY)
        )));
        inv.setStack(4, header);

        // Skill icons
        for (int i = 0; i < GATHERING.length; i++)
            inv.setStack(GATHER_SLOTS[i],  skillIcon(GATHERING[i], data));
        for (int i = 0; i < COMBAT.length; i++)
            inv.setStack(COMBAT_SLOTS[i],  skillIcon(COMBAT[i],    data));
        for (int i = 0; i < MASTERY.length; i++)
            inv.setStack(MASTERY_SLOTS[i], skillIcon(MASTERY[i],   data));

        // Total XP bottle (slot 40)
        ItemStack totalItem = new ItemStack(Items.EXPERIENCE_BOTTLE);
        totalItem.set(DataComponentTypes.CUSTOM_NAME,
            Text.literal("Total Level  " + total + " / " + max)
                .formatted(Formatting.AQUA, Formatting.BOLD));
        totalItem.set(DataComponentTypes.LORE, new LoreComponent(List.of(
            separator(),
            Text.literal("  " + bar(total, max, 20) + "  ").formatted(Formatting.AQUA)
                .append(Text.literal(String.format("%.1f%%", total * 100.0 / max)).formatted(Formatting.WHITE)),
            separator()
        )));
        inv.setStack(40, totalItem);

        // Back/refresh (slot 49)
        ItemStack refresh = new ItemStack(Items.ARROW);
        refresh.set(DataComponentTypes.CUSTOM_NAME,
            Text.literal("Refresh").formatted(Formatting.YELLOW, Formatting.BOLD));
        refresh.set(DataComponentTypes.LORE, new LoreComponent(List.of(
            Text.literal("  Click to reload your skill data").formatted(Formatting.DARK_GRAY)
        )));
        inv.setStack(49, refresh);
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
        if (level > 1) stack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);

        // Skill name
        stack.set(DataComponentTypes.CUSTOM_NAME,
            Text.literal(skill.getDisplayName()).formatted(nameColor(skill), Formatting.BOLD));

        // Lore
        List<Text> lore = new ArrayList<>();
        lore.add(separator());

        // Description
        lore.add(Text.literal("  " + skill.getDescription()).formatted(Formatting.GRAY));
        lore.add(Text.empty());

        // Level
        lore.add(Text.literal("  Level  ").formatted(Formatting.GRAY)
            .append(Text.literal(String.valueOf(level)).formatted(Formatting.WHITE, Formatting.BOLD))
            .append(Text.literal(" / " + Skill.MAX_LEVEL).formatted(Formatting.DARK_GRAY)));

        // Progress bar
        if (maxed) {
            lore.add(Text.literal("  ✦ MAX LEVEL ✦").formatted(Formatting.GOLD, Formatting.BOLD));
        } else {
            float pct = (float) progress / span * 100f;
            lore.add(Text.literal("  " + bar(progress, span, 16) + "  ").formatted(Formatting.GREEN)
                .append(Text.literal(String.format("%.1f%%", pct)).formatted(Formatting.GRAY)));
            lore.add(Text.literal(String.format("  %,d / %,d XP", progress, span))
                .formatted(Formatting.DARK_GRAY));
        }

        // Stat bonuses
        List<SkillStatSource> sources = StatRegistry.SOURCES.stream()
            .filter(s -> s.skill() == skill)
            .toList();

        if (!sources.isEmpty()) {
            lore.add(Text.empty());
            lore.add(Text.literal("  Stat Bonuses:").formatted(Formatting.YELLOW));
            for (SkillStatSource src : sources) {
                double current = src.compute(level);
                lore.add(
                    Text.literal("   +" + statValue(src.stat(), current) + "  ").formatted(Formatting.GREEN)
                        .append(Text.literal("(" + statValue(src.stat(), src.valuePerLevel()) + "/lvl)")
                            .formatted(Formatting.DARK_GRAY)));
            }
        } else {
            lore.add(Text.empty());
            lore.add(Text.literal("  No stat bonuses").formatted(Formatting.DARK_GRAY));
        }

        // Active pet XP bonus for this skill
        data.getPetRoster().getActivePet().ifPresent(pet -> {
            double xpBonus = PetAbilityRegistry.getAbilities(pet.getType()).stream()
                .filter(a -> a.type == PetAbility.Type.XP_BONUS && a.skill == skill)
                .mapToDouble(a -> a.compute(pet.getLevel(), pet.getRarity()))
                .sum();
            if (xpBonus > 0) {
                lore.add(Text.empty());
                lore.add(Text.literal("  Active Pet Bonus:").formatted(Formatting.GOLD));
                lore.add(Text.literal("   +" + String.format("%.1f%%", xpBonus * 100) + " XP  ")
                    .formatted(Formatting.GREEN)
                    .append(Text.literal("(" + pet.getRarity().displayName + " " + pet.getType().displayName + ")")
                        .formatted(pet.getRarity().color)));
            }
        });

        lore.add(separator());
        stack.set(DataComponentTypes.LORE, new LoreComponent(lore));
        return stack;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static ItemStack pane(Item item, String name) {
        ItemStack s = new ItemStack(item);
        s.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name));
        return s;
    }

    private static Text separator() {
        return Text.literal("  ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬").formatted(Formatting.DARK_GRAY);
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
