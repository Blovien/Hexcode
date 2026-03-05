package com.riprod.hexcode.core.state.crafting.utils;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.glyphs.component.EffectComponent;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.VariableComponent;
import com.riprod.hexcode.core.common.glyphs.utils.GlyphType;
import com.riprod.hexcode.core.common.glyphs.values.HexVal;

import java.util.List;

import javax.annotation.Nullable;

public class CraftingDropHandler {

    public enum DropResult {
        PLACED, LINKED, SLOTTED, SWAPPED, IGNORED
    }

    public static DropResult handleDrop(CommandBuffer<EntityStore> accessor,
            Ref<EntityStore> draggedRef, @Nullable Ref<EntityStore> targetRef) {

        if (targetRef == null) {
            return DropResult.PLACED;
        }

        EffectComponent draggedEffect = accessor.getComponent(draggedRef,
                EffectComponent.getComponentType());
        if (draggedEffect == null) {
            return DropResult.IGNORED;
        }

        GlyphType draggedType = draggedEffect.getGlyph().getType();

        EffectComponent targetEffect = accessor.getComponent(targetRef,
                EffectComponent.getComponentType());
        VariableComponent targetVar = accessor.getComponent(targetRef,
                VariableComponent.getComponentType());

        if (draggedType == GlyphType.Effect && targetEffect != null
                && targetEffect.getGlyph().getType() == GlyphType.Effect) {
            linkGlyphs(draggedEffect, targetEffect);
            offsetToAvoidOverlap(accessor, draggedRef, targetRef);
            return DropResult.LINKED;
        }

        if (draggedType == GlyphType.Value && targetEffect != null
                && targetEffect.getGlyph().getType() == GlyphType.Effect) {
            if (fillNextAvailableSlot(targetEffect.getGlyph())) {
                return DropResult.SLOTTED;
            }
            return DropResult.IGNORED;
        }

        if (draggedType == GlyphType.Value && targetVar != null) {
            return DropResult.SWAPPED;
        }

        if (draggedType == GlyphType.Value && targetEffect != null
                && targetEffect.getGlyph().getType() == GlyphType.Value) {
            return DropResult.IGNORED;
        }

        return DropResult.IGNORED;
    }

    private static void linkGlyphs(EffectComponent source, EffectComponent target) {
        source.getGlyph().addNext(target.getId());
        target.getGlyph().addPrevious(source.getId());
    }

    private static void offsetToAvoidOverlap(CommandBuffer<EntityStore> accessor,
            Ref<EntityStore> draggedRef, Ref<EntityStore> targetRef) {
        TransformComponent targetTransform = accessor.getComponent(targetRef,
                TransformComponent.getComponentType());
        TransformComponent draggedTransform = accessor.getComponent(draggedRef,
                TransformComponent.getComponentType());
        if (targetTransform == null || draggedTransform == null) return;

        Vector3d targetPos = targetTransform.getPosition();
        Vector3d draggedPos = draggedTransform.getPosition();

        double dx = draggedPos.x - targetPos.x;
        double dz = draggedPos.z - targetPos.z;
        double dist = Math.sqrt(dx * dx + dz * dz);

        if (dist < 0.5) {
            draggedTransform.setPosition(new Vector3d(
                targetPos.x + 0.6, targetPos.y, targetPos.z));
        }
    }

    private static boolean fillNextAvailableSlot(Glyph glyph) {
        int totalInputs = glyph.getTotalInputs();
        List<HexVal> inputs = glyph.getInputs();

        if (totalInputs == -1 || inputs.size() < totalInputs) {
            return true;
        }

        int totalOutputs = glyph.getTotalOutputs();
        List<HexVal> outputs = glyph.getOutputs();
        if (totalOutputs == -1 || outputs.size() < totalOutputs) {
            return true;
        }

        return false;
    }
}
