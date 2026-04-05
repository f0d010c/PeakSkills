package com.peakskills.gui;

import com.peakskills.pet.*;
import com.peakskills.player.PlayerData;
import com.peakskills.player.PlayerDataManager;
import com.peakskills.stat.StatManager;
import com.peakskills.skill.Skill;
import com.peakskills.pet.PetDisplayManager;
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

    public static void open(ServerPlayerEntity player) {
        open(player, Category.ALL);
    }

    public static void open(ServerPlayerEntity player, Category filter) {
        PlayerData data = PlayerDataManager.get(player.getUuid());
        SimpleInventory inv = new SimpleInventory(54);
        populate(inv, data, filter);

        Map<Integer, Runnable> handlers       = buildClickHandlers(player, data, filter);
        Map<Integer, Runnable> rightHandlers  = buildRightClickHandlers(player, data, filter);

        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
            (syncId, playerInv, p) -> new SkillsScreenHandler(syncId, playerInv, inv, handlers, rightHandlers),
            Text.literal("✦ Pet Roster").formatted(Formatting.LIGHT_PURPLE)
        ));
    }

    // ── Build ─────────────────────────────────────────────────────────────────

    private static void populate(SimpleInventory inv, PlayerData data, Category filter) {
        ItemStack bg = pane(" ");
        for (int i = 0; i < 54; i++) inv.setStack(i, bg.copy());

        // Filter tabs (slots 1-4)
        inv.setStack(1, filterTab("All",       Items.WHITE_STAINED_GLASS_PANE,  filter == Category.ALL));
        inv.setStack(2, filterTab("Gathering", Items.LIME_STAINED_GLASS_PANE,   filter == Category.GATHERING));
        inv.setStack(3, filterTab("Combat",    Items.RED_STAINED_GLASS_PANE,    filter == Category.COMBAT));
        inv.setStack(4, filterTab("Mastery",   Items.PURPLE_STAINED_GLASS_PANE, filter == Category.MASTERY));

        List<PetInstance> pets = data.getPetRoster().getPets().stream()
            .filter(p -> matchesFilter(p, filter))
            .toList();

        // Craft Pets button — slot 6
        ItemStack craftBtn = new ItemStack(Items.BLAZE_POWDER);
        craftBtn.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        craftBtn.set(DataComponentTypes.CUSTOM_NAME,
            Text.literal("✦ Craft Pet Eggs").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD));
        craftBtn.set(DataComponentTypes.LORE, new LoreComponent(List.of(
            Text.literal("  Craft Common pet eggs").formatted(Formatting.DARK_GRAY),
            Text.literal("  using materials from your inventory").formatted(Formatting.DARK_GRAY)
        )));
        inv.setStack(6, craftBtn);

        // Title — slot 8
        ItemStack title = new ItemStack(Items.LEAD);
        title.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        title.set(DataComponentTypes.CUSTOM_NAME,
            Text.literal("✦ Pet Roster  ").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD)
                .append(Text.literal(pets.size() + " / " + PetRoster.MAX_SLOTS)
                    .formatted(Formatting.GRAY)));
        title.set(DataComponentTypes.LORE, new LoreComponent(List.of(
            Text.literal("  Click a pet to activate / deactivate it").formatted(Formatting.DARK_GRAY),
            Text.literal("  Click a maxed pet to upgrade its rarity").formatted(Formatting.DARK_GRAY),
            Text.literal("  Active pet earns XP and grants bonuses").formatted(Formatting.DARK_GRAY)
        )));
        inv.setStack(8, title);

        // Pet icons
        for (int i = 0; i < pets.size() && i < PET_SLOTS.length; i++) {
            inv.setStack(PET_SLOTS[i], petIcon(pets.get(i)));
        }

        // Empty slot indicators
        ItemStack empty = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
        empty.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Empty Slot").formatted(Formatting.DARK_GRAY));
        for (int i = pets.size(); i < PET_SLOTS.length; i++) {
            inv.setStack(PET_SLOTS[i], empty.copy());
        }
    }

    // ── Click handlers ────────────────────────────────────────────────────────

    private static Map<Integer, Runnable> buildClickHandlers(ServerPlayerEntity player, PlayerData data, Category filter) {
        Map<Integer, Runnable> handlers = new HashMap<>();

        // Filter tab handlers
        handlers.put(1, () -> open(player, Category.ALL));
        handlers.put(2, () -> open(player, Category.GATHERING));
        handlers.put(3, () -> open(player, Category.COMBAT));
        handlers.put(4, () -> open(player, Category.MASTERY));

        // Craft button
        handlers.put(6, () -> PetBreederGui.open(player));

        // Pet click handlers — must match the filtered list used in populate()
        List<PetInstance> pets = data.getPetRoster().getPets().stream()
            .filter(p -> matchesFilter(p, filter))
            .toList();

        for (int i = 0; i < pets.size() && i < PET_SLOTS.length; i++) {
            final PetInstance pet = pets.get(i);
            final int slot = PET_SLOTS[i];
            handlers.put(slot, () -> {
                PetRoster roster = data.getPetRoster();
                if (pet.canUpgrade()) {
                    PetUpgradeHandler.tryUpgrade(player, pet.getId());
                    open(player, filter);
                } else if (pet.isActive()) {
                    roster.deactivate();
                    PetDisplayManager.killDisplay(player.getUuid(), com.peakskills.player.PlayerDataManager.getServer());
                    StatManager.applyStats(player);
                    player.sendMessage(Text.literal("Pet deactivated.").formatted(Formatting.YELLOW), false);
                    open(player, filter);
                } else {
                    roster.setActivePet(pet.getId());
                    PetDisplayManager.spawnDisplay(player, pet.getType());
                    StatManager.applyStats(player);
                    player.sendMessage(
                        Text.literal("Active pet: ").formatted(Formatting.GREEN)
                            .append(Text.literal(pet.getRarity().displayName + " " + pet.getType().displayName)
                                .formatted(pet.getRarity().color)),
                        false);
                    open(player, filter);
                }
            });
        }
        return handlers;
    }

    // ── Right-click: remove pet ───────────────────────────────────────────────

    private static Map<Integer, Runnable> buildRightClickHandlers(ServerPlayerEntity player, PlayerData data, Category filter) {
        Map<Integer, Runnable> handlers = new HashMap<>();

        List<PetInstance> pets = data.getPetRoster().getPets().stream()
            .filter(p -> matchesFilter(p, filter))
            .toList();

        for (int i = 0; i < pets.size() && i < PET_SLOTS.length; i++) {
            final PetInstance pet = pets.get(i);
            handlers.put(PET_SLOTS[i], () -> {
                if (pet.isActive()) {
                    PetDisplayManager.killDisplay(player.getUuid(), com.peakskills.player.PlayerDataManager.getServer());
                }
                data.getPetRoster().removePet(pet.getId());
                StatManager.applyStats(player);

                // Return the pet as an egg in the player's inventory
                ItemStack egg = PetEggHandler.createEgg(pet.getType(), pet.getRarity());
                if (!player.getInventory().insertStack(egg)) {
                    // Inventory full — drop at player's feet
                    player.dropItem(egg, false);
                }

                player.sendMessage(
                    Text.literal("Returned ").formatted(Formatting.YELLOW)
                        .append(Text.literal(pet.getRarity().displayName + " " + pet.getType().displayName + " Egg")
                            .formatted(pet.getRarity().color))
                        .append(Text.literal(" to your inventory.").formatted(Formatting.YELLOW)),
                    false);
                open(player, filter);
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
        if (active) stack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);

        stack.set(DataComponentTypes.CUSTOM_NAME,
            Text.literal(rar.displayName + " " + pet.getType().displayName)
                .formatted(rar.color, Formatting.BOLD));

        List<Text> lore = new ArrayList<>();
        lore.add(separator());

        if (active) lore.add(Text.literal("  ✦ ACTIVE").formatted(Formatting.GOLD, Formatting.BOLD));

        lore.add(Text.literal("  Affinity: ").formatted(Formatting.GRAY)
            .append(Text.literal(pet.getType().affinity.getDisplayName()).formatted(Formatting.WHITE)));

        lore.add(Text.empty());

        lore.add(Text.literal("  Level  ").formatted(Formatting.GRAY)
            .append(Text.literal(String.valueOf(level)).formatted(Formatting.WHITE, Formatting.BOLD))
            .append(Text.literal(" / " + rar.levelCap).formatted(Formatting.DARK_GRAY)));

        if (maxed && pet.canUpgrade()) {
            PetRarity next = rar.next();
            lore.add(Text.literal("  ✦ READY TO UPGRADE → ").formatted(Formatting.GREEN, Formatting.BOLD)
                .append(Text.literal(next.displayName).formatted(next.color, Formatting.BOLD)));
            lore.add(Text.literal("  Cost: " + upgradeCost(rar)).formatted(Formatting.GRAY));
        } else if (maxed && rar.isMax()) {
            lore.add(Text.literal("  ✦ MAX RARITY & LEVEL").formatted(Formatting.GOLD, Formatting.BOLD));
        } else {
            lore.add(Text.literal("  " + bar(prog, span, 16) + "  ").formatted(Formatting.LIGHT_PURPLE)
                .append(Text.literal(String.format("%.1f%%", prog * 100.0 / span)).formatted(Formatting.GRAY)));
            lore.add(Text.literal(String.format("  %,d / %,d XP", prog, span)).formatted(Formatting.DARK_GRAY));
        }

        List<PetAbility> abilities = PetAbilityRegistry.getAbilities(pet.getType());
        if (!abilities.isEmpty()) {
            lore.add(Text.empty());
            lore.add(Text.literal("  Abilities:").formatted(Formatting.YELLOW));
            for (PetAbility ability : abilities) {
                lore.add(Text.literal("   ✦ " + ability.displayLine(level, rar))
                    .formatted(Formatting.GREEN));
            }
        }

        lore.add(separator());
        if (pet.canUpgrade()) {
            lore.add(Text.literal("  Left-click to upgrade rarity").formatted(Formatting.GREEN));
        } else if (!active) {
            lore.add(Text.literal("  Left-click to activate").formatted(Formatting.DARK_GRAY));
        } else {
            lore.add(Text.literal("  Left-click to deactivate").formatted(Formatting.DARK_GRAY));
        }
        lore.add(Text.literal("  Right-click to remove").formatted(Formatting.RED));

        stack.set(DataComponentTypes.LORE, new LoreComponent(lore));
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
        stack.set(DataComponentTypes.CUSTOM_NAME,
            Text.literal(label).formatted(
                active ? Formatting.WHITE : Formatting.GRAY,
                active ? Formatting.BOLD  : Formatting.ITALIC));
        if (active) stack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
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

    private static Text separator() {
        return Text.literal("  ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬").formatted(Formatting.DARK_GRAY);
    }

    private static ItemStack pane(String name) {
        ItemStack s = new ItemStack(Items.BLACK_STAINED_GLASS_PANE);
        s.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name));
        return s;
    }
}
