package com.peakskills.player;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.peakskills.MinecraftTestBootstrap;
import com.peakskills.collection.CollectionType;
import com.peakskills.pet.PetInstance;
import com.peakskills.pet.PetRarity;
import com.peakskills.pet.PetType;
import com.peakskills.skill.Skill;
import com.peakskills.fishing.FishingDepth;
import com.peakskills.fishing.FishingMood;
import com.peakskills.fishing.FishingOutcomeCategory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerDataSerializationTest {

    @BeforeAll
    static void initializeMinecraft() {
        MinecraftTestBootstrap.initializeRegistries();
    }

    @Test
    void roundTripPreservesProgressionAndPreferences() {
        UUID playerId = UUID.randomUUID();
        PlayerData original = new PlayerData(playerId);
        original.addXp(Skill.MINING, 12_345);
        original.setMaxMana(275);
        original.setPetsVisible(false);
        original.setLimitBurstLevelUpSounds(false);

        PetInstance pet = new PetInstance(UUID.randomUUID(), PetType.BEE, PetRarity.COMMON, 42);
        original.getPetRoster().addPet(pet);
        original.getPetRoster().setActivePet(pet.getId());

        CollectionType collection = CollectionType.values()[0];
        original.getCollections().getCounts().put(collection, 321L);
        original.getCollections().getUnlockedTiers().put(collection, 1);
        original.getFishingJournal().loadTotals(12, 27);
        original.getFishingJournal().loadCategory(FishingOutcomeCategory.FISH, 9);
        original.getFishingJournal().loadDepth(FishingDepth.DEEP_WATER);
        original.getFishingJournal().loadMood(FishingMood.TREASURE_RIPPLE);
        original.getFishingJournal().loadLoot("sea_crystal");
        original.getFishingJournal().loadBiome("minecraft:ocean");

        PlayerData restored = PlayerDataManager.fromJson(playerId, PlayerDataManager.toJson(original));

        assertEquals(12_345, restored.getXp(Skill.MINING));
        assertEquals(275, restored.getMaxMana());
        assertFalse(restored.isPetsVisible());
        assertFalse(restored.shouldLimitBurstLevelUpSounds());
        assertEquals(321, restored.getCollections().getCount(collection));
        assertEquals(1, restored.getCollections().getUnlockedTier(collection));
        assertEquals(12, restored.getFishingJournal().getTotalCatches());
        assertEquals(27, restored.getFishingJournal().getTotalItems());
        assertEquals(9, restored.getFishingJournal().getCategoryCatches(FishingOutcomeCategory.FISH));
        assertTrue(restored.getFishingJournal().getDepths().contains(FishingDepth.DEEP_WATER));
        assertTrue(restored.getFishingJournal().getMoods().contains(FishingMood.TREASURE_RIPPLE));
        assertTrue(restored.getFishingJournal().hasDiscovered("sea_crystal"));
        assertTrue(restored.getFishingJournal().getBiomes().contains("minecraft:ocean"));

        PetInstance restoredPet = restored.getPetRoster().getActivePet().orElseThrow();
        assertEquals(pet.getId(), restoredPet.getId());
        assertEquals(PetType.BEE, restoredPet.getType());
        assertEquals(42, restoredPet.getXp());
    }

    @Test
    void legacyEmptyJsonUsesSafeDefaults() {
        PlayerData restored = PlayerDataManager.fromJson(UUID.randomUUID(), new JsonObject());

        assertEquals(1, restored.getDataVersion());
        assertEquals(100, restored.getMaxMana());
        assertTrue(restored.isPetsVisible());
        assertTrue(restored.shouldLimitBurstLevelUpSounds());
        assertTrue(restored.getPetRoster().getPets().isEmpty());
    }

    @Test
    void atomicWriteReplacesFileAndCleansTemporaryFile(@TempDir Path directory) throws Exception {
        Path file = directory.resolve("player.json");
        Files.writeString(file, "{\"stale\":true}");
        JsonObject replacement = new JsonObject();
        replacement.addProperty("dataVersion", PlayerData.DATA_VERSION);

        PlayerDataManager.writeJsonAtomic(file, replacement);

        try (Reader reader = Files.newBufferedReader(file)) {
            assertEquals(PlayerData.DATA_VERSION,
                JsonParser.parseReader(reader).getAsJsonObject().get("dataVersion").getAsInt());
        }
        try (var files = Files.list(directory)) {
            assertEquals(1, files.count());
        }
    }
}
