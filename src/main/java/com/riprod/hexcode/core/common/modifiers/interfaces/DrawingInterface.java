package com.riprod.hexcode.core.common.modifiers.interfaces;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public interface DrawingInterface {
    void onDrawFinish(CommandBuffer<EntityStore> accessor, Ref<EntityStore> playerRef);
}
