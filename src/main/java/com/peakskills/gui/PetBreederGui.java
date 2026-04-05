package com.peakskills.gui;

import com.peakskills.pet.*;
import com.peakskills.player.PlayerData;
import com.peakskills.player.PlayerDataManager;
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

import java.util.*;

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

    public static void open(ServerPlayerEntity player) {
        PlayerData data = PlayerDataManager.get(player.getUuid());
        SimpleInventory inv = new SimpleInventory(54);
        populate(inv, player);

        Map<Integer, Runnable> handlers = new HashMap<>();

        PetType[] types = PetType.values();
        for (int i = 0; i < types.length && i < PET_SLOTS.length; i++) {
            final PetType pt = types[i];
            handlers.put(PET_SLOTS[i], () -> {
                tryCraft(player, data, pt);
                open(player); // always refresh so affordability indicators update
            });
        }

        // Back to pet roster
        handlers.put(49, () -> PetMenuGui.open(player));

        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
            (syncId, playerInv, p) -> new SkillsScreenHandler(syncId, playerInv, inv, handlers),
            Text.literal("✦ Pet Breeder").formatted(Formatting.LIGHT_PURPLE)
        ));
    }

    // ── Build ─────────────────────────────────────────────────────────────────

    private static void populate(SimpleInventory inv, ServerPlayerEntity player) {
        ItemStack bg = pane(" ");
        for (int i = 0; i < 54; i++) inv.setStack(i, bg.copy());

        // Title (slot 4)
        ItemStack title = new ItemStack(Items.BLAZE_POWDER);
        title.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        title.set(DataComponentTypes.CUSTOM_NAME,
            Text.literal("✦ Pet Breeder").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD));
        title.set(DataComponentTypes.LORE, new LoreComponent(List.of(
            Text.literal("  Craft a Common pet egg").formatted(Formatting.DARK_GRAY),
            Text.literal("  Cost: 1 Lead + recipe material").formatted(Formatting.DARK_GRAY)
        )));
        inv.setStack(4, title);

        // Back button (slot 49)
        ItemStack back = new ItemStack(Items.ARROW);
        back.set(DataComponentTypes.CUSTOM_NAME,
            Text.literal("« Back to Pets").formatted(Formatting.GRAY));
        inv.setStack(49, back);

        // Pet recipe icons
        PetType[] types = PetType.values();
        for (int i = 0; i < types.length && i < PET_SLOTS.length; i++) {
            inv.setStack(PET_SLOTS[i], petRecipeIcon(types[i], player));
        }
    }

    // ── Pet recipe icon ───────────────────────────────────────────────────────

    private static ItemStack petRecipeIcon(PetType petType, ServerPlayerEntity player) {
        Recipe recipe = RECIPES.get(petType);
        boolean canAfford = recipe != null
            && hasItem(player, Items.LEAD, 1)
            && hasItem(player, recipe.material(), recipe.count());

        ItemStack stack = new ItemStack(petType.icon);
        stack.set(DataComponentTypes.CUSTOM_NAME,
            Text.literal(petType.displayName).formatted(
                canAfford ? Formatting.GREEN : Formatting.WHITE, Formatting.BOLD));

        List<Text> lore = new ArrayList<>();
        lore.add(Text.literal("  ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬").formatted(Formatting.DARK_GRAY));
        lore.add(Text.literal("  Affinity: ").formatted(Formatting.GRAY)
            .append(Text.literal(petType.affinity.getDisplayName()).formatted(Formatting.WHITE)));
        lore.add(Text.empty());
        lore.add(Text.literal("  Recipe — Common Egg:").formatted(Formatting.YELLOW));
        lore.add(Text.literal("   ✦ 1x Lead").formatted(
            hasItem(player, Items.LEAD, 1) ? Formatting.GREEN : Formatting.RED));
        if (recipe != null) {
            String matName = recipe.material().getName().getString();
            boolean hasMat = hasItem(player, recipe.material(), recipe.count());
            lore.add(Text.literal("   ✦ " + recipe.count() + "x " + matName)
                .formatted(hasMat ? Formatting.GREEN : Formatting.RED));
        }
        lore.add(Text.empty());
        if (canAfford) {
            lore.add(Text.literal("  ✦ Click to craft!").formatted(Formatting.GREEN, Formatting.BOLD));
        } else {
            lore.add(Text.literal("  ✗ Missing materials").formatted(Formatting.RED));
        }
        lore.add(Text.literal("  ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬").formatted(Formatting.DARK_GRAY));

        stack.set(DataComponentTypes.LORE, new LoreComponent(lore));
        return stack;
    }

    // ── Crafting ──────────────────────────────────────────────────────────────

    private static void tryCraft(ServerPlayerEntity player, PlayerData data, PetType petType) {
        Recipe recipe = RECIPES.get(petType);
        if (recipe == null) return;

        if (!hasItem(player, Items.LEAD, 1) || !hasItem(player, recipe.material(), recipe.count())) {
            player.sendMessage(
                Text.literal("✗ Need: 1x Lead + " + recipe.count() + "x "
                    + recipe.material().getName().getString())
                    .formatted(Formatting.RED),
                true);
            return;
        }

        if (data.getPetRoster().isFull()) {
            player.sendMessage(Text.literal("Pet roster is full! (" + PetRoster.MAX_SLOTS + " max)")
                .formatted(Formatting.RED), true);
            return;
        }

        removeItem(player, Items.LEAD, 1);
        removeItem(player, recipe.material(), recipe.count());

        ItemStack egg = PetEggHandler.createEgg(petType, PetRarity.COMMON);
        player.giveItemStack(egg);

        player.sendMessage(
            Text.literal("✦ Crafted: ").formatted(Formatting.GOLD)
                .append(Text.literal("Common " + petType.displayName + " Egg!")
                    .formatted(Formatting.WHITE, Formatting.BOLD))
                .append(Text.literal("  Right-click to hatch.").formatted(Formatting.DARK_GRAY)),
            false);
    }

    // ── Inventory helpers ─────────────────────────────────────────────────────

    private static boolean hasItem(ServerPlayerEntity player, Item item, int count) {
        int found = 0;
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack s = player.getInventory().getStack(i);
            if (s.getItem() == item) found += s.getCount();
            if (found >= count) return true;
        }
        return false;
    }

    private static void removeItem(ServerPlayerEntity player, Item item, int toRemove) {
        for (int i = 0; i < player.getInventory().size() && toRemove > 0; i++) {
            ItemStack s = player.getInventory().getStack(i);
            if (s.getItem() == item) {
                int take = Math.min(s.getCount(), toRemove);
                s.decrement(take);
                toRemove -= take;
            }
        }
    }

    private static ItemStack pane(String name) {
        ItemStack s = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
        s.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name));
        return s;
    }
}
