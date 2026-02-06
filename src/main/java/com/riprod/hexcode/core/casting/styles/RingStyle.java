package com.riprod.hexcode.core.casting.styles;

import javax.annotation.Nonnull;

import com.riprod.hexcode.core.casting.utils.CastingStyle;
import com.riprod.hexcode.utils.SphericalPosition;

import java.util.ArrayList;
import java.util.List;

public class RingStyle implements CastingStyle {

    public static final String ID = "ring";
    private static final float DEFAULT_DISTANCE = 2.0f;
    private static final float RING_PITCH = 0.0f; // Horizontal ring at eye level

    @Nonnull
    @Override
    public String getStyleId() {
        return ID;
    }

    @Nonnull
    @Override
    public List<SphericalPosition> getInitialPositions(int glyphCount, float lookYaw, float lookPitch) {
        List<SphericalPosition> positions = new ArrayList<>();

        if (glyphCount <= 0) {
            return positions;
        }

        float angleStep = (float) (2 * Math.PI / glyphCount);

        for (int i = 0; i < glyphCount; i++) {
            float yaw = angleStep * i; // Full 360° around the player
            positions.add(new SphericalPosition(yaw, RING_PITCH, DEFAULT_DISTANCE));
        }

        return positions;
    }
}
