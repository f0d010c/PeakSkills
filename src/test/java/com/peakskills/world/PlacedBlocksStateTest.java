package com.peakskills.world;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlacedBlocksStateTest {

    @Test
    void playerPlacedBlockCanOnlyBeConsumedOnce() {
        PlacedBlocksState state = new PlacedBlocksState();
        state.markPlaced(123456789L);

        assertTrue(state.consumeIfPlaced(123456789L));
        assertFalse(state.consumeIfPlaced(123456789L));
        assertFalse(state.consumeIfPlaced(987654321L));
    }

    @Test
    void persistedPositionsSurviveCodecRoundTrip() {
        PlacedBlocksState original = new PlacedBlocksState();
        original.markPlaced(42L);
        original.markPlaced(-99L);

        JsonElement encoded = PlacedBlocksState.CODEC.encodeStart(JsonOps.INSTANCE, original).getOrThrow();
        PlacedBlocksState restored = PlacedBlocksState.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();

        assertTrue(restored.consumeIfPlaced(42L));
        assertTrue(restored.consumeIfPlaced(-99L));
        assertFalse(restored.consumeIfPlaced(42L));
    }
}
