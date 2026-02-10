package com.riprod.hexcode.core.casting.component;

import javax.annotation.Nonnull;

import com.riprod.hexcode.utils.SphericalPosition;

import java.util.List;

public interface CastingStyle {

    @Nonnull
    String getStyleId();

    @Nonnull
    List<SphericalPosition> getInitialPositions(int glyphCount, float lookYaw, float lookPitch);
}
