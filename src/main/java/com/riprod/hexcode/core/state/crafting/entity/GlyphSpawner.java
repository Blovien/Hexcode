package com.riprod.hexcode.core.state.crafting.entity;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.glyphs.component.GlyphComponent;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.utils.CreateGlyph;
import com.riprod.hexcode.core.common.hexes.component.HexComponent;
import com.riprod.hexcode.core.common.hidden.utils.HiddenUtils;
import com.riprod.hexcode.core.common.hover.component.HoverableComponent;
import com.riprod.hexcode.core.common.hover.component.HoverableType;
import com.riprod.hexcode.core.state.crafting.constants.NodeType;
import com.riprod.hexcode.core.state.crafting.handlers.node.NodeRouter;

public class GlyphSpawner {

    public static void spawnCraftingGlyphs(CommandBuffer<EntityStore> buffer,
            HexComponent hexComp, Vector3d hexWorldPos, Ref<EntityStore> playerRef) {
        for (Glyph glyph : hexComp.getHex().getGlyphs()) {
            GlyphComponent effect = new GlyphComponent(glyph);
            effect.setHexRef(hexComp.getSelfRef());

            Vector3f offset = glyph.getPosition();
            Vector3d worldPos = new Vector3d(
                    hexWorldPos.x + offset.x,
                    hexWorldPos.y + offset.y,
                    hexWorldPos.z + offset.z);

            Holder<EntityStore> holder = CreateGlyph.createGlyphHolder(buffer, effect, worldPos);
            HiddenUtils.addHiddenToHolder(buffer, holder, playerRef);
            holder.addComponent(HoverableComponent.getComponentType(),
                    new HoverableComponent(HoverableType.GLYPH));

            Ref<EntityStore> ref = CreateGlyph.createEntity(buffer, holder);
            effect.setSelfRef(ref);
            hexComp.addChildGlyphRef(glyph.getId(), ref);
            Ref<EntityStore> glyphRef = NodeRouter.getHandler(NodeType.Glyph).spawnNode(buffer, ref, worldPos, playerRef);
            effect.setNodeRef(glyphRef);
        }
    }
}
