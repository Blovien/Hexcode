package com.riprod.hexcode.core.state.crafting.handlers.node;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public interface NodeInterface {
  InteractionState enter(CommandBuffer<EntityStore> accessor, Ref<EntityStore> node,
      Ref<EntityStore> playerRef);

  InteractionState tick(CommandBuffer<EntityStore> accessor, Ref<EntityStore> node,
      Ref<EntityStore> playerRef);

  InteractionState exit(CommandBuffer<EntityStore> accessor, Ref<EntityStore> node,
      Ref<EntityStore> playerRef);

  InteractionState click(CommandBuffer<EntityStore> accessor, Ref<EntityStore> node,
      Ref<EntityStore> playerRef);

  InteractionState ability1(CommandBuffer<EntityStore> accessor, Ref<EntityStore> node,
      Ref<EntityStore> playerRef);

  InteractionState ability2(CommandBuffer<EntityStore> accessor, Ref<EntityStore> node,
      Ref<EntityStore> playerRef);

  InteractionState ability3(CommandBuffer<EntityStore> accessor, Ref<EntityStore> node,
      Ref<EntityStore> playerRef);

  Holder<EntityStore> spawnNode(CommandBuffer<EntityStore> accessor, Ref<EntityStore> parentRef, Vector3d position,
      Ref<EntityStore> playerRef);

  void despawn(CommandBuffer<EntityStore> accessor, Ref<EntityStore> nodeRef,
      Ref<EntityStore> playerRef);

  void hover(CommandBuffer<EntityStore> accessor, Ref<EntityStore> nodeRef,
      Ref<EntityStore> playerRef);

  void unhover(CommandBuffer<EntityStore> accessor, Ref<EntityStore> nodeRef,
      Ref<EntityStore> playerRef);
}
