package com.peakskills.stat;

import com.peakskills.PeakSkills;
import com.peakskills.pet.PetAbility;
import com.peakskills.pet.PetAbilityRegistry;
import com.peakskills.player.PlayerData;
import com.peakskills.player.PlayerDataManager;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public class StatManager {

    private static final Identifier MODIFIER_ID = Identifier.of(PeakSkills.MOD_ID, "skill_stats");

    /**
     * Recalculates and applies all stat bonuses to the player.
     * Call this whenever a skill levels up.
     */
    public static void applyStats(ServerPlayerEntity player) {
        float healthBefore = player.getMaxHealth();

        PlayerData data = PlayerDataManager.get(player.getUuid());

        // Sum contributions per stat
        Map<Stat, Double> totals = new HashMap<>();
        for (SkillStatSource source : StatRegistry.SOURCES) {
            int level = data.getLevel(source.skill());
            totals.merge(source.stat(), source.compute(level), Double::sum);
        }

        // Add collection stat bonuses
        data.getCollections().computeStatBonuses()
            .forEach((stat, value) -> totals.merge(stat, value, Double::sum));

        // Add active pet stat bonuses on top of skill bonuses
        data.getPetRoster().getActivePet().ifPresent(pet ->
            PetAbilityRegistry.getAbilities(pet.getType()).stream()
                .filter(a -> a.type == PetAbility.Type.STAT_BONUS)
                .forEach(a -> totals.merge(a.stat, a.compute(pet.getLevel(), pet.getRarity()), Double::sum))
        );

        // Apply to attributes
        for (Stat stat : Stat.values()) {
            EntityAttributeInstance instance = player.getAttributeInstance(stat.getAttribute());
            if (instance == null) continue;

            // Remove old modifier first
            instance.removeModifier(MODIFIER_ID);

            double value = totals.getOrDefault(stat, 0.0);
            if (value == 0.0) continue;

            instance.addPersistentModifier(new EntityAttributeModifier(
                MODIFIER_ID,
                value,
                EntityAttributeModifier.Operation.ADD_VALUE
            ));
        }

        // Fill any new hearts gained from skill progression.
        // Scales current health up by the same delta so new hearts appear full.
        float healthAfter = player.getMaxHealth();
        float delta = healthAfter - healthBefore;
        if (delta > 0) {
            player.setHealth(Math.min(healthAfter, player.getHealth() + delta));
        } else if (player.getHealth() > healthAfter) {
            player.setHealth(healthAfter);
        }
    }

    /**
     * Remove all PeakSkills stat modifiers from a player (e.g. on disconnect).
     */
    public static void removeStats(ServerPlayerEntity player) {
        for (Stat stat : Stat.values()) {
            EntityAttributeInstance instance = player.getAttributeInstance(stat.getAttribute());
            if (instance != null) instance.removeModifier(MODIFIER_ID);
        }
    }
}
