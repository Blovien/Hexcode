package com.riprod.hexcode.builtin.glyphs.effect.onrelease;

import java.util.List;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.hexcaster.component.HexcasterComponent;
import com.riprod.hexcode.core.state.execution.component.HexContext;

public class OnReleaseGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        List<String> children = glyph.getNextLinks();
        if (children.isEmpty()) {
            LOGGER.atInfo().log("onrelease: no children to queue");
            return;
        }

        Ref<EntityStore> casterRef = hexContext.getCasterRef();
        if (casterRef == null || !casterRef.isValid()) return;

        HexcasterComponent comp = hexContext.getAccessor().getComponent(
                casterRef, HexcasterComponent.getComponentType());
        if (comp == null) return;

        comp.addPendingRelease(new HexcasterComponent.PendingRelease(children, hexContext));
        LOGGER.atInfo().log("onrelease: queued %d branch(es) for release", children.size());
        // does NOT call continueExecution — children fire on release, not now
    }
}
