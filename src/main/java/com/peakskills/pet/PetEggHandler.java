package com.peakskills.pet;

import com.peakskills.player.PlayerData;
import com.peakskills.player.PlayerDataManager;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.mob.SpiderEntity;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.component.type.LoreComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Handles pet egg drops from mob kills and hatching via right-click.
 *
 * Drop chance: 3% base (+0.05% per Taming level).
 * Eggs store pet type and rarity in CUSTOM_DATA NBT.
 */
public class PetEggHandler {

    private static final double BASE_DROP_CHANCE = 0.03;
    private static final double TAMING_BONUS_PER_LEVEL = 0.0005; // +0.05%/level → max +4.95% at 99

    // ── Registration ─────────────────────────────────────────────────────────

    public static void register() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (!isPetEgg(stack)) return ActionResult.PASS;
            if (world.isClient()) return ActionResult.PASS;

            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
            hatch(serverPlayer, stack, hand);
            return ActionResult.SUCCESS;
        });
    }

    // ── Drop logic (called from SkillEvents) ─────────────────────────────────

    public static void tryDrop(LivingEntity entity, ServerPlayerEntity killer) {
        Optional<PetType> petType = petTypeFor(entity);
        if (petType.isEmpty()) return;

        PlayerData data = PlayerDataManager.get(killer.getUuid());
        int tamingLevel = data.getLevel(com.peakskills.skill.Skill.TAMING);
        double chance = BASE_DROP_CHANCE + tamingLevel * TAMING_BONUS_PER_LEVEL;

        // Use the mob's world (always ServerWorld since this is AFTER_DEATH on server)
        if (!(entity.getEntityWorld() instanceof ServerWorld serverWorld)) return;
        if (serverWorld.getRandom().nextDouble() >= chance) return;

        PetRarity rarity = rollRarity(serverWorld.getRandom());
        ItemStack egg = createEgg(petType.get(), rarity);

        ItemEntity item = new ItemEntity(
            serverWorld,
            entity.getX(), entity.getY() + 0.5, entity.getZ(),
            egg
        );
        item.setPickupDelay(10);
        serverWorld.spawnEntity(item);

        killer.sendMessage(
            Text.literal("✦ A pet egg dropped! ").formatted(Formatting.GOLD)
                .append(Text.literal(rarity.displayName + " " + petType.get().displayName + " Egg")
                    .formatted(rarity.color, Formatting.BOLD)),
            true);
    }

    // ── Hatching ─────────────────────────────────────────────────────────────

    private static void hatch(ServerPlayerEntity player, ItemStack stack, Hand hand) {
        NbtComponent nbtComp = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (nbtComp == null) return;
        NbtCompound nbt = nbtComp.copyNbt();
        if (!nbt.contains("petEggType")) return;

        PetType type;
        PetRarity rarity;
        try {
            type   = PetType.valueOf(nbt.getString("petEggType").orElse(""));
            rarity = PetRarity.valueOf(nbt.getString("petEggRarity").orElse(""));
        } catch (IllegalArgumentException e) { return; }

        PlayerData data = PlayerDataManager.get(player.getUuid());
        if (data.getPetRoster().isFull()) {
            player.sendMessage(Text.literal("Your pet roster is full! (Max " + PetRoster.MAX_SLOTS + " pets)")
                .formatted(Formatting.RED), false);
            return;
        }

        long rawXp  = nbt.getLong("petEggXp").orElse(0L);
        long maxXp  = PetXPTable.xpForLevel(rarity.levelCap, rarity);
        long storedXp = Math.max(0, Math.min(rawXp, maxXp));
        PetInstance pet = new PetInstance(java.util.UUID.randomUUID(), type, rarity, storedXp);

        data.getPetRoster().addPet(pet);

        // Consume egg
        stack.decrement(1);
        player.setStackInHand(hand, stack.isEmpty() ? ItemStack.EMPTY : stack);

        player.sendMessage(
            Text.literal("✦ Hatched: ").formatted(Formatting.GOLD)
                .append(Text.literal(rarity.displayName + " " + type.displayName)
                    .formatted(rarity.color, Formatting.BOLD))
                .append(Text.literal(" — use /pets to view it!").formatted(Formatting.GRAY)),
            false);
    }

    // ── Item creation ─────────────────────────────────────────────────────────

    public static ItemStack createEgg(PetType type, PetRarity rarity) {
        return createEgg(type, rarity, 0L);
    }

    public static ItemStack createEgg(PetType type, PetRarity rarity, long xp) {
        ItemStack stack = new ItemStack(Items.PAPER);

        // Store type + rarity (+ xp for re-hatching) in NBT
        NbtCompound nbt = new NbtCompound();
        nbt.putString("petEggType",   type.name());
        nbt.putString("petEggRarity", rarity.name());
        nbt.putBoolean("isPetEgg", true);
        if (xp > 0) nbt.putLong("petEggXp", xp);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));

        // Cosmetics
        stack.set(DataComponentTypes.CUSTOM_NAME,
            Text.literal(rarity.displayName + " " + type.displayName + " Egg")
                .formatted(rarity.color, Formatting.BOLD));
        stack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);

        List<Text> lore = new ArrayList<>();
        lore.add(Text.literal("  Affinity: ").formatted(Formatting.GRAY)
            .append(Text.literal(type.affinity.getDisplayName()).formatted(Formatting.WHITE)));
        lore.add(Text.empty());
        lore.add(Text.literal("  Right-click to hatch!").formatted(Formatting.YELLOW));
        stack.set(DataComponentTypes.LORE, new LoreComponent(lore));

        return stack;
    }

    public static boolean isPetEgg(ItemStack stack) {
        NbtComponent comp = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (comp == null) return false;
        return comp.copyNbt().getBoolean("isPetEgg").orElse(false);
    }

    // ── Entity → PetType mapping ──────────────────────────────────────────────

    private static Optional<PetType> petTypeFor(LivingEntity entity) {
        if (entity instanceof IronGolemEntity) return Optional.of(PetType.IRON_GOLEM);
        if (entity instanceof BatEntity)       return Optional.of(PetType.BAT);
        if (entity instanceof FoxEntity)       return Optional.of(PetType.FOX);
        if (entity instanceof RabbitEntity)    return Optional.of(PetType.RABBIT);
        if (entity instanceof BeeEntity)       return Optional.of(PetType.BEE);
        if (entity instanceof AxolotlEntity)   return Optional.of(PetType.AXOLOTL);
        if (entity instanceof DolphinEntity)   return Optional.of(PetType.DOLPHIN);
        if (entity instanceof WolfEntity)      return Optional.of(PetType.WOLF);
        if (entity instanceof SpiderEntity)    return Optional.of(PetType.SPIDER);
        if (entity instanceof TurtleEntity)    return Optional.of(PetType.TURTLE);
        if (entity instanceof EndermanEntity)  return Optional.of(PetType.ENDERMAN);
        if (entity instanceof MooshroomEntity) return Optional.of(PetType.MOOSHROOM);
        if (entity instanceof ChickenEntity)   return Optional.of(PetType.CHICKEN);
        if (entity instanceof SheepEntity)     return Optional.of(PetType.SHEEP);
        if (entity instanceof CatEntity)       return Optional.of(PetType.CAT);
        if (entity instanceof HorseEntity)     return Optional.of(PetType.HORSE);
        if (entity instanceof AllayEntity)     return Optional.of(PetType.ALLAY);
        if (entity instanceof ParrotEntity)    return Optional.of(PetType.PARROT);
        return Optional.empty();
    }

    private static PetRarity rollRarity(net.minecraft.util.math.random.Random rng) {
        double roll = rng.nextDouble();
        if (roll < 0.02) return PetRarity.EPIC;
        if (roll < 0.10) return PetRarity.RARE;
        if (roll < 0.30) return PetRarity.UNCOMMON;
        return PetRarity.COMMON;
    }
}
