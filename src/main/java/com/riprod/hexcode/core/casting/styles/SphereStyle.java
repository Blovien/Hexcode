package com.riprod.hexcode.core.casting.styles;

import javax.annotation.Nonnull;

import com.riprod.hexcode.core.casting.utils.CastingStyle;
import com.riprod.hexcode.utils.SphericalPosition;

import java.util.ArrayList;
import java.util.List;

public class SphereStyle implements CastingStyle {

    public static final String ID = "sphere";
    private static final float DEFAULT_DISTANCE = 3.0f;

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
            positions.add(new SphericalPosition(0f, (float) (Math.PI / 4), DEFAULT_DISTANCE));
            return positions;
        }

        // distribute points across the upper hemisphere using fibonacci sphere
        float goldenAngle = (float) (Math.PI * (3.0 - Math.sqrt(5.0)));

        for (int i = 0; i < glyphCount; i++) {
            // y ranges from 0 (equator) to 1 (top) for upper hemisphere
            float y = (float) i / (glyphCount - 1);
            float pitch = (float) Math.asin(y); // 0 at equator, pi/2 at top

            float yaw = goldenAngle * i;

            positions.add(new SphericalPosition(yaw, pitch, DEFAULT_DISTANCE));
        }

        return positions;
    }
}
