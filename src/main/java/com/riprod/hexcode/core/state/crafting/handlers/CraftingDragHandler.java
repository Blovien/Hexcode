package com.riprod.hexcode.core.state.crafting.handlers;

import com.hypixel.hytale.builtin.mounts.MountedComponent;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.MountController;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.glyphs.utils.CreateGlyph;

public class CraftingDragHandler {
    private static HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static Ref<EntityStore> startDrag(CommandBuffer<EntityStore> accessor,
            Ref<EntityStore> playerRef, Ref<EntityStore> entityRef) {
        float eyeHeight = 0f;
        ModelComponent modelComp = accessor.getComponent(playerRef, ModelComponent.getComponentType());
        if (modelComp != null && modelComp.getModel() != null) {
            eyeHeight = modelComp.getModel().getEyeHeight(playerRef, accessor);
        }

        Ref<EntityStore> headAnchorRef = CreateGlyph.createHeadAnchor(accessor, playerRef, eyeHeight);

        accessor.tryRemoveComponent(entityRef, MountedComponent.getComponentType());
        accessor.addComponent(entityRef, MountedComponent.getComponentType(),
                new MountedComponent(headAnchorRef, new Vector3f(0, 0, -2f), MountController.Minecart));

        return headAnchorRef;
    }

    public static void updateDrag(CommandBuffer<EntityStore> accessor,
            Ref<EntityStore> headAnchorRef, Ref<EntityStore> playerRef) {
        HeadRotation headRot = accessor.getComponent(playerRef, HeadRotation.getComponentType());
        if (headRot == null || headAnchorRef == null || !headAnchorRef.isValid()) return;

        TransformComponent headTransform = accessor.getComponent(headAnchorRef,
                TransformComponent.getComponentType());
        headTransform.getRotation().assign(
                headRot.getRotation().getPitch(),
                headRot.getRotation().getYaw(),
                0);
    }

    public static void endDrag(CommandBuffer<EntityStore> accessor,
            Ref<EntityStore> entityRef, Ref<EntityStore> headAnchorRef) {
        if (entityRef != null && entityRef.isValid()) {
            accessor.tryRemoveComponent(entityRef, MountedComponent.getComponentType());
        }
        if (headAnchorRef != null && headAnchorRef.isValid()) {
            accessor.tryRemoveEntity(headAnchorRef, RemoveReason.REMOVE);
        }
    }
}
