package com.peakskills.mixin;

import net.minecraft.block.entity.BrewingStandBlockEntity;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Kept as a no-op so the mixin JSON entry remains valid.
 * Alchemy XP is now awarded by BrewingMixin when the player collects a potion.
 */
@Mixin(BrewingStandBlockEntity.class)
public class AlchemyMixin {
}
