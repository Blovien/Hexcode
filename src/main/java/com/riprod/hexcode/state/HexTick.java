package com.riprod.hexcode.state;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.hexcaster.component.HexcasterComponent;

public class HexTick extends EntityTickingSystem<EntityStore> {
  private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

  @Override
  public Query<EntityStore> getQuery() {
    return HexcasterComponent.getComponentType();
  }

  @Override
  public void tick(float dt, int index, ArchetypeChunk<EntityStore> chunk,
      Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {
    HexcasterComponent comp = chunk.getComponent(index,
        HexcasterComponent.getComponentType());

    Ref<EntityStore> ref = chunk.getReferenceTo(index);

    HexState pending = comp.consumePendingState();
    if (pending != null) {
      LOGGER.atInfo().log("%s -> %s", comp.getState(), pending);
      HexcodeManager old = StateRouter.route(comp.getState());
      if (old != null) {
        old.lastTick(ref, comp, store, buffer);
      }

      comp.applyState(pending);

      HexcodeManager next = StateRouter.route(pending);
      if (next != null) {
        next.firstTick(ref, comp, store, buffer);
        return;
      }
    }

    HexcodeManager manager = StateRouter.route(comp.getState());
    if (manager != null) {
      manager.tick(ref, comp, dt, store, buffer);
    }
  }
}
