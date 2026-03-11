package com.riprod.hexcode.core.state.crafting.utils;

import java.util.List;

import javax.annotation.Nullable;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphComponent;
import com.riprod.hexcode.core.common.hexes.component.HexComponent;
import com.riprod.hexcode.core.state.crafting.component.NodeComponent;
import com.riprod.hexcode.core.state.crafting.component.PedestalBlockComponent;
import com.riprod.hexcode.core.state.crafting.component.PedestalDataComponent;
import com.riprod.hexcode.core.state.crafting.component.SlotComponent;
import com.riprod.hexcode.core.state.crafting.constants.CraftingColors;
import com.riprod.hexcode.utils.VfxUtil;

public class LinkRenderer {

    private static final double LINK_THICKNESS = 0.05;

    public static void renderLinks(CommandBuffer<EntityStore> accessor, PedestalDataComponent playerData,
            PedestalBlockComponent pedestal, float dt, @Nullable Ref<EntityStore> playerRef) {
        Ref<EntityStore> hexRootRef = playerData.getAnchorRef();
        if (hexRootRef == null)
            return;

        if (pedestal.getTickLength("LINK_RENDER") < 0) {
            pedestal.incrementTickLength("LINK_RENDER", dt);
            return;
        }
        pedestal.setTickLength("LINK_RENDER", -0.5f);

        HexComponent hexComp = accessor.getComponent(hexRootRef,
                HexComponent.getComponentType());
        if (hexComp == null)
            return;

        World world = accessor.getExternalData().getWorld();

        LinkRenderer.renderAllLinks(accessor, hexComp, world, playerData.getAnchorNodeRef(), playerRef);

        renderSlotValueLinks(accessor, hexComp, world, playerData.getSlotNodeRefs(), playerRef);
    }

    public static void renderActiveLink(ComponentAccessor<EntityStore> accessor,
            World world, Vector3d source, Vector3d target, Vector3f color) {
        VfxUtil.line(accessor, world, source, target, color, LINK_THICKNESS, 0.04f, false);
    }

    public static void renderAllLinks(ComponentAccessor<EntityStore> accessor,
            HexComponent hexComp, World world, Ref<EntityStore> rootNodeRef, @Nullable Ref<EntityStore> playerRef) {

        String firstId = hexComp.getHex().getFirstGlyphId();
        if (rootNodeRef != null && rootNodeRef.isValid() && firstId != null) {
            Ref<EntityStore> firstGlyphRef = hexComp.getChildGlyphRef(firstId);
            if (firstGlyphRef != null && firstGlyphRef.isValid()) {
                TransformComponent rootTransform = accessor.getComponent(rootNodeRef,
                        TransformComponent.getComponentType());
                TransformComponent firstTransform = accessor.getComponent(firstGlyphRef,
                        TransformComponent.getComponentType());
                if (rootTransform != null && firstTransform != null) {
                    VfxUtil.line(accessor, world, rootTransform.getPosition(),
                            firstTransform.getPosition(), CraftingColors.ANCHOR, LINK_THICKNESS, 1f, false, playerRef);
                }
            }
        }

        List<Glyph> glyphs = hexComp.getHex().getGlyphs();

        for (Glyph glyph : glyphs) {
            Ref<EntityStore> sourceRef = hexComp.getChildGlyphRef(glyph.getId());
            if (sourceRef == null || !sourceRef.isValid())
                continue;

            TransformComponent sourceTransform = accessor.getComponent(sourceRef,
                    TransformComponent.getComponentType());
            if (sourceTransform == null)
                continue;

            for (String nextId : glyph.getNext()) {
                Ref<EntityStore> targetRef = hexComp.getChildGlyphRef(nextId);
                if (targetRef == null || !targetRef.isValid())
                    continue;

                TransformComponent targetTransform = accessor.getComponent(targetRef,
                        TransformComponent.getComponentType());
                if (targetTransform == null)
                    continue;

                VfxUtil.line(accessor, world, sourceTransform.getPosition(),
                        targetTransform.getPosition(), CraftingColors.GLYPH_LINK, LINK_THICKNESS, 1f, false, playerRef);
            }

        }
    }

    private static void renderSlotValueLinks(ComponentAccessor<EntityStore> accessor,
            HexComponent hexComp, World world,
            @Nullable List<Ref<EntityStore>> slotRefs, @Nullable Ref<EntityStore> playerRef) {
        if (slotRefs == null || slotRefs.isEmpty())
            return;

        for (Ref<EntityStore> slotRef : slotRefs) {
            if (slotRef == null || !slotRef.isValid())
                continue;

            SlotComponent slotComp = accessor.getComponent(slotRef, SlotComponent.getComponentType());
            NodeComponent nodeComp = accessor.getComponent(slotRef, NodeComponent.getComponentType());
            if (slotComp == null || nodeComp == null)
                continue;

            Ref<EntityStore> parentGlyphRef = nodeComp.getParentEntity();
            if (parentGlyphRef == null || !parentGlyphRef.isValid())
                continue;

            GlyphComponent glyphComp = accessor.getComponent(parentGlyphRef, GlyphComponent.getComponentType());
            if (glyphComp == null)
                continue;

            String valueGlyphId = glyphComp.getGlyph().getInputs().get(slotComp.getSlotKey());
            if (valueGlyphId == null)
                continue;

            Ref<EntityStore> valueRef = hexComp.getChildGlyphRef(valueGlyphId);
            if (valueRef == null || !valueRef.isValid())
                continue;

            TransformComponent slotTransform = accessor.getComponent(slotRef, TransformComponent.getComponentType());
            TransformComponent valueTransform = accessor.getComponent(valueRef, TransformComponent.getComponentType());
            if (slotTransform == null || valueTransform == null)
                continue;

            VfxUtil.line(accessor, world, slotTransform.getPosition(),
                    valueTransform.getPosition(), CraftingColors.INPUT, LINK_THICKNESS, 1f, false, playerRef);
        }
    }
}
