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

/**
 * Handles glyph visual updates including dynamic lighting and hover effects.
 */
public class GlyphRenderer {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static final float HOVER_INTENSITY_MULTIPLIER = 1.5f;
    private static final float HOVER_SCALE_MULTIPLIER = 1.1f;
    private static final int UNAVAILABLE_COLOR = 0x808080; // Grey

    private final HexcodeConfig config;
    private final LightingManager lightingManager;
    private final Map<Ref<EntityStore>, HoverState> hoverStates;

    public GlyphRenderer() {
        this.config = HexcodeConfig.getInstance();
        this.lightingManager = LightingManager.getInstance();
        this.hoverStates = new HashMap<>();
    }

    /**
     * Add dynamic lighting to a glyph entity.
     *
     * @param store The entity store
     * @param entityRef The glyph entity reference
     * @param glyph The glyph to get color from
     */
    public void addDynamicLight(Store<EntityStore> store, Ref<EntityStore> entityRef, Glyph glyph) {
        // Delegate to LightingManager
        lightingManager.addDynamicLight(store, entityRef, glyph);

        // Initialize hover state
        hoverStates.put(entityRef, new HoverState(glyph.getVisual().getGlowIntensity()));

        LOGGER.atInfo().log("Added dynamic light to glyph '%s'", glyph.getDisplayName());
    }

    /**
     * Remove dynamic lighting from a glyph entity.
     *
     * @param store The entity store
     * @param entityRef The glyph entity reference
     */
    public void removeDynamicLight(Store<EntityStore> store, Ref<EntityStore> entityRef) {
        lightingManager.removeDynamicLight(store, entityRef);
        hoverStates.remove(entityRef);
    }

    /**
     * Update hover highlight visual for a glyph.
     *
     * @param store The entity store
     * @param entityRef The glyph entity reference
     * @param isHovered Whether the glyph is being hovered
     */
    public void updateHoverHighlight(Store<EntityStore> store, Ref<EntityStore> entityRef, boolean isHovered) {
        HoverState state = hoverStates.get(entityRef);
        if (state == null) {
            return;
        }

        if (isHovered && !state.isHovered) {
            // Entering hover - increase intensity and scale
            lightingManager.applyHoverEffect(store, entityRef, true);
            applyScaleEffect(store, entityRef, HOVER_SCALE_MULTIPLIER);
            state.isHovered = true;
            LOGGER.atInfo().log("Showing hover highlight on glyph");
        } else if (!isHovered && state.isHovered) {
            // Exiting hover - restore normal intensity and scale
            lightingManager.applyHoverEffect(store, entityRef, false);
            applyScaleEffect(store, entityRef, 1.0f);
            state.isHovered = false;
            LOGGER.atInfo().log("Hiding hover highlight on glyph");
        }
    }

    /**
     * Apply scale effect to a glyph entity.
     *
     * @param store The entity store
     * @param entityRef The glyph entity reference
     * @param scale Scale multiplier (1.0 = normal)
     */
    private void applyScaleEffect(Store<EntityStore> store, Ref<EntityStore> entityRef, float scale) {
        // TODO: Apply scale to entity's transform or model component
        // TransformComponent transform = store.getComponent(entityRef, TransformComponent.getComponentType());
        // if (transform != null) {
        //     transform.setScale(new Vector3f(scale, scale, scale));
        // }
        LOGGER.atInfo().log("Applied scale %.2f to glyph", scale);
    }

    /**
     * Set glyph availability visual (greyed out if incompatible).
     *
     * @param store The entity store
     * @param entityRef The glyph entity reference
     * @param isAvailable Whether the glyph is available for use
     */
    public void setAvailabilityVisual(Store<EntityStore> store, Ref<EntityStore> entityRef, boolean isAvailable) {
        // TODO: Update visual to show availability
        // - Full color and glow when available
        // - Greyed out and dim when unavailable

        if (!isAvailable) {
            LOGGER.atInfo().log("Setting glyph visual to unavailable (greyed out)");
        }
    }

    /**
     * Create particle trail effect while dragging.
     *
     * @param store The entity store
     * @param entityRef The glyph entity reference
     * @param color Trail color
     */
    public void startDragTrail(Store<EntityStore> store, Ref<EntityStore> entityRef, int color) {
        int particleCount = config.getDragTrailParticles();
        float duration = config.getDragTrailDuration();

        // TODO: Spawn particle trail effect
        // ParticleSystem trail = new ParticleSystem("hexcode:glyph_drag_trail");
        // trail.setColor(color);
        // trail.setEmissionRate(particleCount);
        // trail.attachTo(entityRef);

        LOGGER.atInfo().log("Starting drag trail (particles=%d, duration=%.2f)", particleCount, duration);
    }

    /**
     * Stop particle trail effect.
     *
     * @param store The entity store
     * @param entityRef The glyph entity reference
     */
    public void stopDragTrail(Store<EntityStore> store, Ref<EntityStore> entityRef) {
        // TODO: Stop particle trail effect
        LOGGER.atInfo().log("Stopping drag trail");
    }

    /**
     * Update all glyph visuals (for animations).
     *
     * @param store The entity store
     * @param dt Delta time in seconds
     */
    public void update(Store<EntityStore> store, float dt) {
        lightingManager.update(store, dt);

        // Update pulse animations for hovered glyphs
        float time = System.currentTimeMillis() / 1000.0f;
        for (Map.Entry<Ref<EntityStore>, HoverState> entry : hoverStates.entrySet()) {
            HoverState state = entry.getValue();
            if (state.isHovered) {
                // Apply gentle pulse to hovered glyphs
                lightingManager.applyPulseAnimation(store, entry.getKey(), time, 2.0f, 0.1f);
            }
        }
    }

    /**
     * Internal state for hover tracking.
     */
    private static class HoverState {
        float baseIntensity;
        boolean isHovered;

        HoverState(float baseIntensity) {
            this.baseIntensity = baseIntensity;
            this.isHovered = false;
        }
    }
}
