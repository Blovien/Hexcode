package com.riprod.hexcode.core.state.crafting.system;

import java.util.List;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.glyphs.component.EffectComponent;
import com.riprod.hexcode.core.common.hexcaster.component.HexcasterComponent;
import com.riprod.hexcode.core.state.casting.utils.GlyphStyler;
import com.riprod.hexcode.core.state.crafting.component.HexcasterCraftingComponent;
import com.riprod.hexcode.core.state.crafting.component.PedestalBlockComponent;
import com.riprod.hexcode.core.common.hover.utils.HoverableUtils;

public class CraftingStateSystem {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String HOVER_PARTICLE = "Object_Hover";

    public static InteractionState enterInteraction(Ref<EntityStore> ref, HexcasterComponent comp,
            CommandBuffer<EntityStore> buffer) {
        return InteractionState.Failed;
    }

     public static void tickInteraction(CommandBuffer<EntityStore> buffer, float dt, Ref<EntityStore> ref,
            PedestalBlockComponent pedestal) {

            }

    public static void tickCrafting(CommandBuffer<EntityStore> buffer, float dt, Ref<EntityStore> ref,
            PedestalBlockComponent pedestal) {
        List<Ref<EntityStore>> previewRefs = pedestal.getHexPreviewRefs();

        if (previewRefs == null || previewRefs.isEmpty()) {
            return;
        }

        HexcasterCraftingComponent craftingComp = buffer.getComponent(ref,
                HexcasterCraftingComponent.getComponentType());
        if (craftingComp == null) {
            return;
        }

        List<Ref<EntityStore>> hexGlyphs = pedestal.getHexPreviewRefs();

        if (hexGlyphs.isEmpty())
            return;

        Ref<EntityStore> targetRef = HoverableUtils.getSmallestTarget(buffer, ref, hexGlyphs, null);
        Ref<EntityStore> hoveredRef = resolveHoveredPreview(targetRef, previewRefs, buffer);

        Ref<EntityStore> previousHovered = craftingComp.getHoveredHexRef();
        boolean changed = (hoveredRef == null) != (previousHovered == null)
                || (hoveredRef != null && !hoveredRef.equals(previousHovered));
        if (changed) {
            LOGGER.atInfo().log("crafting: hover changed from %s to %s", previousHovered, hoveredRef);
            pedestal.setTickLength(HOVER_PARTICLE, 1f);
        }

        craftingComp.setHoveredHexRef(hoveredRef);

        if (pedestal.getTickLength(HOVER_PARTICLE) < 0.5f) {
            pedestal.incrementTickLength(HOVER_PARTICLE, dt);
            return;
        }
        pedestal.setTickLength(HOVER_PARTICLE, 0f);

        if (hoveredRef != null && hoveredRef.isValid()) {
            TransformComponent transform = buffer.getComponent(hoveredRef, TransformComponent.getComponentType());
            if (transform != null) {
                Vector3d pos = transform.getPosition();
                ParticleUtil.spawnParticleEffect(HOVER_PARTICLE, pos, hoveredRef, List.of(ref), buffer);
                GlyphStyler.updateScale(buffer, hoveredRef, 0.5f);
            }
        }
    }

    public static Ref<EntityStore> resolveHoveredPreview(Ref<EntityStore> targetRef,
            List<Ref<EntityStore>> previewRefs, CommandBuffer<EntityStore> buffer) {
        if (targetRef == null || !targetRef.isValid()) {
            return null;
        }

        for (Ref<EntityStore> previewRef : previewRefs) {
            if (previewRef != null && previewRef.equals(targetRef)) {
                return targetRef;
            }
        }

        EffectComponent glyphComp = buffer.getComponent(targetRef, EffectComponent.getComponentType());
        if (glyphComp != null) {
            Ref<EntityStore> hexRef = glyphComp.getHexRef();
            if (hexRef != null && hexRef.isValid()) {
                for (Ref<EntityStore> previewRef : previewRefs) {
                    if (previewRef != null && previewRef.equals(hexRef)) {
                        return hexRef;
                    }
                }
            }
        }

        return null;
    }
}
