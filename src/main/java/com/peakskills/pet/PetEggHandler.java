package com.peakskills.pet;

import com.peakskills.player.PlayerData;
import com.peakskills.player.PlayerDataManager;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.minecraft.world.entity.animal.dolphin.Dolphin;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.animal.turtle.Turtle;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
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
            ItemStack stack = player.getItemInHand(hand);
            if (!isPetEgg(stack)) return InteractionResult.PASS;
            if (world.isClientSide()) return InteractionResult.PASS;

            ServerPlayer serverPlayer = (ServerPlayer) player;
            hatch(serverPlayer, stack, hand);
            return InteractionResult.SUCCESS;
        });
    }

    // ── Drop logic (called from SkillEvents) ─────────────────────────────────

    public static void tryDrop(LivingEntity entity, ServerPlayer killer) {
        Optional<PetType> petType = petTypeFor(entity);
        if (petType.isEmpty()) return;

        PlayerData data = PlayerDataManager.get(killer.getUUID());
        int tamingLevel = data.getLevel(com.peakskills.skill.Skill.TAMING);
        double chance = BASE_DROP_CHANCE + tamingLevel * TAMING_BONUS_PER_LEVEL;

        // Use the mob's world (always ServerWorld since this is AFTER_DEATH on server)
        if (!(entity.level() instanceof ServerLevel serverWorld)) return;
        if (serverWorld.getRandom().nextDouble() >= chance) return;

        PetRarity rarity = rollRarity(serverWorld.getRandom());
        ItemStack egg = createEgg(petType.get(), rarity);

        ItemEntity item = new ItemEntity(
            serverWorld,
            entity.getX(), entity.getY() + 0.5, entity.getZ(),
            egg
        );
        item.setPickUpDelay(10);
        serverWorld.addFreshEntity(item);

        killer.sendSystemMessage(
            Component.literal("✦ A pet egg dropped! ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(rarity.displayName + " " + petType.get().displayName + " Egg")
                    .withStyle(rarity.color, ChatFormatting.BOLD)),
            true);
    }

    // ── Hatching ─────────────────────────────────────────────────────────────

    private static void hatch(ServerPlayer player, ItemStack stack, InteractionHand hand) {
        CustomData nbtComp = stack.get(DataComponents.CUSTOM_DATA);
        if (nbtComp == null) return;
        CompoundTag nbt = nbtComp.copyTag();
        if (!nbt.contains("petEggType")) return;

        PetType type;
        PetRarity rarity;
        try {
            type   = PetType.valueOf(nbt.getString("petEggType").orElse(""));
            rarity = PetRarity.valueOf(nbt.getString("petEggRarity").orElse(""));
        } catch (IllegalArgumentException e) { return; }

        PlayerData data = PlayerDataManager.get(player.getUUID());
        if (data.getPetRoster().isFull()) {
            player.sendSystemMessage(Component.literal("Your pet roster is full! (Max " + PetRoster.MAX_SLOTS + " pets)")
                .withStyle(ChatFormatting.RED), false);
            return;
        }

        long rawXp  = nbt.getLong("petEggXp").orElse(0L);
        long maxXp  = PetXPTable.xpForLevel(rarity.levelCap, rarity);
        long storedXp = Math.max(0, Math.min(rawXp, maxXp));
        PetInstance pet = new PetInstance(java.util.UUID.randomUUID(), type, rarity, storedXp);

        data.getPetRoster().addPet(pet);

        // Consume egg
        stack.shrink(1);
        player.setItemInHand(hand, stack.isEmpty() ? ItemStack.EMPTY : stack);

        player.sendSystemMessage(
            Component.literal("✦ Hatched: ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(rarity.displayName + " " + type.displayName)
                    .withStyle(rarity.color, ChatFormatting.BOLD))
                .append(Component.literal(" — use /pets to view it!").withStyle(ChatFormatting.GRAY)),
            false);
    }

    // ── Item creation ─────────────────────────────────────────────────────────

    public static ItemStack createEgg(PetType type, PetRarity rarity) {
        return createEgg(type, rarity, 0L);
    }

    public static ItemStack createEgg(PetType type, PetRarity rarity, long xp) {
        ItemStack stack = new ItemStack(Items.PAPER);

        // Store type + rarity (+ xp for re-hatching) in NBT
        CompoundTag nbt = new CompoundTag();
        nbt.putString("petEggType",   type.name());
        nbt.putString("petEggRarity", rarity.name());
        nbt.putBoolean("isPetEgg", true);
        if (xp > 0) nbt.putLong("petEggXp", xp);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));

        // Cosmetics
        stack.set(DataComponents.CUSTOM_NAME,
            Component.literal(rarity.displayName + " " + type.displayName + " Egg")
                .withStyle(rarity.color, ChatFormatting.BOLD));
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);

        List<Component> lore = new ArrayList<>();
        lore.add(Component.literal("  Affinity: ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(type.affinity.getDisplayName()).withStyle(ChatFormatting.WHITE)));
        lore.add(Component.empty());
        lore.add(Component.literal("  Right-click to hatch!").withStyle(ChatFormatting.YELLOW));
        stack.set(DataComponents.LORE, new ItemLore(lore));

        return stack;
    }

    public static boolean isPetEgg(ItemStack stack) {
        CustomData comp = stack.get(DataComponents.CUSTOM_DATA);
        if (comp == null) return false;
        return comp.copyTag().getBoolean("isPetEgg").orElse(false);
    }

    // ── Entity → PetType mapping ──────────────────────────────────────────────

    private static Optional<PetType> petTypeFor(LivingEntity entity) {
        if (entity instanceof IronGolem) return Optional.of(PetType.IRON_GOLEM);
        if (entity instanceof Bat)       return Optional.of(PetType.BAT);
        if (entity instanceof Fox)       return Optional.of(PetType.FOX);
        if (entity instanceof Rabbit)    return Optional.of(PetType.RABBIT);
        if (entity instanceof Bee)       return Optional.of(PetType.BEE);
        if (entity instanceof Axolotl)   return Optional.of(PetType.AXOLOTL);
        if (entity instanceof Dolphin)   return Optional.of(PetType.DOLPHIN);
        if (entity instanceof Wolf)      return Optional.of(PetType.WOLF);
        if (entity instanceof Spider)    return Optional.of(PetType.SPIDER);
        if (entity instanceof Turtle)    return Optional.of(PetType.TURTLE);
        if (entity instanceof EnderMan)  return Optional.of(PetType.ENDERMAN);
        if (entity instanceof MushroomCow) return Optional.of(PetType.MOOSHROOM);
        if (entity instanceof Chicken)   return Optional.of(PetType.CHICKEN);
        if (entity instanceof Sheep)     return Optional.of(PetType.SHEEP);
        if (entity instanceof Cat)       return Optional.of(PetType.CAT);
        if (entity instanceof Horse)     return Optional.of(PetType.HORSE);
        if (entity instanceof Allay)     return Optional.of(PetType.ALLAY);
        if (entity instanceof Parrot)    return Optional.of(PetType.PARROT);
        return Optional.empty();
    }

    private static PetRarity rollRarity(net.minecraft.util.RandomSource rng) {
        double roll = rng.nextDouble();
        if (roll < 0.02) return PetRarity.EPIC;
        if (roll < 0.10) return PetRarity.RARE;
        if (roll < 0.30) return PetRarity.UNCOMMON;
        return PetRarity.COMMON;
    }
}
