package com.riprod.hexcode.core.common.hover.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.ResourceType;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.spatial.SpatialResource;
import com.hypixel.hytale.component.spatial.SpatialSystem;
import org.joml.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.hover.component.HoverableComponent;
import com.hypixel.hytale.logger.HytaleLogger;

public class HoverableSpatialSystem extends SpatialSystem<EntityStore> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static ResourceType<EntityStore, SpatialResource<Ref<EntityStore>, EntityStore>> resourceType;

    public static final Query<EntityStore> QUERY = Query.and(HoverableComponent.getComponentType());

    public HoverableSpatialSystem(
            ResourceType<EntityStore, SpatialResource<Ref<EntityStore>, EntityStore>> resourceType) {
        super(resourceType);
        HoverableSpatialSystem.resourceType = resourceType;
    }

    public static ResourceType<EntityStore, SpatialResource<Ref<EntityStore>, EntityStore>> getResourceType() {
        return resourceType;
    }

    @Override
    public Query<EntityStore> getQuery() {
        return QUERY;
    }

    @Override
    public Vector3d getPosition(ArchetypeChunk<EntityStore> chunk, int index) {
        try {
            return chunk.getComponent(index, TransformComponent.getComponentType()).getPosition();
        } catch (Exception e) {
            LOGGER.atSevere().log("[hexcode] HoverableSpatialSystem.getPosition failed: %s", e.getMessage());
            return new Vector3d(0, 0, 0);
        }
    }
}