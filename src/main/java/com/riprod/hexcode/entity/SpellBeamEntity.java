package com.riprod.hexcode.entity;

import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.ColorLight;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.DynamicLight;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.execution.ExecutionContext;
import com.riprod.hexcode.execution.HexExecutor;
import com.riprod.hexcode.execution.TargetSet;
import com.riprod.hexcode.glyph.selects.SelectGlyph;
import com.riprod.hexcode.hex.HexNode;
import com.riprod.hexcode.util.HexMathUtil;

import java.util.UUID;

/**
 * Represents a spell beam entity in the world.
 *
 * Created by BEAM select glyphs. Travels as a fast raycast-like
 * projectile and executes children on hit.
 */
public class SpellBeamEntity {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final HexNode pendingNode;
    private final ExecutionContext context;
    private final SelectGlyph selectGlyph;
    private final Ref<EntityStore> caster;

    private Ref<EntityStore> entityRef;
    private Vector3d startPosition;
    private Vector3d endPosition;
    private Vector3d direction;
    private float speed;
    private float maxDistance;
    private float currentLength;
    private boolean resolved;

    public SpellBeamEntity(HexNode pendingNode, ExecutionContext context, SelectGlyph selectGlyph,
                           Vector3d startPosition, Vector3d direction, float speed, float maxDistance) {
        this.pendingNode = pendingNode;
        this.context = context;
        this.selectGlyph = selectGlyph;
        this.caster = context.getCaster();
        this.startPosition = new Vector3d(startPosition);
        this.direction = new Vector3d(direction).normalize();
        this.endPosition = new Vector3d(startPosition);
        this.speed = speed;
        this.maxDistance = maxDistance;
        this.currentLength = 0;
        this.resolved = false;
    }

    /**
     * @return The hex node containing children to execute on hit
     */
    public HexNode getPendingNode() {
        return pendingNode;
    }

    /**
     * @return The execution context
     */
    public ExecutionContext getContext() {
        return context;
    }

    /**
     * @return Start position of the beam
     */
    public Vector3d getStartPosition() {
        return new Vector3d(startPosition);
    }

    /**
     * @return Current end position of the beam
     */
    public Vector3d getEndPosition() {
        return new Vector3d(endPosition);
    }

    /**
     * @return Beam direction
     */
    public Vector3d getDirection() {
        return new Vector3d(direction);
    }

    /**
     * @return Current beam length
     */
    public float getCurrentLength() {
        return currentLength;
    }

    /**
     * @return true if the beam has hit or reached max length
     */
    public boolean isResolved() {
        return resolved;
    }

    /**
     * @return true if the beam has reached max distance without hitting
     */
    public boolean isExpired() {
        return currentLength >= maxDistance;
    }

    /**
     * Update beam extension.
     *
     * @param dt Delta time in seconds
     */
    public void update(float dt) {
        if (resolved) {
            return;
        }

        float extension = speed * dt;
        currentLength = Math.min(currentLength + extension, maxDistance);
        endPosition = new Vector3d(startPosition).add(HexMathUtil.mul(new Vector3d(direction), currentLength));

        if (currentLength >= maxDistance) {
            // Expired without hitting anything
            LOGGER.atInfo().log("Beam reached max distance (%.1f blocks)", maxDistance);
            resolved = true;
        }
    }

    /**
     * Called when the beam hits an entity.
     *
     * @param hitEntity The entity that was hit
     * @param hitPosition The position of impact
     * @param executor The hex executor
     */
    public void onHitEntity(Ref<EntityStore> hitEntity, Vector3d hitPosition, HexExecutor executor) {
        if (resolved) {
            return;
        }

        LOGGER.atInfo().log("Beam hit entity at (%.1f, %.1f, %.1f)",
                hitPosition.x, hitPosition.y, hitPosition.z);

        // Update beam end to hit point
        endPosition = new Vector3d(hitPosition);
        currentLength = (float) HexMathUtil.distance(startPosition, hitPosition);

        // Set the hit target and execute children
        TargetSet targets = TargetSet.of(hitEntity).withOrigin(hitPosition);
        context.pushTargets(targets);

        // Execute all children of the select glyph
        for (HexNode child : pendingNode.getChildren()) {
            executor.executeNode(child, context);
        }

        context.popTargets();
        resolved = true;
    }

    /**
     * Called when the beam hits a block.
     *
     * @param hitPosition The position of impact
     * @param executor The hex executor
     */
    public void onHitBlock(Vector3d hitPosition, HexExecutor executor) {
        if (resolved) {
            return;
        }

        LOGGER.atInfo().log("Beam hit block at (%.1f, %.1f, %.1f)",
                hitPosition.x, hitPosition.y, hitPosition.z);

        // Update beam end to hit point
        endPosition = new Vector3d(hitPosition);
        currentLength = (float) HexMathUtil.distance(startPosition, hitPosition);

        // For block hits, execute with position target only
        TargetSet targets = TargetSet.ofPosition(hitPosition).withOrigin(hitPosition);
        context.pushTargets(targets);

        // Execute all children
        for (HexNode child : pendingNode.getChildren()) {
            executor.executeNode(child, context);
        }

        context.popTargets();
        resolved = true;
    }

    /**
     * Spawn this beam entity in the world.
     *
     * @param store The entity store
     */
    public void spawn(Store<EntityStore> store) {
        if (entityRef != null) {
            LOGGER.atWarning().log("Spell beam already spawned");
            return;
        }

        // Create entity holder
        Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();

        // Add UUID component
        holder.addComponent(UUIDComponent.getComponentType(), new UUIDComponent(UUID.randomUUID()));

        // Add transform component
        TransformComponent transform = new TransformComponent(startPosition, new Vector3f(0, 0, 0));
        holder.addComponent(TransformComponent.getComponentType(), transform);

        // Add dynamic light (beam glow)
        ColorLight beamLight = new ColorLight();
        beamLight.radius = 15;
        beamLight.red = (byte) 200;
        beamLight.green = (byte) 200;
        beamLight.blue = (byte) 255;
        holder.addComponent(DynamicLight.getComponentType(), new DynamicLight(beamLight));

        // Add entity to store
        entityRef = store.addEntity(holder, AddReason.SPAWN);

        LOGGER.atInfo().log("Spawned spell beam from (%.1f, %.1f, %.1f) with speed %.1f",
                startPosition.x, startPosition.y, startPosition.z, speed);
    }

    /**
     * Despawn this beam entity.
     *
     * @param store The entity store
     */
    public void despawn(Store<EntityStore> store) {
        if (entityRef != null) {
            store.removeEntity(entityRef, RemoveReason.REMOVE);
            LOGGER.atInfo().log("Despawned spell beam");
            entityRef = null;
        }
    }
}
