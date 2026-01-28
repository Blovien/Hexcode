package com.riprod.hexcode.math;

import com.hypixel.hytale.math.vector.Vector3d;

/**
 * Represents a rotation (pitch/yaw) for positioning orbital elements relative to the player's eyes.
 *
 * <p>Orbital elements (glyphs and hexes) are positioned using spherical coordinates:
 * <ul>
 *   <li><b>Pitch</b>: Vertical angle in degrees. Negative = up, positive = down. Range: [-90, +90]</li>
 *   <li><b>Yaw</b>: Horizontal angle in degrees. 0 = forward (+Z), 90 = right (+X). Range: [-180, +180]</li>
 * </ul>
 *
 * <p>World position is calculated as: eyePosition + direction(pitch, yaw) * distance
 *
 * <p>This is immutable. Use {@link #withPitch(float)}, {@link #withYaw(float)}, or the static factory
 * methods to create new instances.
 */
public final class GlyphRotation {

    /** Default distance from player eyes to glyph (in blocks) */
    public static final float DEFAULT_DISTANCE = 2.0f;

    /** Player eye height above feet position (in blocks) */
    public static final float EYE_HEIGHT = 1.62f;

    /** Base selection tolerance for single glyphs (in degrees) */
    public static final float BASE_TOLERANCE = 10.0f;

    /** Selection tolerance for composed hexes (parent nodes) (in degrees) */
    public static final float COMPOSED_TOLERANCE = 10.0f;

    /** Tolerance decay multiplier for inner tiers */
    public static final float TOLERANCE_DECAY = 0.8f;

    private final float pitch;
    private final float yaw;

    /**
     * Create a new GlyphRotation.
     *
     * @param pitch Vertical angle in degrees. Negative = up, positive = down.
     * @param yaw Horizontal angle in degrees. 0 = forward (+Z), 90 = right (+X).
     */
    public GlyphRotation(float pitch, float yaw) {
        this.pitch = clampPitch(pitch);
        this.yaw = normalizeYaw(yaw);
    }

    /**
     * Create a GlyphRotation from a direction vector.
     *
     * @param direction The direction vector (does not need to be normalized)
     * @return A GlyphRotation representing that direction
     */
    public static GlyphRotation fromDirection(Vector3d direction) {
        // Normalize the direction
        double length = Math.sqrt(direction.x * direction.x + direction.y * direction.y + direction.z * direction.z);
        if (length < 1e-8) {
            return new GlyphRotation(0, 0);
        }

        double nx = direction.x / length;
        double ny = direction.y / length;
        double nz = direction.z / length;

        // Calculate pitch: arcsin(-y) gives angle from horizontal
        // Negative y = looking up = negative pitch
        float pitch = (float) Math.toDegrees(Math.asin(-ny));

        // Calculate yaw: atan2(x, z) gives angle from +Z axis
        float yaw = (float) Math.toDegrees(Math.atan2(nx, nz));

        return new GlyphRotation(pitch, yaw);
    }

    /**
     * Create a GlyphRotation from player look direction (HeadRotation vector).
     *
     * @param lookDirection The player's look direction from HeadRotation.getDirection()
     * @return A GlyphRotation matching the player's gaze
     */
    public static GlyphRotation fromLookDirection(Vector3d lookDirection) {
        return fromDirection(lookDirection);
    }

    /**
     * Create a GlyphRotation from a world position relative to player eyes.
     *
     * @param eyePosition The player's eye position
     * @param worldPosition The target world position
     * @return A GlyphRotation pointing from eyes to target
     */
    public static GlyphRotation fromWorldPosition(Vector3d eyePosition, Vector3d worldPosition) {
        Vector3d direction = new Vector3d(
            worldPosition.x - eyePosition.x,
            worldPosition.y - eyePosition.y,
            worldPosition.z - eyePosition.z
        );
        return fromDirection(direction);
    }

    // --- Getters ---

    /**
     * @return Vertical angle in degrees. Negative = up, positive = down.
     */
    public float getPitch() {
        return pitch;
    }

    /**
     * @return Horizontal angle in degrees. 0 = forward (+Z), 90 = right (+X).
     */
    public float getYaw() {
        return yaw;
    }

    // --- Immutable Modifiers ---

    /**
     * Create a new GlyphRotation with a different pitch.
     */
    public GlyphRotation withPitch(float newPitch) {
        return new GlyphRotation(newPitch, this.yaw);
    }

    /**
     * Create a new GlyphRotation with a different yaw.
     */
    public GlyphRotation withYaw(float newYaw) {
        return new GlyphRotation(this.pitch, newYaw);
    }

    // --- Conversions ---

    /**
     * Convert this rotation to a normalized direction vector.
     *
     * @return Unit vector pointing in this rotation's direction
     */
    public Vector3d toDirection() {
        double pitchRad = Math.toRadians(pitch);
        double yawRad = Math.toRadians(yaw);

        // cos(pitch) = horizontal component magnitude
        // sin(pitch) = vertical component (negated because +pitch = down)
        double cosPitch = Math.cos(pitchRad);
        double sinPitch = Math.sin(pitchRad);

        // sin(yaw) = x component, cos(yaw) = z component
        double sinYaw = Math.sin(yawRad);
        double cosYaw = Math.cos(yawRad);

        return new Vector3d(
            sinYaw * cosPitch,  // x
            -sinPitch,          // y (negated: +pitch = down = -y)
            cosYaw * cosPitch   // z
        );
    }

    /**
     * Calculate the world position for this rotation relative to a player.
     *
     * @param playerPosition The player's feet position
     * @return World position at DEFAULT_DISTANCE from player eyes
     */
    public Vector3d toWorldPosition(Vector3d playerPosition) {
        return toWorldPosition(playerPosition, DEFAULT_DISTANCE);
    }

    /**
     * Calculate the world position for this rotation relative to a player.
     *
     * @param playerPosition The player's feet position
     * @param distance Distance from player eyes
     * @return World position at specified distance from player eyes
     */
    public Vector3d toWorldPosition(Vector3d playerPosition, float distance) {
        Vector3d direction = toDirection();
        return new Vector3d(
            playerPosition.x + direction.x * distance,
            playerPosition.y + EYE_HEIGHT + direction.y * distance,
            playerPosition.z + direction.z * distance
        );
    }

    /**
     * Calculate the world position using a specific eye position.
     *
     * @param eyePosition The eye position (already includes height offset)
     * @param distance Distance from eyes
     * @return World position at specified distance from eyes
     */
    public Vector3d toWorldPositionFromEyes(Vector3d eyePosition, float distance) {
        Vector3d direction = toDirection();
        return new Vector3d(
            eyePosition.x + direction.x * distance,
            eyePosition.y + direction.y * distance,
            eyePosition.z + direction.z * distance
        );
    }

    // --- Angular Distance ---

    /**
     * Calculate the angular distance to another rotation (in degrees).
     *
     * <p>Uses the great-circle distance formula for accurate spherical distance.
     *
     * @param other The other rotation
     * @return Angular distance in degrees [0, 180]
     */
    public float angularDistanceTo(GlyphRotation other) {
        Vector3d dir1 = this.toDirection();
        Vector3d dir2 = other.toDirection();

        // Dot product of unit vectors = cos(angle)
        double dot = dir1.x * dir2.x + dir1.y * dir2.y + dir1.z * dir2.z;

        // Clamp to [-1, 1] to handle floating point errors
        dot = Math.max(-1.0, Math.min(1.0, dot));

        return (float) Math.toDegrees(Math.acos(dot));
    }

    /**
     * Check if this rotation is within a tolerance of another rotation.
     *
     * @param other The other rotation
     * @param toleranceDegrees The tolerance in degrees
     * @return true if within tolerance
     */
    public boolean isWithinTolerance(GlyphRotation other, float toleranceDegrees) {
        return angularDistanceTo(other) <= toleranceDegrees;
    }

    /**
     * Check if this rotation is within the base selection tolerance of another.
     *
     * @param other The other rotation
     * @return true if within BASE_TOLERANCE degrees
     */
    public boolean isWithinBaseTolerance(GlyphRotation other) {
        return isWithinTolerance(other, BASE_TOLERANCE);
    }

    // --- Utility ---

    /**
     * Clamp pitch to valid range [-90, +90].
     */
    private static float clampPitch(float pitch) {
        return Math.max(-90.0f, Math.min(90.0f, pitch));
    }

    /**
     * Normalize yaw to range [-180, +180].
     */
    private static float normalizeYaw(float yaw) {
        yaw = yaw % 360.0f;
        if (yaw > 180.0f) {
            yaw -= 360.0f;
        } else if (yaw < -180.0f) {
            yaw += 360.0f;
        }
        return yaw;
    }

    /**
     * Interpolate between two rotations.
     *
     * @param from Start rotation
     * @param to End rotation
     * @param t Interpolation factor [0, 1]
     * @return Interpolated rotation
     */
    public static GlyphRotation lerp(GlyphRotation from, GlyphRotation to, float t) {
        t = Math.max(0.0f, Math.min(1.0f, t));

        // Handle yaw wrapping
        float fromYaw = from.yaw;
        float toYaw = to.yaw;

        // Take the shortest path around the circle
        float yawDiff = toYaw - fromYaw;
        if (yawDiff > 180.0f) {
            yawDiff -= 360.0f;
        } else if (yawDiff < -180.0f) {
            yawDiff += 360.0f;
        }

        float newPitch = from.pitch + (to.pitch - from.pitch) * t;
        float newYaw = fromYaw + yawDiff * t;

        return new GlyphRotation(newPitch, newYaw);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof GlyphRotation)) return false;
        GlyphRotation other = (GlyphRotation) obj;
        return Float.compare(pitch, other.pitch) == 0 && Float.compare(yaw, other.yaw) == 0;
    }

    @Override
    public int hashCode() {
        int result = Float.floatToIntBits(pitch);
        result = 31 * result + Float.floatToIntBits(yaw);
        return result;
    }

    @Override
    public String toString() {
        return String.format("GlyphRotation(pitch=%.1f, yaw=%.1f)", pitch, yaw);
    }
}
