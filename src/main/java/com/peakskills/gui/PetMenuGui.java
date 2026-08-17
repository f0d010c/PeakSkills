package com.peakskills.gui;

import com.peakskills.pet.*;
import com.peakskills.player.PlayerData;
import com.peakskills.player.PlayerDataManager;
import com.peakskills.stat.StatManager;
import com.peakskills.skill.Skill;
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

public class PetMenuGui {

    // 21 pet slots across 3 rows of 7
    private static final int[] PET_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34
    };

    public enum Category { ALL, GATHERING, COMBAT, MASTERY }

    private static final Skill[] GATHERING_SKILLS = { Skill.MINING, Skill.WOODCUTTING, Skill.EXCAVATING, Skill.FARMING, Skill.FISHING, Skill.SMITHING };
    private static final Skill[] COMBAT_SKILLS    = { Skill.SLAYING, Skill.RANGED, Skill.DEFENSE };
    private static final Skill[] MASTERY_SKILLS   = { Skill.ENCHANTING, Skill.ALCHEMY, Skill.COOKING, Skill.CRAFTING, Skill.AGILITY, Skill.TAMING, Skill.TRADING };

    public static void open(ServerPlayer player) {
        open(player, Category.ALL);
    }

    public static void open(ServerPlayer player, Category filter) {
        open(player, filter, PlayerDataManager.get(player.getUUID()).isPetsVisible());
    }

    public static void open(ServerPlayer player, Category filter, boolean petsVisible) {
        PlayerData data = PlayerDataManager.get(player.getUUID());
        SimpleContainer inv = new SimpleContainer(54);
        Category[] currentFilter = { filter };
        boolean[] currentPetsVisible = { petsVisible };
        populate(inv, data, currentFilter[0], currentPetsVisible[0]);

        Map<Integer, Runnable> handlers        = buildClickHandlers(player, data, inv, currentFilter, currentPetsVisible);
        Map<Integer, Runnable> rightHandlers   = buildRightClickHandlers(player, data, inv, currentFilter, currentPetsVisible);
        Map<Integer, Runnable> middleHandlers  = buildMiddleClickHandlers(player, data, inv, currentFilter, currentPetsVisible);

        com.peakskills.gui.core.LegacyContainerGui.open(player,
            Component.literal("✦ Pet Roster").withStyle(ChatFormatting.LIGHT_PURPLE),
            inv, handlers, rightHandlers, middleHandlers);
    }

    // ── Build ─────────────────────────────────────────────────────────────────

    private static void populate(SimpleContainer inv, PlayerData data, Category filter, boolean petsVisible) {
        ItemStack bg = pane(" ");
        for (int i = 0; i < 54; i++) inv.setItem(i, bg.copy());

        // Filter tabs (slots 1-4)
        inv.setItem(1, filterTab("All",       Items.WHITE_STAINED_GLASS_PANE,  filter == Category.ALL));
        inv.setItem(2, filterTab("Gathering", Items.LIME_STAINED_GLASS_PANE,   filter == Category.GATHERING));
        inv.setItem(3, filterTab("Combat",    Items.RED_STAINED_GLASS_PANE,    filter == Category.COMBAT));
        inv.setItem(4, filterTab("Mastery",   Items.PURPLE_STAINED_GLASS_PANE, filter == Category.MASTERY));

        List<PetInstance> pets = data.getPetRoster().getPets().stream()
            .filter(p -> matchesFilter(p, filter))
            .toList();

        // Craft Pets button — slot 6
        ItemStack craftBtn = new ItemStack(Items.BLAZE_POWDER);
        craftBtn.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        craftBtn.set(DataComponents.CUSTOM_NAME,
            Component.literal("✦ Craft Pet Eggs").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
        craftBtn.set(DataComponents.LORE, new ItemLore(List.of(
            Component.literal("  Craft Common pet eggs").withStyle(ChatFormatting.DARK_GRAY),
            Component.literal("  using materials from your inventory").withStyle(ChatFormatting.DARK_GRAY)
        )));
        inv.setItem(6, craftBtn);

        // Visibility toggle — slot 7
        ItemStack visToggle = new ItemStack(petsVisible ? Items.ENDER_EYE : Items.FERMENTED_SPIDER_EYE);
        visToggle.set(DataComponents.CUSTOM_NAME,
            Component.literal(petsVisible ? "✦ Pet Visuals: ON" : "✦ Pet Visuals: OFF")
                .withStyle(petsVisible ? ChatFormatting.GREEN : ChatFormatting.RED, ChatFormatting.BOLD));
        visToggle.set(DataComponents.LORE, new ItemLore(List.of(
            Component.literal("  Click to " + (petsVisible ? "hide" : "show") + " active pet display")
                .withStyle(ChatFormatting.DARK_GRAY)
        )));
        inv.setItem(7, visToggle);

        // Title — slot 8
        ItemStack title = new ItemStack(Items.LEAD);
        title.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        title.set(DataComponents.CUSTOM_NAME,
            Component.literal("✦ Pet Roster  ").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD)
                .append(Component.literal(pets.size() + " / " + PetRoster.MAX_SLOTS)
                    .withStyle(ChatFormatting.GRAY)));
        title.set(DataComponents.LORE, new ItemLore(List.of(
            Component.literal("  Left-click a pet to activate / deactivate").withStyle(ChatFormatting.DARK_GRAY),
            Component.literal("  Shift-click a maxed pet to upgrade rarity").withStyle(ChatFormatting.DARK_GRAY),
            Component.literal("  Active pet earns XP and grants bonuses").withStyle(ChatFormatting.DARK_GRAY)
        )));
        inv.setItem(8, title);

        // Pet icons
        for (int i = 0; i < pets.size() && i < PET_SLOTS.length; i++) {
            inv.setItem(PET_SLOTS[i], petIcon(pets.get(i)));
        }

        // Empty slot indicators
        ItemStack empty = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
        empty.set(DataComponents.CUSTOM_NAME, Component.literal("Empty Slot").withStyle(ChatFormatting.DARK_GRAY));
        for (int i = pets.size(); i < PET_SLOTS.length; i++) {
            inv.setItem(PET_SLOTS[i], empty.copy());
        }
    }

    // ── Click handlers ────────────────────────────────────────────────────────

    private static Map<Integer, Runnable> buildClickHandlers(ServerPlayer player, PlayerData data,
                                                             SimpleContainer inv, Category[] currentFilter,
                                                             boolean[] currentPetsVisible) {
        Map<Integer, Runnable> handlers = new HashMap<>();

        // Filter tab handlers
        handlers.put(1, () -> refresh(inv, data, currentFilter, currentPetsVisible, Category.ALL));
        handlers.put(2, () -> refresh(inv, data, currentFilter, currentPetsVisible, Category.GATHERING));
        handlers.put(3, () -> refresh(inv, data, currentFilter, currentPetsVisible, Category.COMBAT));
        handlers.put(4, () -> refresh(inv, data, currentFilter, currentPetsVisible, Category.MASTERY));

        // Craft button
        handlers.put(6, () -> PetBreederGui.open(player));

        // Visibility toggle
        handlers.put(7, () -> {
            boolean nowVisible = !currentPetsVisible[0];
            data.setPetsVisible(nowVisible);
            currentPetsVisible[0] = nowVisible;
            if (nowVisible) {
                PetDisplayManager.restoreDisplay(player);
            } else {
                PetDisplayManager.killDisplay(player.getUUID(), PlayerDataManager.getServer());
            }
            populate(inv, data, currentFilter[0], currentPetsVisible[0]);
        });

        // Pet click handlers — must match the filtered list used in populate()
        for (int i = 0; i < PET_SLOTS.length; i++) {
            final int slot = PET_SLOTS[i];
            handlers.put(slot, () -> {
                PetInstance pet = petAtSlot(data, currentFilter[0], slot);
                if (pet == null) return;
                PetRoster roster = data.getPetRoster();
                if (pet.isActive()) {
                    roster.deactivate();
                    PetDisplayManager.killDisplay(player.getUUID(), com.peakskills.player.PlayerDataManager.getServer());
                    StatManager.applyStats(player);
                    player.sendSystemMessage(Component.literal("Pet deactivated.").withStyle(ChatFormatting.YELLOW), false);
                } else {
                    roster.setActivePet(pet.getId());
                    PetDisplayManager.spawnDisplay(player, pet.getType());
                    StatManager.applyStats(player);
                    player.sendSystemMessage(
                        Component.literal("Active pet: ").withStyle(ChatFormatting.GREEN)
                            .append(Component.literal(pet.getRarity().displayName + " " + pet.getType().displayName)
                                .withStyle(pet.getRarity().color)),
                        false);
                }
                populate(inv, data, currentFilter[0], currentPetsVisible[0]);
            });
        }
        return handlers;
    }

    private static void refresh(SimpleContainer inv, PlayerData data, Category[] currentFilter,
                                boolean[] currentPetsVisible, Category filter) {
        currentFilter[0] = filter;
        currentPetsVisible[0] = data.isPetsVisible();
        populate(inv, data, currentFilter[0], currentPetsVisible[0]);
    }

    private static PetInstance petAtSlot(PlayerData data, Category filter, int slot) {
        int index = -1;
        for (int i = 0; i < PET_SLOTS.length; i++) {
            if (PET_SLOTS[i] == slot) {
                index = i;
                break;
            }
        }
        if (index < 0) return null;

        List<PetInstance> pets = data.getPetRoster().getPets().stream()
            .filter(p -> matchesFilter(p, filter))
            .toList();
        return index < pets.size() ? pets.get(index) : null;
    }

    // ── Middle-click: upgrade pet ─────────────────────────────────────────────

    private static Map<Integer, Runnable> buildMiddleClickHandlers(ServerPlayer player, PlayerData data,
                                                                   SimpleContainer inv, Category[] currentFilter,
                                                                   boolean[] currentPetsVisible) {
        Map<Integer, Runnable> handlers = new HashMap<>();

        for (int i = 0; i < PET_SLOTS.length; i++) {
            final int slot = PET_SLOTS[i];
            handlers.put(slot, () -> {
                PetInstance pet = petAtSlot(data, currentFilter[0], slot);
                if (pet == null || !pet.canUpgrade()) return;
                PetUpgradeHandler.tryUpgrade(player, pet.getId());
                populate(inv, data, currentFilter[0], currentPetsVisible[0]);
            });
        }
        return handlers;
    }

    // ── Right-click: remove pet ───────────────────────────────────────────────

    private static Map<Integer, Runnable> buildRightClickHandlers(ServerPlayer player, PlayerData data,
                                                                  SimpleContainer inv, Category[] currentFilter,
                                                                  boolean[] currentPetsVisible) {
        Map<Integer, Runnable> handlers = new HashMap<>();

        for (int i = 0; i < PET_SLOTS.length; i++) {
            final int slot = PET_SLOTS[i];
            handlers.put(slot, () -> {
                PetInstance pet = petAtSlot(data, currentFilter[0], slot);
                if (pet == null) return;
                if (pet.isActive()) {
                    PetDisplayManager.killDisplay(player.getUUID(), com.peakskills.player.PlayerDataManager.getServer());
                }
                data.getPetRoster().removePet(pet.getId());
                StatManager.applyStats(player);

                // Return the pet as an egg in the player's inventory
                ItemStack egg = PetEggHandler.createEgg(pet.getType(), pet.getRarity(), pet.getXp());
                if (!player.getInventory().add(egg)) {
                    // Inventory full — drop at player's feet
                    player.drop(egg, false);
                }

                player.sendSystemMessage(
                    Component.literal("Returned ").withStyle(ChatFormatting.YELLOW)
                        .append(Component.literal(pet.getRarity().displayName + " " + pet.getType().displayName + " Egg")
                            .withStyle(pet.getRarity().color))
                        .append(Component.literal(" to your inventory.").withStyle(ChatFormatting.YELLOW)),
                    false);
                populate(inv, data, currentFilter[0], currentPetsVisible[0]);
            });
        }
        return handlers;
    }

    // ── Pet icon ──────────────────────────────────────────────────────────────

    public static ItemStack petIcon(PetInstance pet) {
        int level      = pet.getLevel();
        PetRarity rar  = pet.getRarity();
        long xp        = pet.getXp();
        long floor     = PetXPTable.xpForLevel(level, rar);
        long cap       = PetXPTable.xpForLevel(level + 1, rar);
        long span      = Math.max(1, cap - floor);
        long prog      = xp - floor;
        boolean maxed  = pet.isAtLevelCap();
        boolean active = pet.isActive();

        ItemStack stack = new ItemStack(pet.getType().icon);
        if (active) stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);

        stack.set(DataComponents.CUSTOM_NAME,
            Component.literal(rar.displayName + " " + pet.getType().displayName)
                .withStyle(rar.color, ChatFormatting.BOLD));

        List<Component> lore = new ArrayList<>();
        lore.add(separator());

        if (active) lore.add(Component.literal("  ✦ ACTIVE").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));

        lore.add(Component.literal("  Affinity: ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(pet.getType().affinity.getDisplayName()).withStyle(ChatFormatting.WHITE)));

        lore.add(Component.empty());

        lore.add(Component.literal("  Level  ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(String.valueOf(level)).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD))
            .append(Component.literal(" / " + rar.levelCap).withStyle(ChatFormatting.DARK_GRAY)));

        if (maxed && pet.canUpgrade()) {
            PetRarity next = rar.next();
            lore.add(Component.literal("  ✦ READY TO UPGRADE → ").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
                .append(Component.literal(next.displayName).withStyle(next.color, ChatFormatting.BOLD)));
            lore.add(Component.literal("  Cost: " + upgradeCost(rar)).withStyle(ChatFormatting.GRAY));
        } else if (maxed && rar.isMax()) {
            lore.add(Component.literal("  ✦ MAX RARITY & LEVEL").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        } else {
            lore.add(Component.literal("  " + bar(prog, span, 16) + "  ").withStyle(ChatFormatting.LIGHT_PURPLE)
                .append(Component.literal(String.format("%.1f%%", prog * 100.0 / span)).withStyle(ChatFormatting.GRAY)));
            lore.add(Component.literal(String.format("  %,d / %,d XP", prog, span)).withStyle(ChatFormatting.DARK_GRAY));
        }

        List<PetAbility> abilities = PetAbilityRegistry.getAbilities(pet.getType());
        if (!abilities.isEmpty()) {
            lore.add(Component.empty());
            lore.add(Component.literal("  Abilities:").withStyle(ChatFormatting.YELLOW));
            for (PetAbility ability : abilities) {
                lore.add(Component.literal("   ✦ " + ability.displayLine(level, rar))
                    .withStyle(ChatFormatting.GREEN));
            }
        }

        lore.add(separator());
        if (!active) {
            lore.add(Component.literal("  Left-click to activate").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            lore.add(Component.literal("  Left-click to deactivate").withStyle(ChatFormatting.DARK_GRAY));
        }
        if (pet.canUpgrade()) {
            lore.add(Component.literal("  Shift-click to upgrade rarity").withStyle(ChatFormatting.GREEN));
        }
        lore.add(Component.literal("  Right-click to remove").withStyle(ChatFormatting.RED));

        stack.set(DataComponents.LORE, new ItemLore(lore));
        return stack;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static boolean matchesFilter(PetInstance pet, Category filter) {
        return switch (filter) {
            case ALL      -> true;
            case GATHERING -> contains(GATHERING_SKILLS, pet.getType().affinity);
            case COMBAT    -> contains(COMBAT_SKILLS, pet.getType().affinity);
            case MASTERY   -> contains(MASTERY_SKILLS, pet.getType().affinity);
        };
    }

    private static boolean contains(Skill[] arr, Skill skill) {
        for (Skill s : arr) if (s == skill) return true;
        return false;
    }

    private static ItemStack filterTab(String label, Item item, boolean active) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME,
            Component.literal(label).withStyle(
                active ? ChatFormatting.WHITE : ChatFormatting.GRAY,
                active ? ChatFormatting.BOLD  : ChatFormatting.ITALIC));
        if (active) stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        return stack;
    }

    private static String upgradeCost(PetRarity rarity) {
        return switch (rarity) {
            case COMMON    -> "16 Gold Ingots";
            case UNCOMMON  -> "8 Diamonds";
            case RARE      -> "16 Emeralds + 4 Diamonds";
            case EPIC      -> "4 Netherite Ingots";
            case LEGENDARY -> "Already max";
        };
    }

    private static String bar(long value, long max, int length) {
        int filled = max > 0 ? (int) Math.min(length, (value * length) / max) : length;
        return "█".repeat(filled) + "░".repeat(length - filled);
    }

    private static Component separator() {
        return Component.literal("  ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬").withStyle(ChatFormatting.DARK_GRAY);
    }

    private static ItemStack pane(String name) {
        ItemStack s = new ItemStack(Items.BLACK_STAINED_GLASS_PANE);
        s.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        return s;
    }
}
