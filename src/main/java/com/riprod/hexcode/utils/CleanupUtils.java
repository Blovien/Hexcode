package com.riprod.hexcode.utils;

import com.hypixel.hytale.builtin.mounts.MountedByComponent;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class CleanupUtils {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static void safeRemoveEntity(CommandBuffer<EntityStore> buffer, Ref<EntityStore> entityRef) {
        if (entityRef != null && entityRef.isValid()) {
            try {
                buffer.tryRemoveEntity(entityRef, RemoveReason.REMOVE);
            } catch (Exception e) {
                LOGGER.atWarning().log("Error occurred while removing entity", e);
            }
        }
    }

    public static void safeRemoveEntities(CommandBuffer<EntityStore> buffer, Iterable<Ref<EntityStore>> entityRefs) {
        for (Ref<EntityStore> ref : entityRefs) {
            safeRemoveEntity(buffer, ref);
        }
    }

    public static void safeRemoveConstruct(CommandBuffer<EntityStore> buffer, Ref<EntityStore> constructRef) {
        if (constructRef == null || !constructRef.isValid()) return;
        try {
            MountedByComponent ridden = buffer.getComponent(constructRef, MountedByComponent.getComponentType());
            if (ridden != null) {
                for (Ref<EntityStore> passenger : ridden.getPassengers()) {
                    safeRemoveEntity(buffer, passenger);
                }
            }
            buffer.tryRemoveEntity(constructRef, RemoveReason.REMOVE);
        } catch (Exception e) {
            LOGGER.atWarning().log("Error occurred while removing construct", e);
        }
    }
}
