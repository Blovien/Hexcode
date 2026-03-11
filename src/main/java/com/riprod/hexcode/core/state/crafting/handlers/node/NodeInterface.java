package com.riprod.hexcode.core.state.crafting.handlers.node;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public interface NodeInterface {
    InteractionState enter(CommandBuffer<EntityStore> accessor, Ref<EntityStore> node,
            Ref<EntityStore> playerRef);

    InteractionState drag(CommandBuffer<EntityStore> accessor, Ref<EntityStore> node,
            Ref<EntityStore> playerRef);

    InteractionState drop(CommandBuffer<EntityStore> accessor, Ref<EntityStore> node,
            Ref<EntityStore> playerRef);

    InteractionState click(CommandBuffer<EntityStore> accessor, Ref<EntityStore> node,
            Ref<EntityStore> playerRef);

    InteractionState ability(CommandBuffer<EntityStore> accessor, Ref<EntityStore> node,
            Ref<EntityStore> playerRef);

    Ref<EntityStore> spawnNode(CommandBuffer<EntityStore> accessor, Ref<EntityStore> parentRef, Vector3d rootPos,
            Ref<EntityStore> playerRef);
}
