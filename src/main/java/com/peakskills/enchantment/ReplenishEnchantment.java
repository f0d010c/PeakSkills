package com.peakskills.enchantment;

import com.peakskills.PeakSkills;
import com.peakskills.player.PlayerDataManager;
import com.peakskills.skill.Skill;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.block.*;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.Recipe;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ReplenishEnchantment {

    /** itemEntityUUID → playerUUID who broke the crop that spawned it. */
    private static final Map<UUID, UUID> TAGGED_DROPS = new ConcurrentHashMap<>();

    public static final RegistryKey<Enchantment> REPLENISH = RegistryKey.of(
        RegistryKeys.ENCHANTMENT,
        Identifier.of(PeakSkills.MOD_ID, "replenish")
    );

    private static final int MIN_FARMING_LEVEL = 30;

    public static void register() {
        PlayerBlockBreakEvents.AFTER.register(ReplenishEnchantment::onBlockBreak);

        // Unlock the recipe for players who already have Farming 30+ on join
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            if (PlayerDataManager.get(player.getUuid()).getLevel(Skill.FARMING) >= MIN_FARMING_LEVEL) {
                unlockRecipe(player);
            }
        });
    }

    /** Called from XpManager when the Farming skill levels up. */
    public static void onFarmingLevelUp(ServerPlayerEntity player, int from, int to) {
        if (from < MIN_FARMING_LEVEL && to >= MIN_FARMING_LEVEL) {
            unlockRecipe(player);
            player.sendMessage(
                Text.literal("  ✦ Recipe Unlocked: ").formatted(Formatting.GOLD)
                    .append(Text.literal("Replenish I Book").formatted(Formatting.AQUA, Formatting.BOLD))
                    .append(Text.literal(" — surround a Book with Wheat Seeds & Bone Meal.")
                        .formatted(Formatting.GRAY)),
                false);
        }
    }

    private static void unlockRecipe(ServerPlayerEntity player) {
        var recipeId = RegistryKey.of(RegistryKeys.RECIPE,
            Identifier.of(PeakSkills.MOD_ID, "replenish_book"));
        ((ServerWorld) player.getEntityWorld()).getServer().getRecipeManager().get(recipeId)
            .ifPresent(r -> player.unlockRecipes(List.of(r)));
    }

    private static void onBlockBreak(World world, PlayerEntity player, BlockPos pos,
                                     BlockState state, net.minecraft.block.entity.BlockEntity be) {
        if (!(world instanceof ServerWorld serverWorld)) return;
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

        // Must be holding a hoe with Replenish
        ItemStack tool = serverPlayer.getMainHandStack();
        if (!hasReplenish(tool, serverWorld)) return;

        // Farming 30 required
        if (PlayerDataManager.get(serverPlayer.getUuid()).getLevel(Skill.FARMING) < MIN_FARMING_LEVEL) return;

        // Works on any age — replants immediately
        Block block = state.getBlock();
        Item seed = seedFor(block);
        if (seed == null) return;

        // Defer to next tick so item entities from the break are fully registered
        // in the world's entity list before we query/collect them.
        BlockState replantState = resetAge(block, state);
        serverWorld.getServer().execute(() -> {
            // Tag all crop item entities near the broken block as belonging to this player
            UUID playerUuid = serverPlayer.getUuid();
            Box tagBox = new Box(pos).expand(2.0);
            serverWorld.getEntitiesByType(net.minecraft.entity.EntityType.ITEM, tagBox,
                e -> isCropDrop(e.getStack().getItem()))
                .forEach(e -> TAGGED_DROPS.put(e.getUuid(), playerUuid));

            // Consume seed from ground drops first, then player inventory as fallback
            if (consumeSeedDrop(serverWorld, pos, seed, serverPlayer)) {
                serverWorld.setBlockState(pos, replantState);
            }
            magnetCollect(serverWorld, serverPlayer);
        });
    }

    private static boolean hasReplenish(ItemStack stack, ServerWorld world) {
        if (stack.isEmpty()) return false;
        RegistryEntry.Reference<Enchantment> entry =
            world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT).getOrThrow(REPLENISH);
        return EnchantmentHelper.getLevel(entry, stack) > 0;
    }

    private static boolean isMatureCrop(Block block, BlockState state) {
        if (block instanceof CropBlock crop) return crop.isMature(state);
        if (block == Blocks.NETHER_WART) return state.get(NetherWartBlock.AGE) == 3;
        if (block == Blocks.COCOA)       return state.get(CocoaBlock.AGE) == 2;
        return false;
    }

    private static Item seedFor(Block block) {
        if (block == Blocks.WHEAT)      return Items.WHEAT_SEEDS;
        if (block == Blocks.CARROTS)    return Items.CARROT;
        if (block == Blocks.POTATOES)   return Items.POTATO;
        if (block == Blocks.BEETROOTS)  return Items.BEETROOT_SEEDS;
        if (block == Blocks.NETHER_WART) return Items.NETHER_WART;
        if (block == Blocks.COCOA)      return Items.COCOA_BEANS;
        return null;
    }

    private static BlockState resetAge(Block block, BlockState state) {
        if (block instanceof CropBlock)        return state.with(CropBlock.AGE, 0);
        if (block == Blocks.NETHER_WART)       return state.with(NetherWartBlock.AGE, 0);
        if (block == Blocks.COCOA)             return state.with(CocoaBlock.AGE, 0);
        return block.getDefaultState();
    }

    /**
     * Pulls all item entities within 0.75 blocks of the broken crop directly into
     * the player's inventory. Any items that don't fit are left with pickup delay 0
     * so the player can walk over them normally.
     */
    /**
     * Collects all crop drops within 8 blocks of the player directly into their inventory.
     * Scanned from the player position so an entire farm row is covered in one sweep.
     * Only pulls items that match a known crop drop (seeds, produce) to avoid vacuuming
     * unrelated items off the ground.
     */
    private static void magnetCollect(ServerWorld world, ServerPlayerEntity player) {
        UUID playerUuid = player.getUuid();
        double x = player.getX(), y = player.getY(), z = player.getZ();
        Box box = new Box(x - 8, y - 2, z - 8, x + 8, y + 4, z + 8);
        world.getEntitiesByType(net.minecraft.entity.EntityType.ITEM, box,
            e -> !e.isRemoved()
                && isCropDrop(e.getStack().getItem())
                && playerUuid.equals(TAGGED_DROPS.get(e.getUuid())))
            .forEach(entity -> {
                TAGGED_DROPS.remove(entity.getUuid());
                ItemStack stack = entity.getStack();
                player.getInventory().insertStack(stack);
                if (stack.isEmpty()) {
                    entity.discard();
                } else {
                    entity.setPickupDelay(0);
                }
            });
    }

    /** Returns true if this item is a known crop produce or seed. */
    private static boolean isCropDrop(Item item) {
        return item == Items.WHEAT          || item == Items.WHEAT_SEEDS
            || item == Items.CARROT
            || item == Items.POTATO
            || item == Items.BEETROOT       || item == Items.BEETROOT_SEEDS
            || item == Items.NETHER_WART
            || item == Items.COCOA_BEANS;
    }

    /**
     * Consumes one seed for replanting.
     * Priority: ground drops near the block first, then player inventory as fallback.
     * Returns true if a seed was successfully consumed.
     */
    private static boolean consumeSeedDrop(ServerWorld world, BlockPos pos, Item seed, ServerPlayerEntity player) {
        // 1. Try ground drops first (mature crop drops a seed itself)
        Box box = new Box(pos).expand(2.0);
        List<ItemEntity> nearby = world.getEntitiesByType(
            net.minecraft.entity.EntityType.ITEM, box,
            e -> e.getStack().isOf(seed));
        if (!nearby.isEmpty()) {
            ItemEntity entity = nearby.get(0);
            ItemStack stack = entity.getStack();
            if (stack.getCount() <= 1) entity.discard();
            else stack.decrement(1);
            return true;
        }
        // 2. Fallback: consume from player inventory (immature crops have no drop)
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack invStack = player.getInventory().getStack(i);
            if (invStack.isOf(seed)) {
                invStack.decrement(1);
                return true;
            }
        }
        return false;
    }
}
