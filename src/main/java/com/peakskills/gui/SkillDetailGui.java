package com.peakskills.gui;

import com.peakskills.player.PlayerData;
import com.peakskills.player.PlayerDataManager;
import com.peakskills.skill.Skill;
import com.peakskills.skill.SkillAbility;
import com.peakskills.skill.SkillAbilityRegistry;
import com.peakskills.stat.SkillStatSource;
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

public class SkillDetailGui {

    // ── Snake path (25 slots per page) ────────────────────────────────────────
    private static final int[] LEVEL_SLOTS = {
         9, 18, 27, 36,       // col0 ↓  (L1–L4)
         37, 38,               // row4 →  (L5–L6)
         29, 20, 11,           // col2 ↑  (L7–L9)
         12, 13,               // row1 →  (L10–L11)
         22, 31, 40,           // col4 ↓  (L12–L14)
         41, 42,               // row4 →  (L15–L16)
         33, 24, 15,           // col6 ↑  (L17–L19)
         16, 17,               // row1 →  (L20–L21)
         26, 35, 44, 53        // col8 ↓  (L22–L25)
    };

    // Bridge slots between snake columns.
    // {slot, laterPageIndex} — lit when playerLevel >= startLevel + laterPageIndex
    private static final int[][] CONNECTOR_SLOTS = {
        {10, 8},  {19, 7},  {28, 6},
        {21, 11}, {30, 12}, {39, 13},
        {23, 17}, {32, 16},
        {25, 21}, {34, 22}, {43, 23},
    };

    private static final int LEVELS_PER_PAGE = 25;
    private static final int MAX_PAGE        = (Skill.MAX_LEVEL - 1) / LEVELS_PER_PAGE; // 3

    // ── Open ──────────────────────────────────────────────────────────────────

    public static void open(ServerPlayerEntity player, PlayerData data, Skill skill) {
        int playerLevel = data.getLevel(skill);
        int page        = Math.max(0, (playerLevel - 1) / LEVELS_PER_PAGE);
        open(player, data, skill, page);
    }

    public static void open(ServerPlayerEntity player, PlayerData data, Skill skill, int page) {
        SimpleInventory inv = new SimpleInventory(54);
        populate(inv, data, skill, page);

        Map<Integer, Runnable> handlers = new HashMap<>();
        handlers.put(0, () -> SkillsGui.open(player));
        if (page > 0)
            handlers.put(45, () -> open(player, PlayerDataManager.get(player.getUuid()), skill, page - 1));
        if (page < MAX_PAGE)
            handlers.put(49, () -> open(player, PlayerDataManager.get(player.getUuid()), skill, page + 1));

        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
            (syncId, playerInv, p) -> new SkillsScreenHandler(syncId, playerInv, inv, handlers),
            Text.literal(skill.getDisplayName() + " Skill").formatted(nameColor(skill), Formatting.BOLD)
        ));
    }

    // ── Populate ──────────────────────────────────────────────────────────────

    private static void populate(SimpleInventory inv, PlayerData data, Skill skill, int page) {
        // Gray background everywhere
        for (int i = 0; i < 54; i++)
            inv.setStack(i, pane(Items.GRAY_STAINED_GLASS_PANE, " "));

        // Skill-coloured header stripe (slots 1-3, 5-7)
        Item hPane = skillPane(skill);
        for (int col : new int[]{1, 2, 3, 5, 6, 7})
            inv.setStack(col, pane(hPane, " "));

        int playerLevel = data.getLevel(skill);
        int startLevel  = page * LEVELS_PER_PAGE + 1;

        // ── Slot 0: barrier block as back button ─────────────────────────────
        ItemStack back = new ItemStack(Items.BARRIER);
        back.set(DataComponentTypes.CUSTOM_NAME,
            Text.literal("← Back to Skills").formatted(nameColor(skill), Formatting.BOLD));
        back.set(DataComponentTypes.LORE, new LoreComponent(List.of(
            Text.literal("  Click to return").formatted(Formatting.DARK_GRAY))));
        inv.setStack(0, back);

        // ── Skill title (slot 4) ─────────────────────────────────────────────
        ItemStack title = new ItemStack(iconFor(skill));
        title.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        title.set(DataComponentTypes.CUSTOM_NAME,
            Text.literal(skill.getDisplayName() + " Skill").formatted(nameColor(skill), Formatting.BOLD));
        title.set(DataComponentTypes.LORE, new LoreComponent(List.of(
            Text.literal("Level ").formatted(Formatting.GRAY)
                .append(Text.literal(String.valueOf(playerLevel)).formatted(Formatting.WHITE, Formatting.BOLD))
                .append(Text.literal(" / " + Skill.MAX_LEVEL).formatted(Formatting.DARK_GRAY)),
            Text.empty(),
            Text.literal(skill.getDescription()).formatted(Formatting.GRAY),
            Text.empty(),
            Text.literal(skill.getTrainingTip()).formatted(Formatting.YELLOW)
        )));
        inv.setStack(4, title);

        // ── Page label (slot 8) ──────────────────────────────────────────────
        int endLevel = Math.min(startLevel + LEVELS_PER_PAGE - 1, Skill.MAX_LEVEL);
        ItemStack pageInfo = new ItemStack(Items.PAPER);
        pageInfo.set(DataComponentTypes.CUSTOM_NAME,
            Text.literal("Page " + (page + 1) + " / " + (MAX_PAGE + 1)).formatted(Formatting.WHITE, Formatting.BOLD));
        pageInfo.set(DataComponentTypes.LORE, new LoreComponent(List.of(
            Text.literal("Levels " + startLevel + " \u2013 " + endLevel).formatted(Formatting.GRAY))));
        inv.setStack(8, pageInfo);

        // ── Connector panes — glow skill colour as path is reached ───────────
        for (int[] conn : CONNECTOR_SLOTS) {
            int slot     = conn[0];
            int laterIdx = conn[1];
            boolean lit  = playerLevel >= startLevel + laterIdx;
            inv.setStack(slot, pane(lit ? hPane : Items.GRAY_STAINED_GLASS_PANE, " "));
        }

        // ── Level items ──────────────────────────────────────────────────────
        for (int i = 0; i < LEVEL_SLOTS.length; i++) {
            int level = startLevel + i;
            if (level > Skill.MAX_LEVEL) break;
            inv.setStack(LEVEL_SLOTS[i], levelItem(skill, level, playerLevel));
        }

        // ── Navigation ───────────────────────────────────────────────────────
        if (page > 0) {
            ItemStack prev = new ItemStack(Items.ARROW);
            prev.set(DataComponentTypes.CUSTOM_NAME,
                Text.literal("Previous Page").formatted(Formatting.YELLOW, Formatting.BOLD));
            prev.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                Text.literal("Levels " + ((page-1)*LEVELS_PER_PAGE+1) + " \u2013 " + (page*LEVELS_PER_PAGE))
                    .formatted(Formatting.DARK_GRAY))));
            inv.setStack(45, prev);
        }
        if (page < MAX_PAGE) {
            ItemStack next = new ItemStack(Items.ARROW);
            next.set(DataComponentTypes.CUSTOM_NAME,
                Text.literal("Next Page").formatted(Formatting.YELLOW, Formatting.BOLD));
            next.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                Text.literal("Levels " + (startLevel+LEVELS_PER_PAGE) + " \u2013 " +
                    Math.min(startLevel+LEVELS_PER_PAGE*2-1, Skill.MAX_LEVEL))
                    .formatted(Formatting.DARK_GRAY))));
            inv.setStack(49, next);
        }
    }

    // ── Level item ────────────────────────────────────────────────────────────

    private static ItemStack levelItem(Skill skill, int level, int playerLevel) {
        boolean reached    = playerLevel >= level;
        boolean isCurrent  = playerLevel == level;
        boolean isMilestone = level == 25 || level == 50 || level == 75 || level == 99;

        Item gearIcon = gearUnlockItem(skill, level);
        List<SkillAbility> abilities = SkillAbilityRegistry.getAbilities(skill).stream()
            .filter(a -> a.minLevel() == level).toList();
        boolean isSpecial = gearIcon != null || !abilities.isEmpty();

        // ── Icon ─────────────────────────────────────────────────────────
        // Current level = skill's own icon. Reached special slots = their icon.
        // All others = coloured glass pane (green reached, red not-yet).
        Item icon;
        if (isCurrent) {
            icon = iconFor(skill);
        } else if (isSpecial && reached) {
            icon = gearIcon != null ? gearIcon : Items.ENCHANTED_BOOK;
        } else if (isMilestone && reached) {
            icon = Items.NETHER_STAR;
        } else if (reached) {
            icon = Items.LIME_STAINED_GLASS_PANE;
        } else {
            icon = Items.RED_STAINED_GLASS_PANE;
        }

        ItemStack stack = new ItemStack(icon);
        // ── Level number in bottom-right corner (capped at 64 — MC limit) ─
        stack.setCount(Math.min(level, 64));

        // Glow current level and reached-special slots
        if (isCurrent || (reached && (isSpecial || isMilestone)))
            stack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);

        // ── Hover name ────────────────────────────────────────────────────
        Formatting nameFmt = isCurrent ? Formatting.GOLD
                           : reached   ? Formatting.GREEN
                                       : Formatting.DARK_GRAY;

        if (isCurrent) {
            stack.set(DataComponentTypes.CUSTOM_NAME,
                Text.literal("Level " + level + " — Current").formatted(Formatting.GOLD, Formatting.BOLD));
        } else if (isSpecial) {
            String spName = gearIcon != null
                ? (gearUnlockNames(skill, level).isEmpty() ? "Gear Unlock" : gearUnlockNames(skill, level).get(0))
                : abilities.get(0).name();
            Formatting spFmt = reached
                ? (gearIcon != null ? Formatting.GREEN : Formatting.LIGHT_PURPLE)
                : Formatting.GRAY;
            stack.set(DataComponentTypes.CUSTOM_NAME,
                Text.literal("Level " + level + "  ").formatted(Formatting.DARK_GRAY)
                    .append(Text.literal(spName).formatted(spFmt, Formatting.BOLD)));
        } else if (isMilestone) {
            stack.set(DataComponentTypes.CUSTOM_NAME,
                Text.literal("Level " + level + "  ★ Milestone")
                    .formatted(reached ? Formatting.GOLD : Formatting.YELLOW, Formatting.BOLD));
        } else {
            stack.set(DataComponentTypes.CUSTOM_NAME,
                Text.literal("Level " + level).formatted(nameFmt, Formatting.BOLD));
        }

        // ── Lore ─────────────────────────────────────────────────────────
        List<Text> lore = new ArrayList<>();
        lore.add(separator());

        if (isSpecial && gearIcon != null) {
            lore.add(Text.literal("  ⚔ GEAR UNLOCK").formatted(Formatting.YELLOW, Formatting.BOLD));
            for (String name : gearUnlockNames(skill, level))
                lore.add(reached ? Text.literal("  ✔ " + name).formatted(Formatting.GREEN)
                                 : Text.literal("  ◆ " + name).formatted(Formatting.YELLOW));
            lore.add(Text.empty());
        }

        if (!abilities.isEmpty()) {
            lore.add(Text.literal("  ✦ NEW ABILITY").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD));
            for (SkillAbility ab : abilities) {
                lore.add(Text.literal("  " + ab.name()).formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD));
                lore.add(Text.literal("  " + ab.description()).formatted(Formatting.GRAY));
            }
            lore.add(Text.empty());
        }

        if (isMilestone) {
            lore.add(Text.literal("  ★ MILESTONE REWARD").formatted(Formatting.GOLD, Formatting.BOLD));
            lore.add(Text.literal("  A special item drops on first reach.").formatted(Formatting.GRAY));
            lore.add(Text.empty());
        }

        // Stat bonuses this level provides
        lore.add(Text.literal("  Stat Bonuses:").formatted(Formatting.YELLOW));
        for (SkillStatSource src : StatRegistry.SOURCES.stream().filter(s -> s.skill() == skill).toList()) {
            double perLvl = src.stat().toDisplay(src.valuePerLevel());
            double total  = src.stat().toDisplay(src.compute(level));
            lore.add(Text.literal("  +" + fmtVal(perLvl) + " " + src.stat().getIcon()
                    + " " + src.stat().getDisplayName()).formatted(Formatting.GREEN)
                .append(Text.literal("  (Total: " + fmtVal(total) + ")").formatted(Formatting.DARK_GRAY)));
        }

        lore.add(Text.empty());
        lore.add(separator());
        if (reached) {
            lore.add(isCurrent
                ? Text.literal("  ▶ YOUR CURRENT LEVEL").formatted(Formatting.GOLD, Formatting.BOLD)
                : Text.literal("  ✔ UNLOCKED").formatted(Formatting.GREEN, Formatting.BOLD));
        } else {
            lore.add(Text.literal("  ✗ " + (level - playerLevel) + " levels to go").formatted(Formatting.RED));
        }

        stack.set(DataComponentTypes.LORE, new LoreComponent(lore));
        return stack;
    }

    // ── Gear unlock helpers ───────────────────────────────────────────────────

    /** Returns the icon item for the gear unlocked at this level, or null if none. */
    private static Item gearUnlockItem(Skill skill, int level) {
        return switch (skill) {
            case MINING      -> level == 20 ? Items.IRON_PICKAXE      : level == 50 ? Items.DIAMOND_PICKAXE      : level == 70 ? Items.NETHERITE_PICKAXE      : null;
            case WOODCUTTING -> level == 20 ? Items.IRON_AXE          : level == 50 ? Items.DIAMOND_AXE          : level == 70 ? Items.NETHERITE_AXE          : null;
            case EXCAVATING  -> level == 20 ? Items.IRON_SHOVEL       : level == 50 ? Items.DIAMOND_SHOVEL       : level == 70 ? Items.NETHERITE_SHOVEL       : null;
            case FARMING     -> level == 20 ? Items.IRON_HOE          : level == 50 ? Items.DIAMOND_HOE          : level == 70 ? Items.NETHERITE_HOE          : null;
            case SLAYING     -> level == 20 ? Items.IRON_SWORD        : level == 50 ? Items.DIAMOND_SWORD        : level == 70 ? Items.NETHERITE_SWORD        : null;
            case DEFENSE     -> level == 20 ? Items.IRON_CHESTPLATE   : level == 50 ? Items.DIAMOND_CHESTPLATE   : level == 70 ? Items.NETHERITE_CHESTPLATE   : null;
            default -> null;
        };
    }

    /** Returns the display name(s) of gear unlocked at this level. */
    private static List<String> gearUnlockNames(Skill skill, int level) {
        List<String> r = new ArrayList<>();
        switch (skill) {
            case MINING      -> { if (level==20) r.add("Iron Pickaxe");    if (level==50) r.add("Diamond Pickaxe");    if (level==70) r.add("Netherite Pickaxe"); }
            case WOODCUTTING -> { if (level==20) r.add("Iron Axe");        if (level==50) r.add("Diamond Axe");        if (level==70) r.add("Netherite Axe"); }
            case EXCAVATING  -> { if (level==20) r.add("Iron Shovel");     if (level==50) r.add("Diamond Shovel");     if (level==70) r.add("Netherite Shovel"); }
            case FARMING     -> { if (level==20) r.add("Iron Hoe");        if (level==50) r.add("Diamond Hoe");        if (level==70) r.add("Netherite Hoe"); }
            case SLAYING     -> { if (level==20) r.add("Iron Sword");      if (level==50) r.add("Diamond Sword");      if (level==70) r.add("Netherite Sword"); }
            case DEFENSE     -> { if (level==20) r.add("Iron Armor Set");  if (level==50) r.add("Diamond Armor Set");  if (level==70) r.add("Netherite Armor Set"); }
            default -> {}
        }
        return r;
    }

    // ── Ability names & flavour ───────────────────────────────────────────────

    private static String abilityNameFor(Skill skill) {
        return switch (skill) {
            case MINING      -> "Prospector";
            case WOODCUTTING -> "Lumberjack";
            case EXCAVATING  -> "Spelunker";
            case FARMING     -> "Cultivator";
            case FISHING     -> "Angler";
            case DEFENSE     -> "Guardian";
            case SLAYING     -> "Warrior";
            case RANGED      -> "Archer";
            case ENCHANTING  -> "Enchanter";
            case ALCHEMY     -> "Alchemist";
            case SMITHING    -> "Forger";
            case COOKING     -> "Chef";
            case CRAFTING    -> "Artisan";
            case AGILITY     -> "Sprinter";
            case TAMING      -> "Tamer";
            case TRADING     -> "Merchant";
        };
    }

    private static String abilityFlavour(Skill skill, int level) {
        return switch (skill) {
            case MINING      -> "Your armor hardens from ore dust. +" + level + " Defense.";
            case WOODCUTTING -> "Your strikes grow mightier. +" + level + " Strength.";
            case EXCAVATING  -> "Earth yields to your will. +" + level + " Strength.";
            case FARMING     -> "The land blesses you with vitality. +" + (level / 2) + " Health.";
            case FISHING     -> "Fortune smiles on your casts. +" + level + " Luck.";
            case DEFENSE     -> "You shrug off blows with ease. +" + level + " Defense.";
            case SLAYING     -> "Your combat prowess grows. +" + level + " Strength.";
            case RANGED      -> "Your aim sharpens with each kill. +" + level + " Strength.";
            case ENCHANTING  -> "Arcane knowledge fills your mind. +" + level + " Luck.";
            case ALCHEMY     -> "Potions flow through your veins. +" + (level / 2) + " Health.";
            case SMITHING    -> "Metals bend to your expertise. +" + level + " Toughness.";
            case COOKING     -> "Nourishment fuels your body. +" + level + " Health.";
            case CRAFTING    -> "Your hands shape the world. +" + level + " Luck.";
            case AGILITY     -> "Your feet barely touch the ground. +" + level + " Speed.";
            case TAMING      -> "Beasts sense your kind spirit. +" + level + " Luck.";
            case TRADING     -> "Merchants favour your silver tongue. +" + level + " Luck.";
        };
    }

    // ── Roman numerals ────────────────────────────────────────────────────────

    private static final int[]    RN_VAL = {1000,900,500,400,100,90,50,40,10,9,5,4,1};
    private static final String[] RN_SYM = {"M","CM","D","CD","C","XC","L","XL","X","IX","V","IV","I"};

    private static String toRoman(int n) {
        if (n <= 0) return String.valueOf(n);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < RN_VAL.length; i++)
            while (n >= RN_VAL[i]) { sb.append(RN_SYM[i]); n -= RN_VAL[i]; }
        return sb.toString();
    }

    // ── Misc helpers ──────────────────────────────────────────────────────────

    private static String fmtVal(double v) {
        if (v == 0) return "0";
        return v < 1
            ? String.format("%.2f", v).replaceAll("0+$", "").replaceAll("\\.$", "")
            : String.format("%.1f", v).replaceAll("\\.0$", "");
    }

    private static Text separator() {
        return Text.literal(" \u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac")
            .formatted(Formatting.DARK_GRAY);
    }

    private static ItemStack pane(Item item, String name) {
        ItemStack s = new ItemStack(item);
        s.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name));
        return s;
    }

    // ── Skill → stained glass pane ────────────────────────────────────────────

    private static Item skillPane(Skill skill) {
        return switch (skill) {
            case MINING      -> Items.GRAY_STAINED_GLASS_PANE;
            case WOODCUTTING -> Items.GREEN_STAINED_GLASS_PANE;
            case EXCAVATING  -> Items.YELLOW_STAINED_GLASS_PANE;
            case FARMING     -> Items.LIME_STAINED_GLASS_PANE;
            case FISHING     -> Items.CYAN_STAINED_GLASS_PANE;
            case DEFENSE     -> Items.WHITE_STAINED_GLASS_PANE;
            case SLAYING     -> Items.RED_STAINED_GLASS_PANE;
            case RANGED      -> Items.ORANGE_STAINED_GLASS_PANE;
            case ENCHANTING  -> Items.PINK_STAINED_GLASS_PANE;
            case ALCHEMY     -> Items.PURPLE_STAINED_GLASS_PANE;
            case SMITHING    -> Items.LIGHT_GRAY_STAINED_GLASS_PANE;
            case COOKING     -> Items.ORANGE_STAINED_GLASS_PANE;
            case CRAFTING    -> Items.BROWN_STAINED_GLASS_PANE;
            case AGILITY     -> Items.BLUE_STAINED_GLASS_PANE;
            case TAMING      -> Items.GREEN_STAINED_GLASS_PANE;
            case TRADING     -> Items.LIME_STAINED_GLASS_PANE;
        };
    }

    // ── Skill name colour ─────────────────────────────────────────────────────

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
            case SMITHING    -> Formatting.GRAY;
            case COOKING     -> Formatting.YELLOW;
            case CRAFTING    -> Formatting.WHITE;
            case AGILITY     -> Formatting.BLUE;
            case TAMING      -> Formatting.GREEN;
            case TRADING     -> Formatting.GREEN;
        };
    }

    // ── Skill title icon ──────────────────────────────────────────────────────

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
