package com.peakskills.stat;

import com.peakskills.PeakSkills;
import com.peakskills.pet.PetAbility;
import com.peakskills.pet.PetAbilityRegistry;
import com.peakskills.player.PlayerData;
import com.peakskills.player.PlayerDataManager;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public class StatManager {

    private static final Identifier MODIFIER_ID = Identifier.fromNamespaceAndPath(PeakSkills.MOD_ID, "skill_stats");

    /**
     * Recalculates and applies all stat bonuses to the player.
     * Call this whenever a skill levels up.
     */
    public static void applyStats(ServerPlayer player) {
        float healthBefore = player.getMaxHealth();

        PlayerData data = PlayerDataManager.get(player.getUUID());

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
            AttributeInstance instance = player.getAttribute(stat.getAttribute());
            if (instance == null) continue;

            // Remove old modifier first
            instance.removeModifier(MODIFIER_ID);

            double value = totals.getOrDefault(stat, 0.0);
            if (value == 0.0) continue;

            instance.addPermanentModifier(new AttributeModifier(
                MODIFIER_ID,
                value,
                AttributeModifier.Operation.ADD_VALUE
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
    public static void removeStats(ServerPlayer player) {
        for (Stat stat : Stat.values()) {
            AttributeInstance instance = player.getAttribute(stat.getAttribute());
            if (instance != null) instance.removeModifier(MODIFIER_ID);
        }
    }
}
