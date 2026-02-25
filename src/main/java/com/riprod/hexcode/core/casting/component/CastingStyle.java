package com.riprod.hexcode.core.casting.component;

import javax.annotation.Nonnull;

import com.hypixel.hytale.math.vector.Vector3f;
import com.riprod.hexcode.utils.SphericalPosition;

import java.util.List;

public interface CastingStyle {

    @Nonnull
    String getStyleId();

    @Nonnull
    List<Vector3f> getInitialPositions(int glyphCount, float lookYaw, float lookPitch);
}
