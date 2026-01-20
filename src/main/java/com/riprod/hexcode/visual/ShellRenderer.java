package com.riprod.hexcode.visual;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.glyph.Glyph;
import com.riprod.hexcode.glyph.GlyphVisual;
import com.hypixel.hytale.math.vector.Vector3d;

/**
 * Renders shell wrapper visuals for modifier and select glyphs.
 *
 * Shell glyphs visually surround their children in the crafting space.
 */
public class ShellRenderer {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /**
     * Create a shell visual for a glyph.
     *
     * @param store The entity store
     * @param position Center position of the shell
     * @param radius Shell radius
     * @param glyph The glyph to get visual properties from
     * @return Entity reference for the shell visual
     */
    public Ref<EntityStore> createShell(Store<EntityStore> store, Vector3d position, float radius, Glyph glyph) {
        GlyphVisual visual = glyph.getVisual();
        int color = visual.getColor();

        // TODO: Create shell mesh or particle effect
        // Options:
        // - Translucent sphere mesh
        // - Particle ring effect
        // - Shader-based glow

        LOGGER.atInfo().log("Creating shell visual for '%s' at (%.1f, %.1f, %.1f) radius=%.2f color=#%06X",
                glyph.getDisplayName(), position.x, position.y, position.z, radius, color);

        return null; // TODO: Return actual entity ref
    }

    /**
     * Update shell radius.
     *
     * @param store The entity store
     * @param shellRef The shell entity reference
     * @param newRadius New radius
     */
    public void updateRadius(Store<EntityStore> store, Ref<EntityStore> shellRef, float newRadius) {
        // TODO: Update shell mesh scale
        LOGGER.atInfo().log("Updating shell radius to %.2f", newRadius);
    }

    /**
     * Destroy a shell visual.
     *
     * @param store The entity store
     * @param shellRef The shell entity reference
     */
    public void destroyShell(Store<EntityStore> store, Ref<EntityStore> shellRef) {
        if (shellRef != null) {
            // TODO: Destroy shell entity
            LOGGER.atInfo().log("Destroying shell visual");
        }
    }
}
