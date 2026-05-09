package com.riprod.hexcode.builtin.staffStyles;

import javax.annotation.Nonnull;

import com.hypixel.hytale.math.vector.Vector3f;
import com.riprod.hexcode.core.state.casting.component.CastingStyle;

import java.util.ArrayList;
import java.util.List;

public class RingStyle implements CastingStyle {

    public static final String ID = "ring";
    private static final float DEFAULT_DISTANCE = 3.0f;
    private static final float RING_PITCH = 0.0f; // Horizontal ring at eye level

    @Nonnull
    @Override
    public String getStyleId() {
        return ID;
    }

    @Nonnull
    @Override
    public List<Vector3f> getInitialPositions(int glyphCount, float lookYaw, float lookPitch) {
        List<Vector3f> positions = new ArrayList<>();

        if (glyphCount <= 0) {
            return positions;
        }

        float angleStep = (float) (2 * Math.PI / glyphCount);

        for (int i = 0; i < glyphCount; i++) {
            float yaw = angleStep * i + lookYaw; // Full 360° around the player
            positions.add(new Vector3f(RING_PITCH, yaw, DEFAULT_DISTANCE));
        }

        return positions;
    }
}
