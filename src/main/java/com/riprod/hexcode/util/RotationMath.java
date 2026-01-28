package com.riprod.hexcode.util;

import com.hypixel.hytale.math.vector.Vector3d;
import com.riprod.hexcode.math.GlyphRotation;

/**
 * Utility methods for rotation-based calculations.
 *
 * <p>This class provides static methods for:
 * <ul>
 *   <li>Angular distance calculations between rotations</li>
 *   <li>Converting between direction vectors and pitch/yaw</li>
 *   <li>Tolerance constants for selection and composition</li>
 * </ul>
 */
public final class RotationMath {

    // --- Constants ---

    /** Base selection tolerance for single glyphs (in degrees) */
    public static final float BASE_TOLERANCE_DEGREES = 5.0f;

    /** Selection tolerance for composed hexes (parent nodes) (in degrees) */
    public static final float COMPOSED_TOLERANCE_DEGREES = 7.0f;

    /** Tolerance decay multiplier for inner tiers */
    public static final float TOLERANCE_DECAY = 0.8f;

    /** Minimum tolerance for innermost tiers (in degrees) */
    public static final float MIN_TOLERANCE_DEGREES = 2.0f;

    /** Default distance from player eyes to glyph (in blocks) */
    public static final float DEFAULT_GLYPH_DISTANCE = 2.0f;

    /** Player eye height above feet position (in blocks) */
    public static final float EYE_HEIGHT = 1.62f;

    /** Ring spawn pitch - slightly below eye level (in degrees) */
    public static final float RING_PITCH_DEGREES = -9.0f;

    private RotationMath() {
        // Utility class
    }

    // --- Angular Distance ---

    /**
     * Calculate the angular distance between two direction vectors (in degrees).
     *
     * @param dir1 First direction vector (does not need to be normalized)
     * @param dir2 Second direction vector (does not need to be normalized)
     * @return Angular distance in degrees [0, 180]
     */
    public static float angularDistance(Vector3d dir1, Vector3d dir2) {
        // Normalize both vectors
        double len1 = Math.sqrt(dir1.x * dir1.x + dir1.y * dir1.y + dir1.z * dir1.z);
        double len2 = Math.sqrt(dir2.x * dir2.x + dir2.y * dir2.y + dir2.z * dir2.z);

        if (len1 < 1e-8 || len2 < 1e-8) {
            return 180.0f; // Undefined direction = maximum distance
        }

        // Dot product of normalized vectors = cos(angle)
        double dot = (dir1.x * dir2.x + dir1.y * dir2.y + dir1.z * dir2.z) / (len1 * len2);

        // Clamp to handle floating point errors
        dot = Math.max(-1.0, Math.min(1.0, dot));

        return (float) Math.toDegrees(Math.acos(dot));
    }

    /**
     * Calculate the angular distance between a look direction and an element position.
     *
     * @param eyePosition The player's eye position
     * @param lookDirection The player's look direction (from HeadRotation)
     * @param elementPosition The element's world position
     * @return Angular distance in degrees [0, 180]
     */
    public static float angularDistanceToPosition(Vector3d eyePosition, Vector3d lookDirection,
                                                   Vector3d elementPosition) {
        // Direction from eye to element
        Vector3d toElement = new Vector3d(
            elementPosition.x - eyePosition.x,
            elementPosition.y - eyePosition.y,
            elementPosition.z - eyePosition.z
        );

        return angularDistance(lookDirection, toElement);
    }

    /**
     * Check if an element is within the selection tolerance.
     *
     * @param eyePosition The player's eye position
     * @param lookDirection The player's look direction
     * @param elementPosition The element's world position
     * @param toleranceDegrees The selection tolerance in degrees
     * @return true if the element is within tolerance
     */
    public static boolean isWithinTolerance(Vector3d eyePosition, Vector3d lookDirection,
                                            Vector3d elementPosition, float toleranceDegrees) {
        return angularDistanceToPosition(eyePosition, lookDirection, elementPosition) <= toleranceDegrees;
    }

    // --- Pitch/Yaw Calculations ---

    /**
     * Extract pitch from a direction vector.
     *
     * @param direction The direction vector (does not need to be normalized)
     * @return Pitch in degrees. Negative = up, positive = down.
     */
    public static float pitchFromDirection(Vector3d direction) {
        double length = Math.sqrt(direction.x * direction.x + direction.y * direction.y + direction.z * direction.z);
        if (length < 1e-8) {
            return 0.0f;
        }
        double ny = direction.y / length;
        // arcsin(-y) because +pitch = down = -y
        return (float) Math.toDegrees(Math.asin(-ny));
    }

    /**
     * Extract yaw from a direction vector.
     *
     * @param direction The direction vector (does not need to be normalized)
     * @return Yaw in degrees. 0 = forward (+Z), 90 = right (+X).
     */
    public static float yawFromDirection(Vector3d direction) {
        double length = Math.sqrt(direction.x * direction.x + direction.y * direction.y + direction.z * direction.z);
        if (length < 1e-8) {
            return 0.0f;
        }
        double nx = direction.x / length;
        double nz = direction.z / length;
        return (float) Math.toDegrees(Math.atan2(nx, nz));
    }

    /**
     * Convert pitch and yaw to a normalized direction vector.
     *
     * @param pitch Pitch in degrees. Negative = up, positive = down.
     * @param yaw Yaw in degrees. 0 = forward (+Z), 90 = right (+X).
     * @return Normalized direction vector
     */
    public static Vector3d directionFromPitchYaw(float pitch, float yaw) {
        double pitchRad = Math.toRadians(pitch);
        double yawRad = Math.toRadians(yaw);

        double cosPitch = Math.cos(pitchRad);
        double sinPitch = Math.sin(pitchRad);
        double sinYaw = Math.sin(yawRad);
        double cosYaw = Math.cos(yawRad);

        return new Vector3d(
            sinYaw * cosPitch,  // x
            -sinPitch,          // y (negated: +pitch = down = -y)
            cosYaw * cosPitch   // z
        );
    }

    // --- Tier Tolerance ---

    /**
     * Calculate the selection tolerance for a hex tier.
     *
     * <p>Tier 0 (outermost/root) has COMPOSED_TOLERANCE.
     * Each inner tier has tolerance = previous * TOLERANCE_DECAY,
     * but never less than MIN_TOLERANCE_DEGREES.
     *
     * @param tier The tier level (0 = outermost)
     * @return Selection tolerance in degrees
     */
    public static float getToleranceForTier(int tier) {
        if (tier <= 0) {
            return COMPOSED_TOLERANCE_DEGREES;
        }

        float tolerance = COMPOSED_TOLERANCE_DEGREES;
        for (int i = 0; i < tier; i++) {
            tolerance *= TOLERANCE_DECAY;
        }
        return Math.max(tolerance, MIN_TOLERANCE_DEGREES);
    }

    // --- Ring Distribution ---

    /**
     * Calculate the yaw angle for an element in a ring distribution.
     *
     * @param index Element index (0-based)
     * @param total Total number of elements
     * @param startYaw Starting yaw angle (typically player's current look yaw)
     * @return Yaw angle in degrees for this element
     */
    public static float getRingYaw(int index, int total, float startYaw) {
        if (total <= 0) {
            return startYaw;
        }
        float angleStep = 360.0f / total;
        return normalizeYaw(startYaw + angleStep * index);
    }

    /**
     * Calculate a GlyphRotation for an element in a ring distribution.
     *
     * @param index Element index (0-based)
     * @param total Total number of elements
     * @param startYaw Starting yaw angle (typically player's current look yaw)
     * @return GlyphRotation for this element
     */
    public static GlyphRotation getRingRotation(int index, int total, float startYaw) {
        float yaw = getRingYaw(index, total, startYaw);
        return new GlyphRotation(RING_PITCH_DEGREES, yaw);
    }

    // --- Normalization ---

    /**
     * Normalize yaw to range [-180, +180].
     */
    public static float normalizeYaw(float yaw) {
        yaw = yaw % 360.0f;
        if (yaw > 180.0f) {
            yaw -= 360.0f;
        } else if (yaw < -180.0f) {
            yaw += 360.0f;
        }
        return yaw;
    }

    /**
     * Clamp pitch to valid range [-90, +90].
     */
    public static float clampPitch(float pitch) {
        return Math.max(-90.0f, Math.min(90.0f, pitch));
    }

    // --- Eye Position ---

    /**
     * Calculate player eye position from feet position.
     *
     * @param playerPosition The player's feet position
     * @return The player's eye position
     */
    public static Vector3d getEyePosition(Vector3d playerPosition) {
        return new Vector3d(playerPosition.x, playerPosition.y + EYE_HEIGHT, playerPosition.z);
    }

    /**
     * Calculate world position from rotation and player position.
     *
     * @param rotation The glyph rotation
     * @param playerPosition The player's feet position
     * @return World position at DEFAULT_GLYPH_DISTANCE from eyes
     */
    public static Vector3d rotationToWorldPosition(GlyphRotation rotation, Vector3d playerPosition) {
        return rotation.toWorldPosition(playerPosition, DEFAULT_GLYPH_DISTANCE);
    }

    /**
     * Calculate GlyphRotation from world position and player position.
     *
     * @param worldPosition The glyph's world position
     * @param playerPosition The player's feet position
     * @return GlyphRotation pointing from eyes to world position
     */
    public static GlyphRotation worldPositionToRotation(Vector3d worldPosition, Vector3d playerPosition) {
        Vector3d eyePosition = getEyePosition(playerPosition);
        return GlyphRotation.fromWorldPosition(eyePosition, worldPosition);
    }
}
