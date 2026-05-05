package com.riprod.hexcode.core.state.drawing.utils;

import javax.annotation.Nullable;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.Color;
import com.hypixel.hytale.protocol.packets.connection.PongType;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.state.drawing.component.DrawnShapeComponent;
import com.riprod.hexcode.core.state.drawing.system.InterfaceManager;
import com.riprod.hexcode.core.state.drawing.system.shapes.DollarOneFixedDetector;
import com.riprod.hexcode.core.state.drawing.system.shapes.ShapeDetector;

import it.unimi.dsi.fastutil.floats.FloatArrayList;

public final class StrokeCapture {
    private static final float MIN_RAD_DELTA = (float) Math.toRadians(0.5);
    private static final float MIN_RAD_DELTA_SQ = MIN_RAD_DELTA * MIN_RAD_DELTA;
    private static final float TWO_PI = (float) (2 * Math.PI);
    private static final ShapeDetector DEFAULT_DETECTOR = new DollarOneFixedDetector();

    private StrokeCapture() {
    }

    public static boolean appendHeadSample(FloatArrayList points, HeadRotation head) {
        if (points == null || head == null) {
            return false;
        }
        float yaw = head.getRotation().getYaw();
        float pitch = head.getRotation().getPitch();

        if (!points.isEmpty()) {
            float lastYaw = points.getFloat(points.size() - 2);
            float lastPitch = points.getFloat(points.size() - 1);
            float dy = yaw - lastYaw;
            if (dy > (float) Math.PI) dy -= TWO_PI;
            else if (dy < -(float) Math.PI) dy += TWO_PI;
            yaw = lastYaw + dy;

            float dp = pitch - lastPitch;
            if (dy * dy + dp * dp < MIN_RAD_DELTA_SQ) {
                return false;
            }
        }

        points.add(yaw);
        points.add(pitch);
        return true;
    }

    @Nullable
    public static DrawnShapeComponent recognizeStroke(ComponentAccessor<EntityStore> accessor,
            Ref<EntityStore> playerRef, FloatArrayList points, ShapeDetector detector,
            long drawDurationMs) {
        if (points == null || points.size() < 3) {
            return null;
        }
        ShapeDetector det = detector != null ? detector : DEFAULT_DETECTOR;

        float minYaw = Float.MAX_VALUE, maxYaw = -Float.MAX_VALUE;
        float minPitch = Float.MAX_VALUE, maxPitch = -Float.MAX_VALUE;
        for (int i = 0; i < points.size(); i += 2) {
            float yaw = points.getFloat(i);
            float pitch = points.getFloat(i + 1);
            if (yaw < minYaw) minYaw = yaw;
            if (yaw > maxYaw) maxYaw = yaw;
            if (pitch < minPitch) minPitch = pitch;
            if (pitch > maxPitch) maxPitch = pitch;
        }

        DrawnShapeComponent result = det.detect(points, minYaw, maxYaw, minPitch, maxPitch);
        if (result == null) {
            return null;
        }

        result.setPoints(InterfaceManager.getPositionsFromAngles(accessor, points, playerRef, 4.0f));
        Color color = InterfaceManager.getColorFromQuality(result.getVolatility());
        result.setColor(color);
        result.setSpeed(drawDurationMs);

        float forgiveness = pingForgiveness(accessor, playerRef);
        result.setVolatility(Math.min(1f, result.getVolatility() * forgiveness));

        float maxSize = Math.max(Math.abs(maxYaw - minYaw), Math.abs(maxPitch - minPitch));
        result.setSize(maxSize);
        return result;
    }

    private static float pingForgiveness(ComponentAccessor<EntityStore> accessor, Ref<EntityStore> playerRef) {
        try {
            int pingMs = (int) accessor.getComponent(playerRef, PlayerRef.getComponentType())
                    .getPacketHandler()
                    .getPingInfo(PongType.Direct)
                    .getPingMetricSet()
                    .getLastValue();
            return Math.min(3f, 1f + Math.min(pingMs, 500) / 1000f);
        } catch (Exception e) {
            return 1f;
        }
    }
}
