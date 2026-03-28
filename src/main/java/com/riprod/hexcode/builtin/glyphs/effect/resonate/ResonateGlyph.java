package com.riprod.hexcode.builtin.glyphs.effect.resonate;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.builtin.glyphs.effect.resonate.style.ResonateStyle;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.registry.GlyphAsset;
import com.riprod.hexcode.core.common.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.core.state.execution.component.HexSignal;
import com.riprod.hexcode.core.state.execution.component.VolatilityTracker;

public class ResonateGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "Glyph_Resonate";

    private static final int MAX_ENTRIES = 4;

    @Override
    public boolean resolveMana(Glyph glyph, HexContext hexContext) {
        GlyphAsset asset = GlyphAsset.getAssetMap().getAsset(glyph.getGlyphId());
        if (asset == null) return true;

        float baseCost = asset.getManaConsumption()
                * ((1 - glyph.getEfficiency()) * 0.25f + 0.75f);

        VolatilityTracker tracker = hexContext.getVolatilityTracker();
        float castMultiplier = (tracker != null) ? tracker.getManaCostMultiplier() : 1.0f;
        float finalCost = baseCost * castMultiplier;

        return hexContext.getRoot().tryConsumeMana(finalCost, hexContext.getAccessor());
    }

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexVar targetVar = glyph.resolveInput("target", hexContext);
        if (!(targetVar instanceof EntityVar entityVar)) {
            LOGGER.atInfo().log("resonate: targets must be entities with hex signals");
            return;
        }

        if (glyph.getNext() == null || glyph.getNext().isEmpty()) {
            LOGGER.atInfo().log("resonate: no children to inject");
            return;
        }

        CommandBuffer<EntityStore> accessor = hexContext.getAccessor();

        Ref<EntityStore> ref = entityVar.getRef(accessor);
        if (ref == null || !ref.isValid()) return;

        HexSignal signal = accessor.getComponent(ref, HexSignal.getComponentType());
        if (signal == null) {
            Vector3d pos = resolvePosition(ref, accessor);
            if (pos != null) ResonateStyle.renderNoSignal(pos, hexContext.getColors(), accessor);
            LOGGER.atInfo().log("resonate: target has no hex signal, skipping");
            return;
        }

        if (signal.getEntries().size() >= MAX_ENTRIES) {
            LOGGER.atInfo().log("resonate: signal already at max entries (%d), skipping", MAX_ENTRIES);
            return;
        }

        HexSignal.SignalEntry newEntry = new HexSignal.SignalEntry(
                hexContext.copy(),
                hexContext.getRoot().getRootEntityRef(),
                glyph,
                glyph.getNext(),
                null);
        signal.addEntry(newEntry);

        Vector3d pos = resolvePosition(ref, accessor);
        if (pos != null) ResonateStyle.renderResonate(pos, hexContext.getColors(), accessor);
        LOGGER.atInfo().log("resonate: appended signal entry (%d total)", signal.getEntries().size());

        // resonate does not call continueExecution — children are the injected payload
    }

    private Vector3d resolvePosition(Ref<EntityStore> ref, CommandBuffer<EntityStore> accessor) {
        TransformComponent tc = accessor.getComponent(ref, TransformComponent.getComponentType());
        return tc != null ? tc.getPosition() : null;
    }
}
