package com.riprod.hexcode.core.common.modifiers.interfaces;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.state.HexState;

public interface ModStateInterface {
    void onStateChange(CommandBuffer<EntityStore> accessor, Ref<EntityStore> playerRef, HexState newState, HexState oldState);
}
