package com.riprod.hexcode.core.state.crafting.handlers;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.glyphs.component.GlyphComponent;
import com.riprod.hexcode.core.common.glyphs.utils.GlyphType;
import com.riprod.hexcode.core.state.crafting.component.NodeComponent;

import javax.annotation.Nullable;

public class CraftingDropHandler {

    public enum DropResult {
        PLACED, LINKED, IGNORED
    }

    public static DropResult handleDrop(CommandBuffer<EntityStore> accessor,
            Ref<EntityStore> draggedRef, @Nullable Ref<EntityStore> targetRef) {

        if (targetRef == null) {
            return DropResult.PLACED;
        }

        GlyphComponent draggedEffect = accessor.getComponent(draggedRef,
                GlyphComponent.getComponentType());
        if (draggedEffect == null) {
            return DropResult.IGNORED;
        }

        GlyphType draggedType = draggedEffect.getGlyph().getType();

        GlyphComponent targetEffect = accessor.getComponent(targetRef,
                GlyphComponent.getComponentType());

        boolean draggedCanChain = draggedType == GlyphType.Effect || draggedType == GlyphType.Hybrid;
        GlyphType targetType = targetEffect != null ? targetEffect.getGlyph().getType() : null;
        boolean targetCanChain = targetType == GlyphType.Effect || targetType == GlyphType.Hybrid;
        if (draggedCanChain && targetCanChain) {
            linkGlyphs(accessor, draggedEffect, targetEffect);
            offsetToAvoidOverlap(accessor, draggedRef, targetRef);
            return DropResult.LINKED;
        }

        return DropResult.IGNORED;
    }

    private static void linkGlyphs(CommandBuffer<EntityStore> accessor,
            GlyphComponent source, GlyphComponent target) {
        source.getGlyph().addNext(target.getId());
        target.getGlyph().addPrevious(source.getId());

        Ref<EntityStore> sourceNodeRef = source.getNodeRef();
        Ref<EntityStore> targetNodeRef = target.getNodeRef();
        if (sourceNodeRef != null && sourceNodeRef.isValid()
                && targetNodeRef != null && targetNodeRef.isValid()) {
            NodeComponent sourceNode = accessor.getComponent(sourceNodeRef, NodeComponent.getComponentType());
            NodeComponent targetNode = accessor.getComponent(targetNodeRef, NodeComponent.getComponentType());
            if (sourceNode != null && targetNode != null) {
                sourceNode.addOutgoingRef(targetNodeRef);
                targetNode.addIncomingRef(sourceNodeRef);
            }
        }
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
}
