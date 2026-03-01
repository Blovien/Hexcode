package com.riprod.hexcode.core.crafting.events;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.event.events.ecs.DamageBlockEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class ObeliskBlockEvent extends EntityEventSystem<EntityStore, BreakBlockEvent> {

    private static final Set<Vector3i> protectedPositions = new HashSet<>();

    public static void protect(Vector3i pos) {
        protectedPositions.add(pos);
    }

    public static void unprotect(Vector3i pos) {
        protectedPositions.remove(pos);
    }

    public ObeliskBlockEvent() {
        super(BreakBlockEvent.class);
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> chunk,
            @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> buffer,
            @Nonnull BreakBlockEvent event) {
        if (protectedPositions.contains(event.getTargetBlock())) {
            event.setCancelled(true);
        }
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }
}
