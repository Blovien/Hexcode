package com.riprod.hexcode.core.common.triggers;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public interface TriggerSource {

    String key();

    default void onFirstListener(Store<EntityStore> store) {
    }

    default void onLastListener(Store<EntityStore> store) {
    }
}
