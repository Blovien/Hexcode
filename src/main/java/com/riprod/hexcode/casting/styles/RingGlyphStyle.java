package com.riprod.hexcode.casting.styles;

import java.util.List;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.entity.HexNodeEntity;
import com.riprod.hexcode.math.GlyphRotation;
import com.riprod.hexcode.util.RotationMath;

/**
 * Ring-style spawn positioning.
 *
 * <p>
 * Elements are spawned in a horizontal circle around the player using
 * rotation-based
 * positioning. All glyphs are placed at a fixed pitch (slightly below eye
 * level) with
 * yaw angles distributed evenly around the player.
 *
 * <p>
 * This style manages:
 * <ul>
 * <li>Ring spawn rotation calculation</li>
 * <li>Visual effects (magic wheel, particles)</li>
 * </ul>
 */
public class RingGlyphStyle extends BaseGlyphStyle {

    /** Ring pitch in degrees (slightly below eye level, negative = up) */
    private float ringPitch;

    /** Starting yaw offset for the first element */
    private float startYaw;

    /** Default ring pitch (slightly below eye level) */
    public static final float DEFAULT_RING_PITCH = -9.0f;

    /** Default starting yaw (forward) */
    public static final float DEFAULT_START_YAW = 0.0f;

    public RingGlyphStyle() {
        super();
        this.ringPitch = DEFAULT_RING_PITCH;
        this.startYaw = DEFAULT_START_YAW;
    }

    /**
     * Create a ring style with custom parameters.
     *
     * @param ringPitch The pitch angle for the ring (degrees, negative = up)
     * @param startYaw  The starting yaw angle (degrees)
     */
    public RingGlyphStyle(float ringPitch, float startYaw) {
        super();
        this.ringPitch = ringPitch;
        this.startYaw = startYaw;
    }

    // --- Spawn Rotation Calculation ---

    /**
     * Get the spawn rotation for an element at a given index.
     *
     * <p>
     * Elements are distributed evenly around the player at the ring pitch.
     *
     * @param index Element index (0-based)
     * @param total Total number of elements
     * @return GlyphRotation for this element
     */
    public GlyphRotation getSpawnRotation(float rotation, int index, int total) {
        float baseYaw = RotationMath.normalizeYaw(startYaw + rotation);
        if (total == 0) {
            return new GlyphRotation(ringPitch, baseYaw);
        }

        float yawStep = 360.0f / total;
        float yaw = RotationMath.normalizeYaw(baseYaw + yawStep * index);

        LOGGER.atInfo().log("RingGlyphStyle.getSpawnRotation() called. Offset %.2f. Spawning element %d of %d at pitch %.2f, yaw %.2f", rotation, index, total, ringPitch, yaw);
        return new GlyphRotation(ringPitch, yaw);
    }

    // --- Mode Lifecycle ---

    @Override
    public void onModeEnter(CommandBuffer<EntityStore> commandBuffer, Vector3d playerPosition) {
        // TODO: Spawn magic wheel entity around player
        // Future: spawn particles, AOE indicators
    }

    @Override
    public void onModeExit(CommandBuffer<EntityStore> commandBuffer) {
        // TODO: Despawn magic wheel entity
        // Future: cleanup particles, AOE indicators
    }

    // --- Visual Effects ---

    @Override
    public void updateEffects(Store<EntityStore> store, List<HexNodeEntity> elements, float dt) {
        // TODO: Update particle positions attached to glyphs
        // Future: AOE effect updates
    }

    // --- Configuration ---

    /**
     * @return The ring pitch in degrees (negative = up)
     */
    public float getRingPitch() {
        return ringPitch;
    }

    /**
     * Set the ring pitch.
     *
     * @param ringPitch New pitch in degrees
     */
    public void setRingPitch(float ringPitch) {
        this.ringPitch = ringPitch;
    }

    /**
     * @return The starting yaw in degrees
     */
    public float getStartYaw() {
        return startYaw;
    }

    /**
     * Set the starting yaw.
     *
     * @param startYaw New starting yaw in degrees
     */
    public void setStartYaw(float startYaw) {
        this.startYaw = startYaw;
    }
}
