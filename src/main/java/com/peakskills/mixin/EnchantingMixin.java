package com.peakskills.mixin;

import com.peakskills.player.PlayerDataManager;
import com.peakskills.skill.Skill;
import com.peakskills.xp.XpManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnchantmentMenu.class)
public class EnchantingMixin {

    @Inject(method = "clickMenuButton", at = @At("RETURN"))
    private void onEnchant(Player player, int id, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) return;
        if (!(player instanceof ServerPlayer sp)) return;

        int enchLevel = PlayerDataManager.get(sp.getUUID()).getLevel(Skill.ENCHANTING);

        // XP scales with skill level so early actions aren't explosive but end-game stays viable.
        // Tier 0: 100 + 7*level  →  L1: 107,  L50: 450,  L99: 793
        // Tier 1: 170 + 12*level →  L1: 182,  L50: 770,  L99: 1358
        // Tier 2: 250 + 17*level →  L1: 267,  L50: 1100, L99: 1933
        long xp = switch (id) {
            case 0  -> 100L + enchLevel * 7L;
            case 1  -> 170L + enchLevel * 12L;
            default -> 250L + enchLevel * 17L;
        };
        XpManager.addXp(sp, Skill.ENCHANTING, xp);

        // ── Enchantment upgrade chance ────────────────────────────────────────
        // Higher Enchanting level = chance to bump one existing enchantment +1 level.
        // Chance: 0.4% per level  →  ~4% at level 10, ~20% at level 50, ~40% at level 99.
        double upgradeChance = enchLevel * 0.004;
        if (upgradeChance <= 0 || sp.getRandom().nextDouble() >= upgradeChance) return;

        EnchantmentMenu handler = (EnchantmentMenu)(Object) this;
        ItemStack item = handler.getSlot(0).getItem();
        if (item.isEmpty()) return;

        ItemEnchantments enchants =
            item.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (enchants.isEmpty()) return;

        ItemEnchantments.Mutable builder = new ItemEnchantments.Mutable(enchants);
        Holder<Enchantment> upgradedEntry = null;
        int newLevel = 0;

        for (var entry : enchants.keySet()) {
            int current = enchants.getLevel(entry);
            int max     = entry.value().getMaxLevel();
            if (current < max) {
                newLevel = current + 1;
                builder.upgrade(entry, newLevel);
                upgradedEntry = entry;
                break; // one upgrade per enchant event
            }
        }

        if (upgradedEntry != null) {
            item.set(DataComponents.ENCHANTMENTS, builder.toImmutable());

            // Chat message with enchantment name and level transition
            int oldLevel = newLevel - 1;
            sp.sendSystemMessage(
                Component.literal("✦ Arcane Mastery!  ").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                    .append(upgradedEntry.value().description().copy().withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD))
                    .append(Component.literal("  " + toRoman(oldLevel) + " → " + toRoman(newLevel))
                        .withStyle(ChatFormatting.WHITE)),
                false
            );

            // Distinct sound — higher pitch than skill level-up (1.0f)
            sp.connection.send(new ClientboundSoundPacket(
                Holder.direct(SoundEvents.PLAYER_LEVELUP),
                SoundSource.PLAYERS,
                sp.getX(), sp.getY(), sp.getZ(),
                0.6f, 1.8f, 0L
            ));
        }
    }

    private static String toRoman(int n) {
        String[] sym = {"M","CM","D","CD","C","XC","L","XL","X","IX","V","IV","I"};
        int[]    val = {1000,900,500,400,100, 90, 50, 40, 10,  9,  5,  4,  1};
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < val.length; i++)
            while (n >= val[i]) { sb.append(sym[i]); n -= val[i]; }
        return sb.toString();
    }
}
