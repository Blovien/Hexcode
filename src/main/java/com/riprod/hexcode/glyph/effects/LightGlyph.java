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
import com.riprod.hexcode.asset.GlyphAssetDefinition;
import com.riprod.hexcode.executing.SpellContext;
import com.riprod.hexcode.glyph.GlyphVisual;

import java.util.UUID;

/**
 * Light effect glyph - creates a light source at target location.
 *
 * <p>Asset-driven properties:
 * <ul>
 *   <li>lightRadius - base light radius in blocks (default: 10.0)</li>
 *   <li>lightIntensity - light brightness (default: 15.0)</li>
 *   <li>lightDuration - how long the light lasts in seconds (default: 60.0)</li>
 *   <li>colorRed - red component 0-255 (default: 255)</li>
 *   <li>colorGreen - green component 0-255 (default: 255)</li>
 *   <li>colorBlue - blue component 0-255 (default: 240)</li>
 * </ul>
 */
public class LightGlyph extends EffectGlyph {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /**
     * Create a light glyph from an asset definition.
     *
     * @param assetDefinition The asset definition containing glyph properties
     */
    public LightGlyph(GlyphAssetDefinition assetDefinition) {
        super(assetDefinition, GlyphVisual.effect(GlyphVisual.COLOR_LIGHT, "Light"));
    }

    @Override
    protected void applyEffect(SpellContext context, Ref<EntityStore> target, float power) {
        Store<EntityStore> store = context.getStore();

        // Get target position
        TransformComponent transform = store.getComponent(target, TransformComponent.getComponentType());
        if (transform != null) {
            Vector3d position = transform.getPosition();
            spawnLightAtPosition(context, position, power);
        }
    }

    @Override
    protected void applyEffectAtPosition(SpellContext context, Vector3d position, float power) {
        spawnLightAtPosition(context, position, power);
    }

    /**
     * Spawn a light entity at the given position.
     */
    private void spawnLightAtPosition(SpellContext context, Vector3d position, float power) {
        Store<EntityStore> store = context.getStore();

        // Get asset-driven properties
        float lightRadius = getProperty("lightRadius", 10.0f);
        float lightIntensity = getProperty("lightIntensity", 15.0f);
        int colorRed = getProperty("colorRed", 255);
        int colorGreen = getProperty("colorGreen", 255);
        int colorBlue = getProperty("colorBlue", 240);

        // Apply power multiplier to intensity and radius
        float actualIntensity = lightIntensity * power;
        float actualRadius = lightRadius * (actualIntensity / lightIntensity);

        LOGGER.atInfo().log("Creating light source: radius %.1f, intensity %.1f at (%.1f, %.1f, %.1f)",
                actualRadius, actualIntensity, position.x, position.y, position.z);

        // Create entity holder
        Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();

        // Add UUID component
        holder.addComponent(UUIDComponent.getComponentType(), new UUIDComponent(UUID.randomUUID()));

        // Add transform component
        TransformComponent transform = new TransformComponent(position, new Vector3f(0, 0, 0));
        holder.addComponent(TransformComponent.getComponentType(), transform);

        // Add dynamic light component
        ColorLight lightSettings = new ColorLight();
        lightSettings.radius = (byte) Math.min(actualRadius, 127);
        lightSettings.red = (byte) colorRed;
        lightSettings.green = (byte) colorGreen;
        lightSettings.blue = (byte) colorBlue;
        holder.addComponent(DynamicLight.getComponentType(), new DynamicLight(lightSettings));

        // Add entity to store
        Ref<EntityStore> lightRef = store.addEntity(holder, AddReason.SPAWN);

        LOGGER.atInfo().log("Spawned light entity at (%.1f, %.1f, %.1f) with radius %.1f",
                position.x, position.y, position.z, actualRadius);

        // Note: Duration-based despawn would need to be handled by a separate system
        // that tracks light entities and removes them after their duration expires.
    }
}
