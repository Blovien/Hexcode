package com.riprod.hexcode.core.drawing;

import java.util.List;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.Color;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.time.TimeResource;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.drawing.component.DrawnShapeComponent;
import com.riprod.hexcode.core.drawing.system.GlyphCreationManager;
import com.riprod.hexcode.core.drawing.system.InterfaceManager;
import com.riprod.hexcode.core.drawing.utils.DrawRasterizer;
import com.riprod.hexcode.core.drawing.utils.ShapeComparator;
import com.riprod.hexcode.core.glyphs.component.GlyphComponent;
import com.riprod.hexcode.core.glyphs.registry.GlyphAsset;
import com.riprod.hexcode.core.hexbook.component.HexBookComponent;
import com.riprod.hexcode.core.hexcaster.component.HexcasterComponent;
import com.riprod.hexcode.core.hexcaster.utils.CasterInventory;
import com.riprod.hexcode.state.HexcodeManager;

import it.unimi.dsi.fastutil.floats.FloatArrayList;

public class DrawingSystem extends HexcodeManager {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Override
    public void firstTick(Ref<EntityStore> ref, HexcasterComponent comp,
            Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {
        comp.setDrawStartTimeMillis(System.currentTimeMillis());
    }

    @Override
    public void lastTick(Ref<EntityStore> ref, HexcasterComponent comp,
            Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {

        List<DrawnShapeComponent> drawnShapes = comp.getDrawnGlyphs();
        if (drawnShapes != null && !drawnShapes.isEmpty()) {
            GlyphCreationManager.NormalizeShapeSizes(drawnShapes);

            GlyphAsset matchedGlyph = GlyphCreationManager.MatchGlyph(drawnShapes);
            GlyphComponent glyphComponent = GlyphCreationManager.CreateGlyphComponent(matchedGlyph, 1.0f, 1l);

            if (glyphComponent != null) {
                HexBookComponent bookComponent = CasterInventory.getHexBookComponent(buffer, ref);
                bookComponent.addGlyph(glyphComponent);
                CasterInventory.saveHexBookComponent(buffer, ref, bookComponent);
            }
        }

        Ref<EntityStore> trailRef = comp.getTrailRef();
        if (trailRef != null && trailRef.isValid()) {
            buffer.removeEntity(trailRef, RemoveReason.REMOVE);
        }

        comp.clearDrawingState();
    }

    @Override
    public void tick0(Ref<EntityStore> ref, HexcasterComponent comp, float dt,
            Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {
        InterfaceManager.createIndicator(buffer, ref, comp);
    }

    @Override
    public void onPlayerJoin(Holder<EntityStore> holder, HexcasterComponent comp) {
    }

    @Override
    public void onPlayerLeave(PlayerRef playerRef) {
    }

    @Override
    public InteractionState onPrimaryEnter(Ref<EntityStore> ref, HexcasterComponent comp,
            ComponentAccessor<EntityStore> accessor) {

        HeadRotation head = accessor.getComponent(ref, HeadRotation.getComponentType());
        if (head == null)
            return InteractionState.Failed;

        TimeResource timeResource = accessor.getResource(TimeResource.getResourceType());
        Long curTime = timeResource.getNow().toEpochMilli();
        comp.setDrawStartTimeMillis(curTime);

        InterfaceManager.spawnTrails(accessor, ref, head);
        return InteractionState.NotFinished;
    }

    @Override
    public InteractionState onPrimaryTick(Ref<EntityStore> ref, HexcasterComponent comp,
            ComponentAccessor<EntityStore> accessor) {

        FloatArrayList points = comp.getDrawnStrokes();
        if (points == null)
            return InteractionState.Finished;

        HeadRotation head = accessor.getComponent(ref, HeadRotation.getComponentType());
        if (head == null)
            return InteractionState.Failed;

        float yaw = (float) Math.toDegrees(head.getRotation().getYaw());
        float pitch = (float) Math.toDegrees(head.getRotation().getPitch());

        if (!points.isEmpty()) {
            float lastYaw = points.getFloat(points.size() - 2);
            float lastPitch = points.getFloat(points.size() - 1);
            float dist = Math.abs(yaw - lastYaw) + Math.abs(pitch - lastPitch);
            if (dist < 0.5f)
                return InteractionState.NotFinished;
        }

        points.add(yaw);
        points.add(pitch);

        InterfaceManager.positionTrail(accessor, ref, head);
        return InteractionState.NotFinished;
    }

    @Override
    public InteractionState onPrimaryExit(Ref<EntityStore> ref, HexcasterComponent comp,
            ComponentAccessor<EntityStore> accessor) {

        FloatArrayList points = comp.getDrawnStrokes();
        if (points == null || points.size() < 3) {
            comp.clearStrokes();
            return InteractionState.Finished;
        }

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

        List<Vector3d> drawnGlyphs = InterfaceManager.getPositionsFromAngles(accessor, points, ref, 4.0f);
        Color color = InterfaceManager.getColorFromQuality(result.getAccuracy());
        result.setColor(color);
        result.setPoints(drawnGlyphs);

        long drawDuration = System.currentTimeMillis() - comp.getDrawStartTimeMillis();
        result.setSpeed(drawDuration);

        float maxSize = Math.max(Math.abs(maxYaw - minYaw), Math.abs(maxPitch - minPitch));
        result.setSize(maxSize);

        InterfaceManager.removeTrails(accessor, ref);
        comp.addDrawnGlyph(result);
        comp.clearStrokes();
        comp.setLastParticleSpawnMillis(0L);
        InterfaceManager.createIndicator(accessor, ref, comp);

        return InteractionState.Finished;
    }
}
