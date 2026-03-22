package com.riprod.hexcode.core.state.casting.utils;

import java.util.List;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.spatial.SpatialResource;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class AuraSpawner {

    private static final String AURA_PARTICLE = "Mist_Spawner";
    private static final String MIST_PARTICLE = "Mist";

    /** Spawn the aura around the player's head */
    public static void SpawnAuraParticles(ComponentAccessor<EntityStore> accessor, Ref<EntityStore> entityRef) {

        TransformComponent transform = accessor.getComponent(entityRef, TransformComponent.getComponentType());
        Vector3d position = transform.getPosition();
        position = position.add(0, 1.0, 0); // raise the position to be around the head

        SpatialResource<Ref<EntityStore>, EntityStore> playerSpatialResource = accessor
                .getResource(EntityModule.get().getPlayerSpatialResourceType());
        List<Ref<EntityStore>> results = SpatialResource.getThreadLocalReferenceList();
        playerSpatialResource.getSpatialStructure().collect(position, 15.0, results);

        ParticleUtil.spawnParticleEffect(AURA_PARTICLE, position, entityRef, results, accessor);

    }

    /** Spawn the mist around the player */
    public static void SpawnMistParticles(ComponentAccessor<EntityStore> accessor, Ref<EntityStore> entityRef) {
        TransformComponent transform = accessor.getComponent(entityRef, TransformComponent.getComponentType());
        Vector3d position = transform.getPosition();
        position = position.add(0, 1.0, 0); // raise the position to be around the head

        SpatialResource<Ref<EntityStore>, EntityStore> playerSpatialResource = accessor
                .getResource(EntityModule.get().getPlayerSpatialResourceType());
        List<Ref<EntityStore>> results = SpatialResource.getThreadLocalReferenceList();
        playerSpatialResource.getSpatialStructure().collect(position, 15.0, results);

        ParticleUtil.spawnParticleEffect(MIST_PARTICLE, position, entityRef, results, accessor);
    }
}
