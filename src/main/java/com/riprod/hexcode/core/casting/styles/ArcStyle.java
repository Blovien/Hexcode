package com.riprod.hexcode.core.casting.styles;

import javax.annotation.Nonnull;

import com.riprod.hexcode.core.casting.utils.CastingStyle;
import com.riprod.hexcode.utils.SphericalPosition;

import java.util.ArrayList;
import java.util.List;

public class ArcStyle implements CastingStyle {

    public static final String ID = "arc";
    private static final float DEFAULT_DISTANCE = 3.0f;
    private static final float ARC_PITCH = 0.0f;
    private static final float ARC_SPAN = (float) (2 * Math.PI / 3); // 120 degrees

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

        if (glyphCount == 1) {
            positions.add(new SphericalPosition(lookYaw, ARC_PITCH, DEFAULT_DISTANCE));
            return positions;
        }

        float startYaw = lookYaw - ARC_SPAN / 2;
        float angleStep = ARC_SPAN / (glyphCount - 1);

        for (int i = 0; i < glyphCount; i++) {
            float yaw = startYaw + angleStep * i;
            positions.add(new SphericalPosition(yaw, ARC_PITCH, DEFAULT_DISTANCE));
        }

        return positions;
    }
}
