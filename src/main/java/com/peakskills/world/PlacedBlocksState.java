package com.peakskills.world;

import com.mojang.serialization.Codec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Persists the set of player-placed block positions across server restarts.
 * Saved automatically to: world/data/peakskills_placed_blocks.dat
 *
 * A position in this set = "a player placed a block here".
 * Breaking it removes the record and awards NO XP.
 * Natural world blocks (not in the set) award XP normally.
 */
public class PlacedBlocksState extends PersistentState {

    private static final String ID = "peakskills_placed_blocks";

    // ConcurrentHashMap-backed set — block events fire on the server thread
    // but this keeps things safe if async tasks ever touch it.
    private final Set<Long> blocks = ConcurrentHashMap.newKeySet();

    // ── Public API ────────────────────────────────────────────────────────────

    /** Record that a player placed a block at this packed BlockPos. */
    public void markPlaced(long posKey) {
        blocks.add(posKey);
        markDirty();
    }

    /**
     * Atomically check-and-remove.
     * Returns true  → was player-placed, do NOT award XP, record consumed.
     * Returns false → natural block, award XP normally.
     */
    public boolean consumeIfPlaced(long posKey) {
        boolean wasPlaced = blocks.remove(posKey);
        if (wasPlaced) markDirty();
        return wasPlaced;
    }

    // ── Codec (1.21.x PersistentState uses Codec instead of writeNbt) ─────────

    // Serialise as a flat list of longs — compact and fast.
    public static final Codec<PlacedBlocksState> CODEC = Codec.LONG.listOf()
        .xmap(
            list -> {
                PlacedBlocksState s = new PlacedBlocksState();
                s.blocks.addAll(list);
                return s;
            },
            s -> new ArrayList<>(s.blocks)
        );

    public static final PersistentStateType<PlacedBlocksState> TYPE =
        new PersistentStateType<>(ID, PlacedBlocksState::new, CODEC, null);

    // ── Accessor ──────────────────────────────────────────────────────────────

    /** Retrieve (or create) the shared instance for this server. */
    public static PlacedBlocksState get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(TYPE);
    }
}
