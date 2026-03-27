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
import com.riprod.hexcode.core.common.pedestal.component.PedestalBlockComponent;
import com.riprod.hexcode.core.state.crafting.component.NodeComponent;
import com.riprod.hexcode.core.state.crafting.component.CraftingDataComponent;
import com.riprod.hexcode.core.common.glyphs.utils.GlyphSlotType;
import com.riprod.hexcode.core.state.crafting.component.SlotComponent;
import com.hypixel.hytale.server.core.modules.debug.DebugUtils;
import com.riprod.hexcode.core.state.crafting.constants.CraftingColors;
import com.riprod.hexcode.utils.VfxUtil;

public class LinkRenderer {

    private static final double LINK_THICKNESS = 0.05;

    public static void renderLinks(CommandBuffer<EntityStore> accessor, CraftingDataComponent playerData,
            PedestalBlockComponent pedestal, float dt, @Nullable Ref<EntityStore> playerRef) {
        Ref<EntityStore> hexRootRef = playerData.getAnchorRef();
        if (hexRootRef == null || !hexRootRef.isValid())
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

        Vector3f glyphLinkColor = playerData.getGlyphColor();
        com.hypixel.hytale.protocol.Color glyphParticleColor = playerData.getGlyphProtocolColor();

        VfxUtil.advanceFlowPhase();

        LinkRenderer.renderAllLinks(accessor, hexComp, world, playerData.getAnchorNodeRef(),
                playerRef, glyphLinkColor, glyphParticleColor);

        renderSlotValueLinks(accessor, hexComp, world, playerData.getSlotNodeRefs(), playerRef);
    }

    public static void renderActiveLink(ComponentAccessor<EntityStore> accessor,
            World world, Vector3d source, Vector3d target, Vector3f color) {
        VfxUtil.line(accessor, world, source, target, color, LINK_THICKNESS, 0.04f, DebugUtils.FLAG_NONE);
    }

    public static void renderAllLinks(ComponentAccessor<EntityStore> accessor,
            HexComponent hexComp, World world, Ref<EntityStore> rootNodeRef,
            @Nullable Ref<EntityStore> playerRef, Vector3f glyphLinkColor,
            @Nullable com.hypixel.hytale.protocol.Color glyphParticleColor) {

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
                            firstTransform.getPosition(), CraftingColors.ANCHOR, LINK_THICKNESS, 1f, DebugUtils.FLAG_NONE, playerRef);
                    VfxUtil.particleAlongPath("Link_Flow", rootTransform.getPosition(),
                            firstTransform.getPosition(), 3, accessor);
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
                        targetTransform.getPosition(), glyphLinkColor, LINK_THICKNESS, 1f, DebugUtils.FLAG_NONE, playerRef);
                if (glyphParticleColor != null) {
                    VfxUtil.particleAlongPath("Link_Flow", sourceTransform.getPosition(),
                            targetTransform.getPosition(), 3, glyphParticleColor, playerRef, accessor);
                } else {
                    VfxUtil.particleAlongPath("Link_Flow", sourceTransform.getPosition(),
                            targetTransform.getPosition(), 3, accessor);
                }
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

            boolean isOutput = slotComp.getSlotType() == GlyphSlotType.Output;
            String valueGlyphId = isOutput
                    ? glyphComp.getGlyph().getOutputs().get(slotComp.getSlotKey())
                    : glyphComp.getGlyph().getInputs().get(slotComp.getSlotKey());
            if (valueGlyphId == null)
                continue;

            Ref<EntityStore> valueRef = hexComp.getChildGlyphRef(valueGlyphId);
            if (valueRef == null || !valueRef.isValid())
                continue;

            TransformComponent slotTransform = accessor.getComponent(slotRef, TransformComponent.getComponentType());
            TransformComponent valueTransform = accessor.getComponent(valueRef, TransformComponent.getComponentType());
            if (slotTransform == null || valueTransform == null)
                continue;

            Vector3f color = isOutput ? CraftingColors.OUTPUT : CraftingColors.INPUT;
            VfxUtil.line(accessor, world, slotTransform.getPosition(),
                    valueTransform.getPosition(), color, LINK_THICKNESS, 1f, DebugUtils.FLAG_NONE, playerRef);
            VfxUtil.particleAlongPath("Slot_Flow", slotTransform.getPosition(),
                    valueTransform.getPosition(), 3, accessor);
        }
    }
}
