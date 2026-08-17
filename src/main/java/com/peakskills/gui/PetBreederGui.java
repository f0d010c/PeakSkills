package com.peakskills.gui;

import com.peakskills.pet.*;
import com.peakskills.player.PlayerData;
import com.peakskills.player.PlayerDataManager;
import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

/**
 * Pet crafting screen — shows all pet types with their crafting recipes.
 * Cost: 1 Lead + a type-specific material (consumed from inventory).
 * Produces a Common pet egg that must be right-clicked to hatch.
 */
public class PetBreederGui {

    private static final int[] PET_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31
    };

    private record Recipe(Item material, int count) {}

    private static final Map<PetType, Recipe> RECIPES = new EnumMap<>(PetType.class);
    static {
        RECIPES.put(PetType.IRON_GOLEM, new Recipe(Items.IRON_INGOT,       16));
        RECIPES.put(PetType.BAT,        new Recipe(Items.LEATHER,            8));
        RECIPES.put(PetType.FOX,        new Recipe(Items.SWEET_BERRIES,      8));
        RECIPES.put(PetType.RABBIT,     new Recipe(Items.RABBIT_FOOT,        4));
        RECIPES.put(PetType.BEE,        new Recipe(Items.HONEYCOMB,          8));
        RECIPES.put(PetType.AXOLOTL,    new Recipe(Items.TROPICAL_FISH,      4));
        RECIPES.put(PetType.DOLPHIN,    new Recipe(Items.COD,                4));
        RECIPES.put(PetType.WOLF,       new Recipe(Items.BONE,               8));
        RECIPES.put(PetType.SPIDER,     new Recipe(Items.SPIDER_EYE,         8));
        RECIPES.put(PetType.TURTLE,     new Recipe(Items.TURTLE_SCUTE,       4));
        RECIPES.put(PetType.ENDERMAN,   new Recipe(Items.ENDER_PEARL,        4));
        RECIPES.put(PetType.MOOSHROOM,  new Recipe(Items.RED_MUSHROOM,       8));
        RECIPES.put(PetType.CHICKEN,    new Recipe(Items.FEATHER,            8));
        RECIPES.put(PetType.SHEEP,      new Recipe(Items.WHITE_WOOL,         8));
        RECIPES.put(PetType.CAT,        new Recipe(Items.STRING,             8));
        RECIPES.put(PetType.HORSE,      new Recipe(Items.SUGAR,              8));
        RECIPES.put(PetType.ALLAY,      new Recipe(Items.AMETHYST_SHARD,     8));
        RECIPES.put(PetType.PARROT,     new Recipe(Items.WHEAT_SEEDS,       16));
    }

    // ── Open ──────────────────────────────────────────────────────────────────

    public static void open(ServerPlayer player) {
        PlayerData data = PlayerDataManager.get(player.getUUID());
        SimpleContainer inv = new SimpleContainer(54);
        populate(inv, player);

        Map<Integer, Runnable> handlers = new HashMap<>();

        PetType[] types = PetType.values();
        for (int i = 0; i < types.length && i < PET_SLOTS.length; i++) {
            final PetType pt = types[i];
            handlers.put(PET_SLOTS[i], () -> {
                tryCraft(player, data, pt);
                populate(inv, player); // refresh affordability indicators without reopening
            });
        }

        // Back to pet roster
        handlers.put(49, () -> PetMenuGui.open(player));

        player.openMenu(new SimpleMenuProvider(
            (syncId, playerInv, p) -> new SkillsScreenHandler(syncId, playerInv, inv, handlers),
            Component.literal("✦ Pet Breeder").withStyle(ChatFormatting.LIGHT_PURPLE)
        ));
    }

    // ── Build ─────────────────────────────────────────────────────────────────

    private static void populate(SimpleContainer inv, ServerPlayer player) {
        ItemStack bg = pane(" ");
        for (int i = 0; i < 54; i++) inv.setItem(i, bg.copy());

        // Title (slot 4)
        ItemStack title = new ItemStack(Items.BLAZE_POWDER);
        title.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        title.set(DataComponents.CUSTOM_NAME,
            Component.literal("✦ Pet Breeder").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
        title.set(DataComponents.LORE, new ItemLore(List.of(
            Component.literal("  Craft a Common pet egg").withStyle(ChatFormatting.DARK_GRAY),
            Component.literal("  Cost: 1 Lead + recipe material").withStyle(ChatFormatting.DARK_GRAY)
        )));
        inv.setItem(4, title);

        // Back button (slot 49)
        ItemStack back = new ItemStack(Items.ARROW);
        back.set(DataComponents.CUSTOM_NAME,
            Component.literal("« Back to Pets").withStyle(ChatFormatting.GRAY));
        inv.setItem(49, back);

        // Pet recipe icons
        PetType[] types = PetType.values();
        for (int i = 0; i < types.length && i < PET_SLOTS.length; i++) {
            inv.setItem(PET_SLOTS[i], petRecipeIcon(types[i], player));
        }
    }

    // ── Pet recipe icon ───────────────────────────────────────────────────────

    private static ItemStack petRecipeIcon(PetType petType, ServerPlayer player) {
        Recipe recipe = RECIPES.get(petType);
        boolean canAfford = recipe != null
            && hasItem(player, Items.LEAD, 1)
            && hasItem(player, recipe.material(), recipe.count());

        ItemStack stack = new ItemStack(petType.icon);
        stack.set(DataComponents.CUSTOM_NAME,
            Component.literal(petType.displayName).withStyle(
                canAfford ? ChatFormatting.GREEN : ChatFormatting.WHITE, ChatFormatting.BOLD));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.literal("  ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬").withStyle(ChatFormatting.DARK_GRAY));
        lore.add(Component.literal("  Affinity: ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(petType.affinity.getDisplayName()).withStyle(ChatFormatting.WHITE)));
        lore.add(Component.empty());
        lore.add(Component.literal("  Recipe — Common Egg:").withStyle(ChatFormatting.YELLOW));
        lore.add(Component.literal("   ✦ 1x Lead").withStyle(
            hasItem(player, Items.LEAD, 1) ? ChatFormatting.GREEN : ChatFormatting.RED));
        if (recipe != null) {
            String matName = new ItemStack(recipe.material()).getHoverName().getString();
            boolean hasMat = hasItem(player, recipe.material(), recipe.count());
            lore.add(Component.literal("   ✦ " + recipe.count() + "x " + matName)
                .withStyle(hasMat ? ChatFormatting.GREEN : ChatFormatting.RED));
        }
        lore.add(Component.empty());
        if (canAfford) {
            lore.add(Component.literal("  ✦ Click to craft!").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
        } else {
            lore.add(Component.literal("  ✗ Missing materials").withStyle(ChatFormatting.RED));
        }
        lore.add(Component.literal("  ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬").withStyle(ChatFormatting.DARK_GRAY));

        stack.set(DataComponents.LORE, new ItemLore(lore));
        return stack;
    }

    // ── Crafting ──────────────────────────────────────────────────────────────

    private static void tryCraft(ServerPlayer player, PlayerData data, PetType petType) {
        Recipe recipe = RECIPES.get(petType);
        if (recipe == null) return;

        if (!hasItem(player, Items.LEAD, 1) || !hasItem(player, recipe.material(), recipe.count())) {
            player.sendSystemMessage(
                Component.literal("✗ Need: 1x Lead + " + recipe.count() + "x "
                    + new ItemStack(recipe.material()).getHoverName().getString())
                    .withStyle(ChatFormatting.RED),
                true);
            return;
        }

        if (data.getPetRoster().isFull()) {
            player.sendSystemMessage(Component.literal("Pet roster is full! (" + PetRoster.MAX_SLOTS + " max)")
                .withStyle(ChatFormatting.RED), true);
            return;
        }

        removeItem(player, Items.LEAD, 1);
        removeItem(player, recipe.material(), recipe.count());

        ItemStack egg = PetEggHandler.createEgg(petType, PetRarity.COMMON);
        player.addItem(egg);

        player.sendSystemMessage(
            Component.literal("✦ Crafted: ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal("Common " + petType.displayName + " Egg!")
                    .withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD))
                .append(Component.literal("  Right-click to hatch.").withStyle(ChatFormatting.DARK_GRAY)),
            false);
    }

    // ── Inventory helpers ─────────────────────────────────────────────────────

    private static boolean hasItem(ServerPlayer player, Item item, int count) {
        int found = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.getItem() == item) found += s.getCount();
            if (found >= count) return true;
        }
        return false;
    }

    private static void removeItem(ServerPlayer player, Item item, int toRemove) {
        for (int i = 0; i < player.getInventory().getContainerSize() && toRemove > 0; i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.getItem() == item) {
                int take = Math.min(s.getCount(), toRemove);
                s.shrink(take);
                toRemove -= take;
            }
        }
    }

    private static ItemStack pane(String name) {
        ItemStack s = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
        s.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        return s;
    }
}
