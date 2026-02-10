package com.riprod.hexcode.core.drawing.system;

import java.util.List;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.Color;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.drawing.component.DrawnShapeComponent;
import com.riprod.hexcode.core.drawing.utils.DrawRasterizer;
import com.riprod.hexcode.core.drawing.utils.ShapeComparator;
import com.riprod.hexcode.player.component.HexcasterComponent;

import it.unimi.dsi.fastutil.floats.FloatArrayList;

public class DrawingManager {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static InteractionState StartDrawing(ComponentAccessor<EntityStore> accessor,
            HexcasterComponent hexcaster,
            Ref<EntityStore> playerRef) {
        // Mouse down begin tracking

        HeadRotation head = accessor.getComponent(playerRef, HeadRotation.getComponentType());
        if (head == null)
            return InteractionState.Failed;

        InterfaceManager.spawnTrails(accessor, playerRef, head);
        return InteractionState.NotFinished;
    }

    public static InteractionState StopDrawing(ComponentAccessor<EntityStore> accessor,
            HexcasterComponent hexcaster,
            Ref<EntityStore> playerRef) {

        FloatArrayList points = hexcaster.getDrawnStrokes();
        if (points == null || points.size() < 3) {
            hexcaster.clearStrokes();
            return InteractionState.Finished; // too few points, discard
        }

        // 1. compute bounding box
        float minYaw = Float.MAX_VALUE, maxYaw = -Float.MAX_VALUE;
        float minPitch = Float.MAX_VALUE, maxPitch = -Float.MAX_VALUE;
        for (int i = 0; i < points.size(); i += 2) {
            float yaw = points.getFloat(i);
            float pitch = points.getFloat(i + 1);
            minYaw = Math.min(minYaw, yaw);
            maxYaw = Math.max(maxYaw, yaw);
            minPitch = Math.min(minPitch, pitch);
            maxPitch = Math.max(maxPitch, pitch);
        }

        boolean[][] grid = DrawRasterizer.rasterize(points, 32,
                minYaw, maxYaw, minPitch, maxPitch);

        DrawnShapeComponent result = ShapeComparator.getShape(grid);

        // get the list of particles to spawn for the shape reference component
        List<Vector3d> drawnGlyphs = InterfaceManager.getPositionsFromAngles(accessor, points, playerRef, 2.0f);
        Color color = InterfaceManager.getColorFromQuality(result.getAccuracy());
        result.setColor(color);
        result.setPoints(drawnGlyphs);

        LOGGER.atInfo().log("Drawn shape: %s with quality %f", (result != null ? result.getGlyphId() : "null"), result.getAccuracy());

        InterfaceManager.removeTrails(accessor, playerRef);
        hexcaster.addDrawnGlyph(result);
        hexcaster.clearStrokes();
        InterfaceManager.spawnParticles(accessor, playerRef, hexcaster);

        return InteractionState.Finished;
    }

    public static InteractionState DrawTick(ComponentAccessor<EntityStore> accessor,
            HexcasterComponent hexcaster,
            Ref<EntityStore> playerRef) {

        // Tick while drawing. Get rotation
        FloatArrayList points = hexcaster.getDrawnStrokes();
        if (points == null)
            return InteractionState.Finished;

        HeadRotation head = accessor.getComponent(playerRef, HeadRotation.getComponentType());
        if (head == null)
            return InteractionState.Failed;

        float yaw = (float) Math.toDegrees(head.getRotation().getYaw());
        float pitch = (float) Math.toDegrees(head.getRotation().getPitch());

        // skip if barely moved (reduces redundant points)
        if (!points.isEmpty()) {
            float lastYaw = points.getFloat(points.size() - 2);
            float lastPitch = points.getFloat(points.size() - 1);
            float dist = Math.abs(yaw - lastYaw) + Math.abs(pitch - lastPitch);
            if (dist < 0.5f)
                return InteractionState.NotFinished;
        }

        points.add(yaw);
        points.add(pitch);

        // spawn particle at the look position
        InterfaceManager.positionTrail(accessor, playerRef, head);
        return InteractionState.NotFinished;
    }
}
