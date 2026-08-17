package com.peakskills.gui;

import com.peakskills.player.PlayerData;
import com.peakskills.player.PlayerDataManager;
import com.peakskills.skill.Skill;
import com.peakskills.skill.SkillAbility;
import com.peakskills.skill.SkillAbilityRegistry;
import com.peakskills.stat.SkillStatSource;
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

    public static void open(ServerPlayer player, PlayerData data, Skill skill) {
        int playerLevel = data.getLevel(skill);
        int page        = Math.max(0, (playerLevel - 1) / LEVELS_PER_PAGE);
        open(player, data, skill, page);
    }

    public static void open(ServerPlayer player, PlayerData data, Skill skill, int page) {
        SimpleContainer inv = new SimpleContainer(54);
        populate(inv, data, skill, page);

        Map<Integer, Runnable> handlers = new HashMap<>();
        configureHandlers(inv, handlers, player, skill, page);

        com.peakskills.gui.core.LegacyContainerGui.open(player,
            Component.literal(skill.getDisplayName() + " Skill").withStyle(nameColor(skill), ChatFormatting.BOLD),
            inv, handlers);
    }

    // ── Populate ──────────────────────────────────────────────────────────────

    private static void configureHandlers(SimpleContainer inv, Map<Integer, Runnable> handlers,
                                          ServerPlayer player, Skill skill, int page) {
        handlers.clear();
        handlers.put(0, () -> SkillsGui.open(player));
        if (page > 0) {
            handlers.put(45, () -> refreshPage(inv, handlers, player, skill, page - 1));
        }
        if (page < MAX_PAGE) {
            handlers.put(49, () -> refreshPage(inv, handlers, player, skill, page + 1));
        }
    }

    private static void refreshPage(SimpleContainer inv, Map<Integer, Runnable> handlers,
                                    ServerPlayer player, Skill skill, int page) {
        populate(inv, PlayerDataManager.get(player.getUUID()), skill, page);
        configureHandlers(inv, handlers, player, skill, page);
        inv.setChanged();
    }

    private static void populate(SimpleContainer inv, PlayerData data, Skill skill, int page) {
        // Gray background everywhere
        for (int i = 0; i < 54; i++)
            inv.setItem(i, pane(Items.GRAY_STAINED_GLASS_PANE, " "));

        // Skill-coloured header stripe (slots 1-3, 5-7)
        Item hPane = skillPane(skill);
        for (int col : new int[]{1, 2, 3, 5, 6, 7})
            inv.setItem(col, pane(hPane, " "));

        int playerLevel = data.getLevel(skill);
        int startLevel  = page * LEVELS_PER_PAGE + 1;

        // ── Slot 0: barrier block as back button ─────────────────────────────
        ItemStack back = new ItemStack(Items.BARRIER);
        back.set(DataComponents.CUSTOM_NAME,
            Component.literal("← Back to Skills").withStyle(nameColor(skill), ChatFormatting.BOLD));
        back.set(DataComponents.LORE, new ItemLore(List.of(
            Component.literal("  Click to return").withStyle(ChatFormatting.DARK_GRAY))));
        inv.setItem(0, back);

        // ── Skill title (slot 4) ─────────────────────────────────────────────
        ItemStack title = new ItemStack(iconFor(skill));
        title.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        title.set(DataComponents.CUSTOM_NAME,
            Component.literal(skill.getDisplayName() + " Skill").withStyle(nameColor(skill), ChatFormatting.BOLD));
        title.set(DataComponents.LORE, new ItemLore(List.of(
            Component.literal("Level ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.valueOf(playerLevel)).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD))
                .append(Component.literal(" / " + Skill.MAX_LEVEL).withStyle(ChatFormatting.DARK_GRAY)),
            Component.empty(),
            Component.literal(skill.getDescription()).withStyle(ChatFormatting.GRAY),
            Component.empty(),
            Component.literal(skill.getTrainingTip()).withStyle(ChatFormatting.YELLOW)
        )));
        inv.setItem(4, title);

        // ── Page label (slot 8) ──────────────────────────────────────────────
        int endLevel = Math.min(startLevel + LEVELS_PER_PAGE - 1, Skill.MAX_LEVEL);
        ItemStack pageInfo = new ItemStack(Items.PAPER);
        pageInfo.set(DataComponents.CUSTOM_NAME,
            Component.literal("Page " + (page + 1) + " / " + (MAX_PAGE + 1)).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD));
        pageInfo.set(DataComponents.LORE, new ItemLore(List.of(
            Component.literal("Levels " + startLevel + " \u2013 " + endLevel).withStyle(ChatFormatting.GRAY))));
        inv.setItem(8, pageInfo);

        // ── Connector panes — glow skill colour as path is reached ───────────
        for (int[] conn : CONNECTOR_SLOTS) {
            int slot     = conn[0];
            int laterIdx = conn[1];
            boolean lit  = playerLevel >= startLevel + laterIdx;
            inv.setItem(slot, pane(lit ? hPane : Items.GRAY_STAINED_GLASS_PANE, " "));
        }

        // ── Level items ──────────────────────────────────────────────────────
        for (int i = 0; i < LEVEL_SLOTS.length; i++) {
            int level = startLevel + i;
            if (level > Skill.MAX_LEVEL) break;
            inv.setItem(LEVEL_SLOTS[i], levelItem(skill, level, playerLevel));
        }

        // ── Navigation ───────────────────────────────────────────────────────
        if (page > 0) {
            ItemStack prev = new ItemStack(Items.ARROW);
            prev.set(DataComponents.CUSTOM_NAME,
                Component.literal("Previous Page").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
            prev.set(DataComponents.LORE, new ItemLore(List.of(
                Component.literal("Levels " + ((page-1)*LEVELS_PER_PAGE+1) + " \u2013 " + (page*LEVELS_PER_PAGE))
                    .withStyle(ChatFormatting.DARK_GRAY))));
            inv.setItem(45, prev);
        }
        if (page < MAX_PAGE) {
            ItemStack next = new ItemStack(Items.ARROW);
            next.set(DataComponents.CUSTOM_NAME,
                Component.literal("Next Page").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
            next.set(DataComponents.LORE, new ItemLore(List.of(
                Component.literal("Levels " + (startLevel+LEVELS_PER_PAGE) + " \u2013 " +
                    Math.min(startLevel+LEVELS_PER_PAGE*2-1, Skill.MAX_LEVEL))
                    .withStyle(ChatFormatting.DARK_GRAY))));
            inv.setItem(49, next);
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
        // ── Level number in bottom-right corner ──────────────────────────
        stack.set(DataComponents.MAX_STACK_SIZE, Skill.MAX_LEVEL);
        stack.setCount(level);

        // Glow current level and reached-special slots
        if (isCurrent || (reached && (isSpecial || isMilestone)))
            stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);

        // ── Hover name ────────────────────────────────────────────────────
        ChatFormatting nameFmt = isCurrent ? ChatFormatting.GOLD
                           : reached   ? ChatFormatting.GREEN
                                       : ChatFormatting.DARK_GRAY;

        if (isCurrent) {
            stack.set(DataComponents.CUSTOM_NAME,
                Component.literal("Level " + level + " — Current").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        } else if (isSpecial) {
            String spName = gearIcon != null
                ? (gearUnlockNames(skill, level).isEmpty() ? "Gear Unlock" : gearUnlockNames(skill, level).get(0))
                : abilities.get(0).name();
            ChatFormatting spFmt = reached
                ? (gearIcon != null ? ChatFormatting.GREEN : ChatFormatting.LIGHT_PURPLE)
                : ChatFormatting.GRAY;
            stack.set(DataComponents.CUSTOM_NAME,
                Component.literal("Level " + level + "  ").withStyle(ChatFormatting.DARK_GRAY)
                    .append(Component.literal(spName).withStyle(spFmt, ChatFormatting.BOLD)));
        } else if (isMilestone) {
            stack.set(DataComponents.CUSTOM_NAME,
                Component.literal("Level " + level + "  ★ Milestone")
                    .withStyle(reached ? ChatFormatting.GOLD : ChatFormatting.YELLOW, ChatFormatting.BOLD));
        } else {
            stack.set(DataComponents.CUSTOM_NAME,
                Component.literal("Level " + level).withStyle(nameFmt, ChatFormatting.BOLD));
        }

        // ── Lore ─────────────────────────────────────────────────────────
        List<Component> lore = new ArrayList<>();
        lore.add(separator());

        if (isSpecial && gearIcon != null) {
            lore.add(Component.literal("  ⚔ GEAR UNLOCK").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
            for (String name : gearUnlockNames(skill, level))
                lore.add(reached ? Component.literal("  ✔ " + name).withStyle(ChatFormatting.GREEN)
                                 : Component.literal("  ◆ " + name).withStyle(ChatFormatting.YELLOW));
            lore.add(Component.empty());
        }

        if (!abilities.isEmpty()) {
            lore.add(Component.literal("  ✦ NEW ABILITY").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
            for (SkillAbility ab : abilities) {
                lore.add(Component.literal("  " + ab.name()).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
                lore.add(Component.literal("  " + ab.description()).withStyle(ChatFormatting.GRAY));
            }
            lore.add(Component.empty());
        }

        if (isMilestone) {
            lore.add(Component.literal("  ★ MILESTONE REWARD").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            lore.add(Component.literal("  A special item drops on first reach.").withStyle(ChatFormatting.GRAY));
            lore.add(Component.empty());
        }

        // Stat bonuses this level provides
        lore.add(Component.literal("  Stat Bonuses:").withStyle(ChatFormatting.YELLOW));
        for (SkillStatSource src : StatRegistry.SOURCES.stream().filter(s -> s.skill() == skill).toList()) {
            double perLvl = src.stat().toDisplay(src.valuePerLevel());
            double total  = src.stat().toDisplay(src.compute(level));
            lore.add(Component.literal("  +" + fmtVal(perLvl) + " " + src.stat().getIcon()
                    + " " + src.stat().getDisplayName()).withStyle(ChatFormatting.GREEN)
                .append(Component.literal("  (Total: " + fmtVal(total) + ")").withStyle(ChatFormatting.DARK_GRAY)));
        }

        lore.add(Component.empty());
        lore.add(separator());
        if (reached) {
            lore.add(isCurrent
                ? Component.literal("  ▶ YOUR CURRENT LEVEL").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                : Component.literal("  ✔ UNLOCKED").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
        } else {
            lore.add(Component.literal("  ✗ " + (level - playerLevel) + " levels to go").withStyle(ChatFormatting.RED));
        }

        stack.set(DataComponents.LORE, new ItemLore(lore));
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

    private static Component separator() {
        return Component.literal(" \u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac")
            .withStyle(ChatFormatting.DARK_GRAY);
    }

    private static ItemStack pane(Item item, String name) {
        ItemStack s = new ItemStack(item);
        s.set(DataComponents.CUSTOM_NAME, Component.literal(name));
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
            case SMITHING    -> ChatFormatting.GRAY;
            case COOKING     -> ChatFormatting.YELLOW;
            case CRAFTING    -> ChatFormatting.WHITE;
            case AGILITY     -> ChatFormatting.BLUE;
            case TAMING      -> ChatFormatting.GREEN;
            case TRADING     -> ChatFormatting.GREEN;
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
