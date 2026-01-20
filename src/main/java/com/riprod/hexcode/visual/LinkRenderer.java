package com.riprod.hexcode.visual;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.ColorLight;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.DynamicLight;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Renders connection lines between linked sibling glyphs.
 * Uses a series of light points to create a glowing line effect.
 */
public class LinkRenderer {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static final int LINK_COLOR = 0xFFD700; // Gold
    private static final int POINTS_PER_LINK = 5;
    private static final float LINK_LIGHT_RADIUS = 3.0f;

    private final Map<String, LinkVisual> activeLinks;

    public LinkRenderer() {
        this.activeLinks = new HashMap<>();
    }

    /**
     * Create a link line between two glyph positions.
     *
     * @param store The entity store
     * @param from Start position
     * @param to End position
     * @return Link ID for later reference
     */
    public String createLink(Store<EntityStore> store, Vector3d from, Vector3d to) {
        String linkId = UUID.randomUUID().toString();

        LinkVisual visual = new LinkVisual(from, to);

        // Create light points along the line
        for (int i = 0; i < POINTS_PER_LINK; i++) {
            float t = (float) i / (POINTS_PER_LINK - 1);
            Vector3d pos = interpolate(from, to, t);

            Ref<EntityStore> pointRef = createLightPoint(store, pos);
            if (pointRef != null) {
                visual.addPoint(pointRef);
            }
        }

        activeLinks.put(linkId, visual);

        LOGGER.atInfo().log("Created link from (%.1f, %.1f, %.1f) to (%.1f, %.1f, %.1f) with %d points",
                from.x, from.y, from.z, to.x, to.y, to.z, visual.getPointCount());

        return linkId;
    }

    /**
     * Create a single light point for the link line.
     */
    private Ref<EntityStore> createLightPoint(Store<EntityStore> store, Vector3d position) {
        Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();

        // Add UUID component
        holder.addComponent(UUIDComponent.getComponentType(), new UUIDComponent(UUID.randomUUID()));

        // Add transform component
        TransformComponent transform = new TransformComponent(position, new Vector3f(0, 0, 0));
        holder.addComponent(TransformComponent.getComponentType(), transform);

        // Add dynamic light for the glow effect
        ColorLight linkLight = new ColorLight();
        linkLight.radius = (int) LINK_LIGHT_RADIUS;
        linkLight.red = (byte) ((LINK_COLOR >> 16) & 0xFF);
        linkLight.green = (byte) ((LINK_COLOR >> 8) & 0xFF);
        linkLight.blue = (byte) (LINK_COLOR & 0xFF);
        holder.addComponent(DynamicLight.getComponentType(), new DynamicLight(linkLight));

        return store.addEntity(holder, AddReason.SPAWN);
    }

    /**
     * Update link endpoints.
     *
     * @param store The entity store
     * @param linkId The link ID
     * @param from New start position
     * @param to New end position
     */
    public void updateLink(Store<EntityStore> store, String linkId, Vector3d from, Vector3d to) {
        LinkVisual visual = activeLinks.get(linkId);
        if (visual == null) {
            return;
        }

        visual.setEndpoints(from, to);

        // Update point positions
        int i = 0;
        for (Ref<EntityStore> pointRef : visual.getPoints()) {
            float t = (float) i / (POINTS_PER_LINK - 1);
            Vector3d pos = interpolate(from, to, t);

            TransformComponent transform = store.getComponent(pointRef, TransformComponent.getComponentType());
            if (transform != null) {
                transform.setPosition(pos);
            }
            i++;
        }

        LOGGER.atInfo().log("Updated link positions");
    }

    /**
     * Destroy a link visual.
     *
     * @param store The entity store
     * @param linkId The link ID
     */
    public void destroyLink(Store<EntityStore> store, String linkId) {
        LinkVisual visual = activeLinks.remove(linkId);
        if (visual != null) {
            for (Ref<EntityStore> pointRef : visual.getPoints()) {
                if (pointRef != null && pointRef.isValid()) {
                    store.removeEntity(pointRef, RemoveReason.REMOVE);
                }
            }
            LOGGER.atInfo().log("Destroyed link visual with %d points", visual.getPointCount());
        }
    }

    /**
     * Destroy all active links.
     */
    public void destroyAllLinks(Store<EntityStore> store) {
        for (String linkId : new java.util.ArrayList<>(activeLinks.keySet())) {
            destroyLink(store, linkId);
        }
    }

    /**
     * Check if a link exists between two positions (for visual indicator).
     */
    public boolean hasLinkNear(Vector3d pos1, Vector3d pos2, float threshold) {
        for (LinkVisual visual : activeLinks.values()) {
            double dist1 = visual.from.distanceTo(pos1) + visual.to.distanceTo(pos2);
            double dist2 = visual.from.distanceTo(pos2) + visual.to.distanceTo(pos1);
            if (dist1 < threshold || dist2 < threshold) {
                return true;
            }
        }
        return false;
    }

    /**
     * Interpolate between two positions.
     */
    private Vector3d interpolate(Vector3d from, Vector3d to, float t) {
        return new Vector3d(
                from.x + (to.x - from.x) * t,
                from.y + (to.y - from.y) * t,
                from.z + (to.z - from.z) * t
        );
    }

    /**
     * Internal class to track link visuals.
     */
    private static class LinkVisual {
        Vector3d from;
        Vector3d to;
        java.util.List<Ref<EntityStore>> points;

        LinkVisual(Vector3d from, Vector3d to) {
            this.from = new Vector3d(from);
            this.to = new Vector3d(to);
            this.points = new java.util.ArrayList<>();
        }

        void addPoint(Ref<EntityStore> point) {
            points.add(point);
        }

        java.util.List<Ref<EntityStore>> getPoints() {
            return points;
        }

        int getPointCount() {
            return points.size();
        }

        void setEndpoints(Vector3d from, Vector3d to) {
            this.from = new Vector3d(from);
            this.to = new Vector3d(to);
        }
    }
}
