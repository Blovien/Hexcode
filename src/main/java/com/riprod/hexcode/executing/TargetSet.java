package com.riprod.hexcode.executing;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.math.vector.Vector3d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a set of targets for spell effects.
 *
 * Targets can be entities, positions, or both. This allows for
 * effects that target specific entities (damage) or locations (light).
 */
public class TargetSet {
    private final List<Ref<EntityStore>> entities;
    private final List<Vector3d> positions;
    private final Vector3d origin;

    private TargetSet(List<Ref<EntityStore>> entities, List<Vector3d> positions, Vector3d origin) {
        this.entities = entities != null ? new ArrayList<>(entities) : new ArrayList<>();
        this.positions = positions != null ? new ArrayList<>(positions) : new ArrayList<>();
        this.origin = origin;
    }

    /**
     * @return Empty target set
     */
    public static TargetSet empty() {
        return new TargetSet(Collections.emptyList(), Collections.emptyList(), null);
    }

    /**
     * Create a target set with a single entity.
     */
    public static TargetSet of(Ref<EntityStore> entity) {
        return new TargetSet(List.of(entity), Collections.emptyList(), null);
    }

    /**
     * Create a target set with multiple entities.
     */
    public static TargetSet ofEntities(List<Ref<EntityStore>> entities) {
        return new TargetSet(entities, Collections.emptyList(), null);
    }

    /**
     * Create a target set with a single position.
     */
    public static TargetSet ofPosition(Vector3d position) {
        return new TargetSet(Collections.emptyList(), List.of(position), null);
    }

    /**
     * Create a target set with multiple positions.
     */
    public static TargetSet ofPositions(List<Vector3d> positions) {
        return new TargetSet(Collections.emptyList(), positions, null);
    }

    /**
     * Create a target set with entities and an origin point.
     */
    public static TargetSet withOrigin(List<Ref<EntityStore>> entities, Vector3d origin) {
        return new TargetSet(entities, Collections.emptyList(), origin);
    }

    /**
     * @return List of target entities
     */
    public List<Ref<EntityStore>> getEntities() {
        return Collections.unmodifiableList(entities);
    }

    /**
     * @return List of target positions
     */
    public List<Vector3d> getPositions() {
        return Collections.unmodifiableList(positions);
    }

    /**
     * @return Origin point for this target selection (e.g., where beam hit)
     */
    public Vector3d getOrigin() {
        return origin;
    }

    /**
     * @return true if there are entity targets
     */
    public boolean hasEntities() {
        return !entities.isEmpty();
    }

    /**
     * @return true if there are position targets
     */
    public boolean hasPositions() {
        return !positions.isEmpty();
    }

    /**
     * @return true if there are any targets
     */
    public boolean isEmpty() {
        return entities.isEmpty() && positions.isEmpty();
    }

    /**
     * @return Number of entity targets
     */
    public int getEntityCount() {
        return entities.size();
    }

    /**
     * @return Number of position targets
     */
    public int getPositionCount() {
        return positions.size();
    }

    /**
     * @return Total target count (entities + positions)
     */
    public int getTotalCount() {
        return entities.size() + positions.size();
    }

    /**
     * Get the first entity target, or null if none.
     */
    public Ref<EntityStore> getFirstEntity() {
        return entities.isEmpty() ? null : entities.get(0);
    }

    /**
     * Get the first position target, or null if none.
     */
    public Vector3d getFirstPosition() {
        return positions.isEmpty() ? null : positions.get(0);
    }

    /**
     * Create a new target set with additional entities.
     */
    public TargetSet addEntities(List<Ref<EntityStore>> newEntities) {
        List<Ref<EntityStore>> combined = new ArrayList<>(this.entities);
        combined.addAll(newEntities);
        return new TargetSet(combined, this.positions, this.origin);
    }

    /**
     * Create a new target set with additional positions.
     */
    public TargetSet addPositions(List<Vector3d> newPositions) {
        List<Vector3d> combined = new ArrayList<>(this.positions);
        combined.addAll(newPositions);
        return new TargetSet(this.entities, combined, this.origin);
    }

    /**
     * Create a new target set with a different origin.
     */
    public TargetSet withOrigin(Vector3d newOrigin) {
        return new TargetSet(this.entities, this.positions, newOrigin);
    }
}
