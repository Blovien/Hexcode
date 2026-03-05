package com.riprod.hexcode.core.state.crafting.utils;

import java.util.ArrayList;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.glyphs.component.EffectComponent;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.hexes.component.Hex;
import com.riprod.hexcode.core.common.hexes.component.HexComponent;

public class CraftingGlyphRemover {

    public static void removeLinks(CommandBuffer<EntityStore> accessor,
            Ref<EntityStore> glyphRef, HexComponent hexComp) {
        EffectComponent effect = accessor.getComponent(glyphRef,
                EffectComponent.getComponentType());
        if (effect == null) return;

        Glyph glyph = effect.getGlyph();
        Hex hex = hexComp.getHex();

        for (String nextId : new ArrayList<>(glyph.getNext())) {
            Glyph target = hex.get(nextId);
            if (target != null) {
                target.removePrevious(glyph.getId());
            }
        }
        glyph.getNext().clear();

        for (String prevId : new ArrayList<>(glyph.getPrevious())) {
            Glyph source = hex.get(prevId);
            if (source != null) {
                source.removeNext(glyph.getId());
            }
        }
        glyph.getPrevious().clear();
    }

    public static void removeGlyph(CommandBuffer<EntityStore> accessor,
            Ref<EntityStore> glyphRef, HexComponent hexComp) {
        removeLinks(accessor, glyphRef, hexComp);

        EffectComponent effect = accessor.getComponent(glyphRef,
                EffectComponent.getComponentType());
        if (effect == null) return;

        Ref<EntityStore> nodeRef = effect.getNodeRef();
        if (nodeRef != null && nodeRef.isValid()) {
            accessor.tryRemoveEntity(nodeRef, RemoveReason.REMOVE);
        }

        hexComp.removeChildGlyph(effect.getId());
        hexComp.getHex().remove(effect.getId());

        accessor.tryRemoveEntity(glyphRef, RemoveReason.REMOVE);
    }
}
