package com.riprod.hexcode.core.state.casting.component;

import javax.annotation.Nonnull;

import org.joml.Vector3f;

import java.util.List;

public interface CastingStyle {

    @Nonnull
    String getStyleId();

    @Nonnull
    List<Vector3f> getInitialPositions(int glyphCount, float lookYaw, float lookPitch);
}
