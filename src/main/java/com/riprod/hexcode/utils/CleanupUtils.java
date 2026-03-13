package com.riprod.hexcode.utils;

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
}
