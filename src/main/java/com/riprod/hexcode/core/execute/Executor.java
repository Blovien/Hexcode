package com.riprod.hexcode.core.execute;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.glyphs.component.GlyphComponent;

import javax.annotation.Nonnull;

public class Executor {

    private Executor() {
    }

    public static void execute(@Nonnull GlyphComponent spell, @Nonnull Ref<EntityStore> casterRef) {
        // todo: execute compiled spell
    }
}
