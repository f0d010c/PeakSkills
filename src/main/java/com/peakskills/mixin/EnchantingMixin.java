package com.peakskills.mixin;

import com.peakskills.player.PlayerDataManager;
import com.peakskills.skill.Skill;
import com.peakskills.xp.XpManager;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.EnchantmentScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnchantmentScreenHandler.class)
public class EnchantingMixin {

    @Inject(method = "onButtonClick", at = @At("RETURN"))
    private void onEnchant(PlayerEntity player, int id, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) return;
        if (!(player instanceof ServerPlayerEntity sp)) return;

        int enchLevel = PlayerDataManager.get(sp.getUuid()).getLevel(Skill.ENCHANTING);

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

        EnchantmentScreenHandler handler = (EnchantmentScreenHandler)(Object) this;
        ItemStack item = handler.getSlot(0).getStack();
        if (item.isEmpty()) return;

        ItemEnchantmentsComponent enchants =
            item.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
        if (enchants.isEmpty()) return;

        ItemEnchantmentsComponent.Builder builder = new ItemEnchantmentsComponent.Builder(enchants);
        RegistryEntry<?> upgradedEntry = null;
        int newLevel = 0;

        for (var entry : enchants.getEnchantments()) {
            int current = enchants.getLevel(entry);
            int max     = entry.value().getMaxLevel();
            if (current < max) {
                newLevel = current + 1;
                builder.add(entry, newLevel);
                upgradedEntry = entry;
                break; // one upgrade per enchant event
            }
        }

        if (upgradedEntry != null) {
            item.set(DataComponentTypes.ENCHANTMENTS, builder.build());

            // Chat message with enchantment name and level transition
            int oldLevel = newLevel - 1;
            sp.sendMessage(
                Text.literal("✦ Arcane Mastery!  ").formatted(Formatting.GOLD, Formatting.BOLD)
                    .append(upgradedEntry.value().description().copy().formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD))
                    .append(Text.literal("  " + toRoman(oldLevel) + " → " + toRoman(newLevel))
                        .formatted(Formatting.WHITE)),
                false
            );

            // Distinct sound — higher pitch than skill level-up (1.0f)
            sp.networkHandler.sendPacket(new PlaySoundS2CPacket(
                RegistryEntry.of(SoundEvents.ENTITY_PLAYER_LEVELUP),
                SoundCategory.PLAYERS,
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
