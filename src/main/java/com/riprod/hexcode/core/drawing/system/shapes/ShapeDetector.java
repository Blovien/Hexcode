package com.riprod.hexcode.core.drawing.system.shapes;

import com.riprod.hexcode.core.drawing.component.DrawnShapeComponent;

import it.unimi.dsi.fastutil.floats.FloatArrayList;

public interface ShapeDetector {

    DrawnShapeComponent detect(FloatArrayList points, float minYaw, float maxYaw, float minPitch, float maxPitch);

    String getName();

    void ensureLoaded();

    void clearCache();
}
