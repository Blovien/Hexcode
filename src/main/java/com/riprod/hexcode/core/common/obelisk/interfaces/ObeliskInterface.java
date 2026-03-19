package com.riprod.hexcode.core.common.obelisk.interfaces;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;

public interface ObeliskInterface {
    void onDrawFinish(CommandBuffer<EntityStore> accessor, Ref<EntityStore> playerRef, Glyph drawnGlyph);
    void onDrawStart(CommandBuffer<EntityStore> accessor, Ref<EntityStore> playerRef);
}
