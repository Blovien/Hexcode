package com.riprod.hexcode.builtin.listeners;

import java.util.function.Consumer;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.api.event.GlyphFizzleEvent;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.registry.GlyphAsset;
import com.riprod.hexcode.core.state.execution.component.HexContext;

public class FizzleMessageListener implements Consumer<GlyphFizzleEvent> {

    @Override
    public void accept(GlyphFizzleEvent event) {
        HexContext ctx = event.getCtx();
        if (ctx == null || ctx.getAccessor() == null) return;
        var root = ctx.getRoot();
        Ref<EntityStore> caster = root.getSourceRef();
        if (caster == null || !caster.isValid()) return;
        PlayerRef pr = ctx.getAccessor().getComponent(caster, PlayerRef.getComponentType());
        if (pr == null) return;
        pr.sendMessage(Message.raw(resolveTitle(event.getGlyph()) + " fizzled!"));
    }

    private static String resolveTitle(Glyph glyph) {
        if (glyph == null) return "Glyph";
        GlyphAsset asset = GlyphAsset.getAssetMap().getAsset(glyph.getGlyphId());
        return (asset != null && asset.getTitle() != null) ? asset.getTitle() : glyph.getGlyphId();
    }
}
