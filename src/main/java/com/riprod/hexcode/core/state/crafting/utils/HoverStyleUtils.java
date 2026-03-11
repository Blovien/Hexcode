package com.riprod.hexcode.core.state.crafting.utils;

import java.util.List;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.glyphs.component.GlyphComponent;
import com.riprod.hexcode.core.common.hover.component.HoverableComponent;
import com.riprod.hexcode.core.common.utilities.component.DebugComponent;
import com.riprod.hexcode.core.state.casting.utils.GlyphStyler;
import com.riprod.hexcode.core.state.crafting.component.PedestalBlockComponent;

public class HoverStyleUtils {

    private static final String HOVER_PARTICLE = "Object_Hover";

    public static void unhover(CommandBuffer<EntityStore> accessor, Ref<EntityStore> unhoveredRef) {

        if (unhoveredRef == null || !unhoveredRef.isValid())
            return;

        HoverableComponent hoveredComponent = accessor.getComponent(unhoveredRef,
                HoverableComponent.getComponentType());
        if (hoveredComponent == null)
            return;

        switch (hoveredComponent.getType()) {
            case GLYPH: {
                GlyphComponent prevEffect = accessor.getComponent(unhoveredRef,
                        GlyphComponent.getComponentType());
                if (prevEffect == null)
                    return;
                GlyphStyler.exitGlyphHover(accessor, prevEffect);
            }
                break;
            case NODE: {
                DebugComponent debug = accessor.getComponent(unhoveredRef, DebugComponent.getComponentType());
                if (debug == null)
                    return;
                debug.resetScaleMultiplier();
                debug.resetFadeMultipler();
                debug.resetIntervalMultiplier();
            }
                break;
            case CONTAINER: {
                DebugComponent debug = accessor.getComponent(unhoveredRef, DebugComponent.getComponentType());
                if (debug == null)
                    return;

                debug.resetIntervalMultiplier();
                debug.resetFadeMultipler();
                debug.resetScaleMultiplier();
            }
        }
    }

    public static void hover(CommandBuffer<EntityStore> accessor, Ref<EntityStore> hovered) {
        if (hovered == null || !hovered.isValid())
            return;

        HoverableComponent hoveredComponent = accessor.getComponent(hovered,
                HoverableComponent.getComponentType());
        if (hoveredComponent == null)
            return;

        switch (hoveredComponent.getType()) {
            case GLYPH: {
                GlyphComponent newEffect = accessor.getComponent(hovered,
                        GlyphComponent.getComponentType());
                if (newEffect == null)
                    return;
                GlyphStyler.enterGlyphHover(accessor, newEffect);
            }
                break;
            case NODE: {
                DebugComponent debug = accessor.getComponent(hovered, DebugComponent.getComponentType());
                if (debug == null)
                    return;
                debug.setScaleMultiplier(1.3f);
                debug.setIntervalMultiplier(0.25f);
                debug.setFadeMultiplier(0.25f);
                debug.setTimer(0);
            }
                break;
            case CONTAINER: {
                DebugComponent debug = accessor.getComponent(hovered, DebugComponent.getComponentType());
                if (debug == null)
                    return;
                debug.setScaleMultiplier(1.3f);
                debug.setIntervalMultiplier(0.25f);
                debug.setFadeMultiplier(0.25f);
                debug.setTimer(0);
            }
        }

    }

    public static void hoverParticles(CommandBuffer<EntityStore> accessor, Ref<EntityStore> hovered, float dt,
            PedestalBlockComponent pedestal, Ref<EntityStore> playerRef) {
        if (hovered == null || !hovered.isValid())
            return;

        HoverableComponent hoverComp = accessor.getComponent(hovered,
                HoverableComponent.getComponentType());

        if (hoverComp == null)
            return;

        switch (hoverComp.getType()) {
            case GLYPH: {
                if (pedestal.getTickLength(HOVER_PARTICLE) > 0f) {
                    pedestal.incrementTickLength(HOVER_PARTICLE, dt);
                    return;
                }

                pedestal.setTickLength(HOVER_PARTICLE, -0.5f);
                TransformComponent transform = accessor.getComponent(hovered,
                        TransformComponent.getComponentType());
                if (transform == null)
                    return;
                Vector3d pos = transform.getPosition();
                ParticleUtil.spawnParticleEffect(HOVER_PARTICLE, pos, hovered,
                        List.of(playerRef), accessor);

            }
                break;
            default:
                break;
        }
    }
}
