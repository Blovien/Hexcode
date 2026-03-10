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
import com.riprod.hexcode.core.common.hexes.component.HexComponent;
import com.riprod.hexcode.core.state.crafting.component.HexcasterCraftingComponent;
import com.riprod.hexcode.core.state.crafting.component.PedestalBlockComponent;
import com.riprod.hexcode.utils.VfxUtil;

public class LinkRenderer {

    private static final Vector3f LINK_COLOR = new Vector3f(0.3f, 0.7f, 1.0f);
    private static final Vector3f ROOT_LINK_COLOR = new Vector3f(1.0f, 0.6f, 0.2f);

    private static final double LINK_THICKNESS = 0.05;

    public static void renderLinks(CommandBuffer<EntityStore> accessor, HexcasterCraftingComponent craftingComp,
            PedestalBlockComponent pedestal, float dt, @Nullable Ref<EntityStore> playerRef) {
        Ref<EntityStore> hexRootRef = craftingComp.getHexRootRef();
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

        LinkRenderer.renderAllLinks(accessor, hexComp,
                accessor.getExternalData().getWorld(), craftingComp.getRootNodeRef(), playerRef);

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
                            firstTransform.getPosition(), ROOT_LINK_COLOR, LINK_THICKNESS, 1f, false, playerRef);
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
                        targetTransform.getPosition(), LINK_COLOR, LINK_THICKNESS, 1f, false, playerRef);
            }
        }
    }
}
