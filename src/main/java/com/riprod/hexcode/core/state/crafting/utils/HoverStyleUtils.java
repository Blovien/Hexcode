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
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.riprod.hexcode.core.state.casting.utils.GlyphStyler;
import com.riprod.hexcode.core.state.crafting.component.PedestalBlockComponent;
import com.riprod.hexcode.core.state.crafting.handlers.node.NodeRouter;

public class HoverStyleUtils {

    private static final String HOVER_PARTICLE = "Object_Hover";

    public static void unhover(CommandBuffer<EntityStore> accessor, Ref<EntityStore> unhoveredRef,
            Ref<EntityStore> playerRef) {

        if (unhoveredRef == null || !unhoveredRef.isValid())
            return;

        HoverableComponent hoveredComponent = accessor.getComponent(unhoveredRef,
                HoverableComponent.getComponentType());
        if (hoveredComponent == null)
            return;

        clearHint(accessor, unhoveredRef, hoveredComponent);

        switch (hoveredComponent.getType()) {
            case GLYPH: {
                GlyphComponent prevEffect = accessor.getComponent(unhoveredRef,
                        GlyphComponent.getComponentType());
                if (prevEffect == null)
                    return;
                GlyphStyler.exitGlyphHover(accessor, prevEffect);
            }
                break;
            case NODE:
                NodeRouter.unhover(accessor, unhoveredRef, playerRef);
                break;
        }
    }

    public static void hover(CommandBuffer<EntityStore> accessor, Ref<EntityStore> hovered,
            Ref<EntityStore> playerRef) {
        if (hovered == null || !hovered.isValid())
            return;

        HoverableComponent hoveredComponent = accessor.getComponent(hovered,
                HoverableComponent.getComponentType());
        if (hoveredComponent == null)
            return;

        showHint(accessor, hovered, hoveredComponent);

        switch (hoveredComponent.getType()) {
            case GLYPH: {
                GlyphComponent newEffect = accessor.getComponent(hovered,
                        GlyphComponent.getComponentType());
                if (newEffect == null)
                    return;
                GlyphStyler.enterGlyphHover(accessor, newEffect);
            }
                break;
            case NODE:
                NodeRouter.hover(accessor, hovered, playerRef);
                break;
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

    private static void showHint(CommandBuffer<EntityStore> accessor, Ref<EntityStore> ref,
            HoverableComponent hoverable) {
        String text = hoverable.getHintText();
        if (text == null)
            return;
        Nameplate nameplate = accessor.getComponent(ref, Nameplate.getComponentType());
        if (nameplate == null)
            return;
        nameplate.setText(text);
    }

    private static void clearHint(CommandBuffer<EntityStore> accessor, Ref<EntityStore> ref,
            HoverableComponent hoverable) {
        if (hoverable.getHintText() == null)
            return;
        Nameplate nameplate = accessor.getComponent(ref, Nameplate.getComponentType());
        if (nameplate == null)
            return;
        nameplate.setText("");
    }
}
