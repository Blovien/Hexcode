package com.riprod.hexcode.glyph.effects;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.ColorLight;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.DynamicLight;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.execution.ExecutionContext;
import com.riprod.hexcode.execution.TargetSet;
import com.riprod.hexcode.glyph.GlyphVisual;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Light effect glyph - creates a light source at target location.
 */
public class LightGlyph extends EffectGlyph {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static final String ID = "hexcode:light";
    public static final int BASE_COST = 10;
    public static final float LIGHT_RADIUS = 10.0f;
    public static final float LIGHT_INTENSITY = 15.0f;
    public static final float LIGHT_DURATION = 60.0f; // 1 minute

    public LightGlyph() {
        super(
            ID,
            "Light",
            BASE_COST,
            GlyphVisual.effect(GlyphVisual.COLOR_UTILITY),
            Set.of("hexcode:power", "hexcode:duration")
        );
    }

    @Override
    public void applyEffect(ExecutionContext ctx, TargetSet targets) {
        float intensity = getModifiedAmount(ctx, LIGHT_INTENSITY);
        float duration = getModifiedDuration(ctx, LIGHT_DURATION);
        float radius = LIGHT_RADIUS * (intensity / LIGHT_INTENSITY);

        Store<EntityStore> store = ctx.getStore();

        LOGGER.atInfo().log("Creating light source: radius %.1f, intensity %.1f, duration %.1f",
                radius, intensity, duration);

        // Create light at target position(s)
        if (targets.hasPositions()) {
            // Light at the target position
            List<Vector3d> position = targets.getPositions();
            for (Vector3d pos : position) {
                spawnLightEntity(store, pos, radius, intensity);
            }
        } else if (targets.getEntityCount() > 0) {
            // Light at each target entity position
            for (Ref<EntityStore> targetRef : targets.getEntities()) {
                TransformComponent transform = store.getComponent(targetRef, TransformComponent.getComponentType());
                if (transform != null) {
                    Vector3d position = transform.getPosition();
                    spawnLightEntity(store, position, radius, intensity);
                }
            }
        } else {
            // Light at origin
            Vector3d origin = ctx.getCurrentOrigin();
            spawnLightEntity(store, origin, radius, intensity);
        }
    }

    /**
     * Spawn a light entity at the given position.
     */
    private void spawnLightEntity(Store<EntityStore> store, Vector3d position, float radius, float intensity) {
        // Create entity holder
        Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();

        // Add UUID component
        holder.addComponent(UUIDComponent.getComponentType(), new UUIDComponent(UUID.randomUUID()));

        // Add transform component
        TransformComponent transform = new TransformComponent(position, new Vector3f(0, 0, 0));
        holder.addComponent(TransformComponent.getComponentType(), transform);

        // Add dynamic light component
        ColorLight lightSettings = new ColorLight();
        lightSettings.radius = (byte) radius;
        lightSettings.red = (byte) 255;
        lightSettings.green = (byte) 255;
        lightSettings.blue = (byte) 240; // Slightly warm white
        holder.addComponent(DynamicLight.getComponentType(), new DynamicLight(lightSettings));

        // Add entity to store
        Ref<EntityStore> lightRef = store.addEntity(holder, AddReason.SPAWN);

        LOGGER.atInfo().log("Spawned light entity at (%.1f, %.1f, %.1f) with radius %.1f",
                position.x, position.y, position.z, radius);

        // Note: Duration-based despawn would need to be handled by a separate system
        // that tracks light entities and removes them after their duration expires.
        // For MVP, lights persist until manually despawned or server restart.
    }
}
