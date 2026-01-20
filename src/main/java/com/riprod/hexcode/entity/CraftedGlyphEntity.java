package com.riprod.hexcode.entity;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.glyph.Glyph;
import com.riprod.hexcode.hex.HexNode;
import com.riprod.hexcode.util.HexMathUtil;
import com.hypixel.hytale.math.vector.Vector3d;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a glyph entity placed in the crafting space.
 *
 * Shows the composed Hex structure with shell visuals.
 * Shell glyphs visually surround their children.
 * Linked siblings show connection lines between them.
 */
public class CraftedGlyphEntity {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final HexNode node;
    private Ref<EntityStore> entityRef;

    private Vector3d localPosition;
    private boolean isShell;
    private float shellRadius;
    private List<CraftedGlyphEntity> linkedSiblings;

    public CraftedGlyphEntity(HexNode node, Vector3d localPosition) {
        this.node = node;
        this.localPosition = localPosition;
        this.isShell = !node.isLeaf();
        this.shellRadius = 0.3f;
        this.linkedSiblings = new ArrayList<>();
    }

    /**
     * @return The hex node this entity represents
     */
    public HexNode getNode() {
        return node;
    }

    /**
     * @return The glyph at this node
     */
    public Glyph getGlyph() {
        return node.getGlyph();
    }

    /**
     * @return Position in crafting space (local to player)
     */
    public Vector3d getLocalPosition() {
        return new Vector3d(localPosition);
    }

    /**
     * Set position in crafting space.
     */
    public void setLocalPosition(Vector3d position) {
        this.localPosition = new Vector3d(position);
    }

    /**
     * @return true if this glyph is a shell (has children)
     */
    public boolean isShell() {
        return isShell;
    }

    /**
     * @return Shell radius (for shell visuals)
     */
    public float getShellRadius() {
        return shellRadius;
    }

    /**
     * Set shell radius.
     */
    public void setShellRadius(float radius) {
        this.shellRadius = radius;
    }

    /**
     * @return List of linked sibling entities
     */
    public List<CraftedGlyphEntity> getLinkedSiblings() {
        return linkedSiblings;
    }

    /**
     * Add a linked sibling.
     */
    public void addLinkedSibling(CraftedGlyphEntity sibling) {
        if (!linkedSiblings.contains(sibling)) {
            linkedSiblings.add(sibling);
        }
    }

    /**
     * Calculate world position from local position.
     *
     * @param playerPosition Player's position
     * @param playerLookDirection Player's look direction
     * @param craftingSpaceDistance Distance from player to crafting space
     * @return World position
     */
    public Vector3d calculateWorldPosition(Vector3d playerPosition, Vector3d playerLookDirection,
                                           float craftingSpaceDistance) {
        // Calculate crafting space center
        Vector3d center = HexMathUtil.mul(new Vector3d(playerLookDirection).normalize(), craftingSpaceDistance);
        center.add(playerPosition);

        // Add local offset
        return center.add(localPosition);
    }

    /**
     * Spawn this crafted glyph entity in the world.
     *
     * @param store The entity store
     * @param worldPosition World position to spawn at
     */
    public void spawn(Store<EntityStore> store, Vector3d worldPosition) {
        // TODO: Implement actual entity spawning
        // This would create an entity with:
        // - ModelComponent (glyph visual + shell if applicable)
        // - TransformComponent
        // - DynamicLight
        // - Custom CraftedGlyphComponent

        LOGGER.atInfo().log("Spawning crafted glyph '%s' at (%.1f, %.1f, %.1f) %s",
                getGlyph().getDisplayName(), worldPosition.x, worldPosition.y, worldPosition.z,
                isShell ? "(shell)" : "(leaf)");
    }

    /**
     * Despawn this crafted glyph entity.
     *
     * @param store The entity store
     */
    public void despawn(Store<EntityStore> store) {
        if (entityRef != null) {
            // TODO: Implement actual entity despawning
            LOGGER.atInfo().log("Despawning crafted glyph '%s'", getGlyph().getDisplayName());
            entityRef = null;
        }
    }

    /**
     * Update visual to show shell wrapping effect.
     */
    public void updateShellVisual() {
        if (!isShell) {
            return;
        }

        // Calculate shell radius based on children
        int childCount = node.getChildCount();
        shellRadius = 0.3f + (childCount * 0.1f);

        // TODO: Update shell mesh/particle effect
    }

    /**
     * Draw connection lines to linked siblings.
     *
     * @param store The entity store
     */
    public void renderSiblingConnections(Store<EntityStore> store) {
        // TODO: Render particle/line connections between siblings
        for (CraftedGlyphEntity sibling : linkedSiblings) {
            LOGGER.atInfo().log("Rendering connection from '%s' to '%s'",
                    getGlyph().getDisplayName(), sibling.getGlyph().getDisplayName());
        }
    }
}
