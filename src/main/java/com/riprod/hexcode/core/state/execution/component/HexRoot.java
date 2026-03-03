package com.riprod.hexcode.core.state.execution.component;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public interface HexRoot {
    boolean isAlive();
    Ref<EntityStore> getSourceRef();
    Ref<EntityStore> getRootEntityRef();
}
