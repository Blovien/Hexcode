package com.riprod.hexcode.builtin.listeners;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.WorldEventSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.api.event.HexCastEvent;
import com.riprod.hexcode.core.common.hexes.component.Hex;
import com.riprod.hexcode.core.state.execution.events.CastingEventData;

public class HexCastDiagnosticListener extends WorldEventSystem<EntityStore, HexCastEvent> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public HexCastDiagnosticListener() {
        super(HexCastEvent.class);
    }

    @Override
    public void handle(@Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> buffer,
                       @Nonnull HexCastEvent event) {
        CastingEventData data = event.getCastingData();
        Hex hex = event.getHex();
        String firstGlyph = hex != null ? hex.getFirstGlyphId() : "<null>";
        LOGGER.atInfo().log(
                "[cast] wielder=%s target=%s firstGlyph=%s mana=%s cancelled=%s",
                event.getWielderRef(),
                event.getTargetRef(),
                firstGlyph,
                data != null ? data.getManaCost() : "<null>",
                event.isCancelled());
    }
}
