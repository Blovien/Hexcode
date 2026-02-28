package com.riprod.hexcode.core.crafting.utils;

import java.util.ArrayList;
import java.util.List;

import com.hypixel.hytale.math.vector.Vector3f;

public class RadialPositionUtil {
    // Generates positions around a circle around a center point in relative XYZ
    // coordinates as relative offset
    public static List<Vector3f> calculateOffsets(int count, float radius, float angleOffset) {
        List<Vector3f> positions = new ArrayList<>();

        if (count <= 0) {
            return positions;
        }

        float angleStep = (float) (2 * Math.PI / count);

        for (int i = 0; i < count; i++) {
            float yaw = angleStep * i + angleOffset; // Full 360° around the player
            float x = (float) Math.cos(yaw) * radius;
            float z = (float) Math.sin(yaw) * radius;
            positions.add(new Vector3f(x, 0, z));
        }

        return positions;
    }
}
