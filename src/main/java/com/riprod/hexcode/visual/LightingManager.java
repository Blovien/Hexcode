package com.riprod.hexcode.visual;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.config.HexcodeConfig;
import com.riprod.hexcode.glyph.Glyph;
import com.riprod.hexcode.glyph.GlyphVisual;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages dynamic lighting for glyph entities.
 *
 * Each glyph in the orbital ring and crafting space can emit light
 * based on its visual properties.
 */
public class LightingManager {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static LightingManager instance;

    private final HexcodeConfig config;
    private final Map<Ref<EntityStore>, LightState> activeLights;

    private LightingManager() {
        this.config = HexcodeConfig.getInstance();
        this.activeLights = new HashMap<>();
    }

    public static synchronized LightingManager getInstance() {
        if (instance == null) {
            instance = new LightingManager();
        }
        return instance;
    }

    /**
     * Add dynamic lighting to a glyph entity.
     *
     * @param store The entity store
     * @param entityRef The glyph entity reference
     * @param glyph The glyph to get visual properties from
     */
    public void addDynamicLight(Store<EntityStore> store, Ref<EntityStore> entityRef, Glyph glyph) {
        GlyphVisual visual = glyph.getVisual();
        float radius = config.getGlyphLightRadius();
        float intensity = visual.getGlowIntensity();
        int color = visual.getColor();

        // Create light state
        LightState lightState = new LightState(radius, intensity, color);
        activeLights.put(entityRef, lightState);

        // TODO: Add DynamicLight component to entity using Hytale's lighting API
        // Example (actual API may differ):
        // DynamicLight light = new DynamicLight();
        // light.setRadius(radius);
        // light.setIntensity(intensity);
        // light.setColor(color);
        // store.addComponent(entityRef, DynamicLight.getComponentType(), light);

        LOGGER.atInfo().log("Added dynamic light to glyph '%s' (radius=%.1f, intensity=%.1f, color=#%06X)",
                glyph.getDisplayName(), radius, intensity, color);
    }

    /**
     * Update light intensity (e.g., for pulsing effect).
     *
     * @param store The entity store
     * @param entityRef The glyph entity reference
     * @param intensity New intensity value
     */
    public void setLightIntensity(Store<EntityStore> store, Ref<EntityStore> entityRef, float intensity) {
        LightState lightState = activeLights.get(entityRef);
        if (lightState == null) {
            return;
        }

        lightState.intensity = intensity;

        // TODO: Update the DynamicLight component
        // DynamicLight light = store.getComponent(entityRef, DynamicLight.getComponentType());
        // if (light != null) {
        //     light.setIntensity(intensity);
        // }

        LOGGER.atInfo().log("Updated light intensity to %.1f", intensity);
    }

    /**
     * Set light color.
     *
     * @param store The entity store
     * @param entityRef The glyph entity reference
     * @param color New color value (RGB)
     */
    public void setLightColor(Store<EntityStore> store, Ref<EntityStore> entityRef, int color) {
        LightState lightState = activeLights.get(entityRef);
        if (lightState == null) {
            return;
        }

        lightState.color = color;

        // TODO: Update the DynamicLight component color
        LOGGER.atInfo().log("Updated light color to #%06X", color);
    }

    /**
     * Set light radius.
     *
     * @param store The entity store
     * @param entityRef The glyph entity reference
     * @param radius New radius value
     */
    public void setLightRadius(Store<EntityStore> store, Ref<EntityStore> entityRef, float radius) {
        LightState lightState = activeLights.get(entityRef);
        if (lightState == null) {
            return;
        }

        lightState.radius = radius;

        // TODO: Update the DynamicLight component radius
        LOGGER.atInfo().log("Updated light radius to %.1f", radius);
    }

    /**
     * Remove dynamic light from an entity.
     *
     * @param store The entity store
     * @param entityRef The entity reference
     */
    public void removeDynamicLight(Store<EntityStore> store, Ref<EntityStore> entityRef) {
        LightState removed = activeLights.remove(entityRef);
        if (removed != null) {
            // TODO: Remove the DynamicLight component
            // store.removeComponent(entityRef, DynamicLight.getComponentType());
            LOGGER.atInfo().log("Removed dynamic light");
        }
    }

    /**
     * Apply hover effect to light (increase intensity).
     *
     * @param store The entity store
     * @param entityRef The glyph entity reference
     * @param isHovered Whether the glyph is being hovered
     */
    public void applyHoverEffect(Store<EntityStore> store, Ref<EntityStore> entityRef, boolean isHovered) {
        LightState lightState = activeLights.get(entityRef);
        if (lightState == null) {
            return;
        }

        float targetIntensity = isHovered ?
                lightState.baseIntensity * 1.5f :
                lightState.baseIntensity;

        setLightIntensity(store, entityRef, targetIntensity);
    }

    /**
     * Apply pulse animation to light.
     *
     * @param entityRef The glyph entity reference
     * @param time Current animation time
     * @param pulseFrequency Pulse frequency in Hz
     * @param pulseAmplitude Pulse amplitude (0-1 range)
     */
    public void applyPulseAnimation(Store<EntityStore> store, Ref<EntityStore> entityRef,
                                     float time, float pulseFrequency, float pulseAmplitude) {
        LightState lightState = activeLights.get(entityRef);
        if (lightState == null) {
            return;
        }

        // Calculate pulsing intensity using sine wave
        float pulse = (float) Math.sin(time * pulseFrequency * Math.PI * 2);
        float intensity = lightState.baseIntensity * (1.0f + pulse * pulseAmplitude);

        setLightIntensity(store, entityRef, intensity);
    }

    /**
     * Update all active lights (for animations).
     *
     * @param store The entity store
     * @param dt Delta time in seconds
     */
    public void update(Store<EntityStore> store, float dt) {
        // Apply any per-frame light updates here
        // For example, ambient pulse animations
    }

    /**
     * Internal state for a dynamic light.
     */
    private static class LightState {
        float radius;
        float intensity;
        float baseIntensity;
        int color;

        LightState(float radius, float intensity, int color) {
            this.radius = radius;
            this.intensity = intensity;
            this.baseIntensity = intensity;
            this.color = color;
        }
    }
}
