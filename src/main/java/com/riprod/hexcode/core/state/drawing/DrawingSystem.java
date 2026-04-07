package com.riprod.hexcode.core.state.drawing;

import java.util.List;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.shape.Box;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.Color;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.time.TimeResource;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.api.event.GlyphDrawnEvent;
import com.riprod.hexcode.api.event.HexcodeEvents;
import com.riprod.hexcode.core.common.obelisk.system.ObeliskDispatcher;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphComponent;
import com.riprod.hexcode.core.common.glyphs.registry.GlyphAsset;
import com.riprod.hexcode.core.common.hexcaster.component.HexcasterComponent;
import com.riprod.hexcode.core.common.hexes.component.HexComponent;
import com.riprod.hexcode.core.common.pedestal.utils.PedestalBlockUtil;
import com.riprod.hexcode.core.common.pedestal.component.PedestalBlockComponent;
import com.riprod.hexcode.core.state.crafting.component.NodeComponent;
import com.riprod.hexcode.core.state.crafting.component.CraftingData;
import com.riprod.hexcode.core.state.crafting.entity.PedestalEntity;
import com.riprod.hexcode.core.state.crafting.handlers.node.Glyph.GlyphNodeHandler;
import com.riprod.hexcode.core.state.crafting.utils.CraftingDataUtil;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.riprod.hexcode.core.state.drawing.component.DrawnShapeComponent;
import com.riprod.hexcode.core.state.drawing.component.HexcasterDrawingComponent;
import com.riprod.hexcode.core.state.drawing.registry.ShapeTemplateStore;
import com.riprod.hexcode.core.state.drawing.system.GlyphCreationManager;
import com.riprod.hexcode.core.state.drawing.system.InterfaceManager;
import com.riprod.hexcode.core.state.drawing.system.shapes.DollarOneFixedDetector;
import com.riprod.hexcode.core.state.drawing.system.shapes.ShapeDetector;
import com.riprod.hexcode.core.state.drawing.utils.ShapeComparator;
import com.riprod.hexcode.state.HexState;
import com.riprod.hexcode.state.HexcodeManager;

import it.unimi.dsi.fastutil.floats.FloatArrayList;

public class DrawingSystem extends HexcodeManager {
  private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
  private static final Box PREVIEW_BOUNDING_BOX = new Box(-0.25, -0.25, -0.25, 0.25, 0.25, 0.25);
  private static final float GLYPH_DISPLAY_DISTANCE = 1.0f;
  private static final float PEDESTAL_GLYPH_PITCH = (float) (-Math.PI / 2);

  private static ShapeDetector shapeDetector = new DollarOneFixedDetector();

  public static ShapeDetector getShapeDetector() {
    return shapeDetector;
  }

  public static void setShapeDetector(ShapeDetector detector) {
    shapeDetector = detector;
  }

  @Override
  public void firstTick(Ref<EntityStore> ref, HexcasterComponent hexcaster,
      Store<EntityStore> store, CommandBuffer<EntityStore> buffer,
      HexState previousState) {
    // first tick stuffs
    HexcasterDrawingComponent drawingComponent = new HexcasterDrawingComponent();
    buffer.putComponent(ref, HexcasterDrawingComponent.getComponentType(), drawingComponent);
  }

  @Override
  public void lastTick(Ref<EntityStore> ref, HexcasterComponent comp,
      Store<EntityStore> store, CommandBuffer<EntityStore> buffer,
      HexState nextState) {

    HexcasterDrawingComponent drawingComp = buffer.getComponent(ref, HexcasterDrawingComponent.getComponentType());
    PedestalBlockComponent pedestal = PedestalBlockUtil.resolvePedestal(ref, buffer);
    if (pedestal == null) {
        return;
    }
    CraftingData playerData = pedestal.getCraftingDataComponent();

    List<DrawnShapeComponent> drawnShapes = drawingComp.getDrawnGlyphs();
    if (drawnShapes != null && !drawnShapes.isEmpty()) {
      GlyphCreationManager.NormalizeShapeSizes(drawnShapes);

      GlyphAsset matchedGlyph = GlyphCreationManager.MatchGlyph(drawnShapes);

      if (matchedGlyph == null) {
        LOGGER.atInfo().log("no matching glyph found for drawn shape");
      } else {
        float efficiency = ShapeComparator.calculateEfficiency(drawnShapes);
        float volatility = ShapeComparator.calculateVolatility(drawnShapes);
        LOGGER.atInfo().log("Efficiency: %f | Volatility: %f", efficiency, volatility);

        Glyph glyph = new Glyph(matchedGlyph, volatility, efficiency);

        if (pedestal != null) {
          ObeliskDispatcher.dispatchGlyphDrawn(buffer, pedestal, ref, glyph);
        }
        HexcodeEvents.fire(new GlyphDrawnEvent(ref, glyph, drawnShapes, matchedGlyph));

        HeadRotation hRotation = buffer.getComponent(ref, HeadRotation.getComponentType());
        Vector3d spawnPos = calculateDrawCenter(drawnShapes);
        Transform transform = new Transform(spawnPos, hRotation.getRotation());

        if (spawnPos == null) {
          LOGGER.atInfo().log("cannot spawn drawn hex: missing pedestal or draw position");
        } else {
          Vector3d anchorPos = PedestalEntity.getAnchorPosition(playerData.getPedestalLocation());
          double maxRadius = pedestal.getMaxRadius();
          double distSq = spawnPos.distanceSquaredTo(anchorPos);

          if (distSq > maxRadius * maxRadius) {
            LOGGER.atInfo().log("drawn hex outside pedestal radius");
          } else {
            spawnDrawnGlyph(buffer, glyph, playerData, transform, ref);
          }
        }
      }
    }

    Ref<EntityStore> trailRef = drawingComp.getTrailRef();
    if (trailRef != null && trailRef.isValid()) {
      buffer.removeEntity(trailRef, RemoveReason.REMOVE);
    }

    drawingComp.clearDrawingState();
    drawingComp.clear(buffer);
  }

  @Override
  public void tick0(Ref<EntityStore> ref, HexcasterComponent comp, float dt,
      Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {

    HexcasterDrawingComponent drawingComp = buffer.getComponent(ref, HexcasterDrawingComponent.getComponentType());
    if (drawingComp == null) {
      return;
    }
    InterfaceManager.createIndicator(buffer, ref, drawingComp);
  }

  @Override
  public void onPlayerJoin(Holder<EntityStore> holder, HexcasterComponent comp) {
  }

  @Override
  public void onPlayerLeave(PlayerRef playerRef) {
  }

  @Override
  public InteractionState enterInteraction(CommandBuffer<EntityStore> accessor, Ref<EntityStore> ref,
      HexcasterComponent comp) {

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
  public InteractionState tickInteraction(CommandBuffer<EntityStore> accessor, Ref<EntityStore> ref, float dt,
      HexcasterComponent comp) {

    HexcasterDrawingComponent drawingComp = accessor.getComponent(ref, HexcasterDrawingComponent.getComponentType());
    if (drawingComp == null) {
      return InteractionState.Failed;
    }
    FloatArrayList points = drawingComp.getDrawnStrokes();
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
      // unwrap yaw so it stays continuous across the ±180 boundary
      float dy = yaw - lastYaw;
      if (dy > 180f)
        dy -= 360f;
      else if (dy < -180f)
        dy += 360f;
      yaw = lastYaw + dy;

      float dp = pitch - lastPitch;
      float dist = dy * dy + dp * dp;
      if (dist < (0.5f * 0.5f)) {
        return InteractionState.NotFinished;
      }
    }

    points.add(yaw);
    points.add(pitch);

    InterfaceManager.positionTrail(accessor, ref, head);
    return InteractionState.NotFinished;
  }

  @Override
  public InteractionState exitInteraction(CommandBuffer<EntityStore> accessor, Ref<EntityStore> ref,
      HexcasterComponent comp) {

    HexcasterDrawingComponent drawingComp = accessor.getComponent(ref, HexcasterDrawingComponent.getComponentType());
    if (drawingComp == null) {
      return InteractionState.Failed;
    }

    FloatArrayList points = drawingComp.getDrawnStrokes();
    if (points == null || points.size() < 3) {
      drawingComp.clearStrokes();
      return InteractionState.Finished;
    }

    String trainingId = comp.consumeTrainingShapeId();
    if (trainingId != null) {
      ShapeTemplateStore.saveTemplate(trainingId, points);
      shapeDetector.clearCache();
      LOGGER.atInfo().log("recorded training template for '%s' (%d points)", trainingId, points.size() / 2);
      InterfaceManager.removeTrails(accessor, ref);
      drawingComp.clearStrokes();
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

    DrawnShapeComponent result = shapeDetector.detect(points, minYaw, maxYaw, minPitch, maxPitch);

    List<Vector3d> drawnGlyphs = InterfaceManager.getPositionsFromAngles(accessor, points, ref, 4.0f);
    Color color = InterfaceManager.getColorFromQuality(result.getVolatility());
    result.setColor(color);
    result.setPoints(drawnGlyphs);

    TimeResource timeResource = accessor.getResource(TimeResource.getResourceType());
    Long curTime = timeResource.getNow().toEpochMilli();
    // time calculations
    long startTime = drawingComp.getDrawStartTimeMillis();
    long drawSpeed = curTime - startTime;
    result.setSpeed(drawSpeed);

    float maxSize = Math.max(Math.abs(maxYaw - minYaw), Math.abs(maxPitch - minPitch));
    result.setSize(maxSize);

    LOGGER.atInfo().log("%d ms (%f score) | S: %f | A: %f", drawSpeed, result.getEfficiency(), maxSize,
        result.getVolatility());

    InterfaceManager.removeTrails(accessor, ref);
    drawingComp.addDrawnGlyph(result);
    drawingComp.clearStrokes();
    drawingComp.setLastParticleSpawnMillis(0L);
    InterfaceManager.createIndicator(accessor, ref, drawingComp);

    return InteractionState.Finished;
  }

  private static Vector3d calculateDrawCenter(List<DrawnShapeComponent> drawnShapes) {
    double x = 0, y = 0, z = 0;
    int count = 0;
    for (DrawnShapeComponent shape : drawnShapes) {
      List<Vector3d> points = shape.getPoints();
      if (points == null) {
        continue;
      }
      for (Vector3d p : points) {
        x += p.x;
        y += p.y;
        z += p.z;
        count++;
      }
    }
    if (count == 0) {
      return null;
    }
    return new Vector3d(x / count, y / count, z / count);
  }

  private static void spawnDrawnGlyph(CommandBuffer<EntityStore> accessor, Glyph glyph,
      CraftingData playerData, Transform spawnPos, Ref<EntityStore> playerRef) {

    Ref<EntityStore> anchorRef = playerData.getAnchorNodeRef();
    if (anchorRef == null || !anchorRef.isValid()) {
      LOGGER.atWarning().log("cannot spawn drawn glyph: no active anchor entity ref on pedestal");
      return;
    }
    NodeComponent nodeComp = accessor.getComponent(anchorRef, NodeComponent.getComponentType());
    if (nodeComp == null) {
      LOGGER.atWarning().log("cannot spawn drawn glyph: no node component on anchor entity");
      return;
    }
    Ref<EntityStore> hexRef = nodeComp.getParentEntity();
    if (hexRef == null || !hexRef.isValid()) {
      LOGGER.atWarning().log("cannot spawn drawn glyph: no hex entity associated with anchor");
      return;
    }

    HexComponent hexComp = accessor.getComponent(hexRef, HexComponent.getComponentType());

    if (hexComp == null) {
      LOGGER.atWarning().log("cannot spawn drawn glyph: no hex component on hex entity");
      return;
    }

    GlyphComponent glyphComponent = new GlyphComponent(glyph);

    Vector3f rotation = spawnPos.getRotation();
    glyphComponent.setRotation(rotation);
    glyph.setRotation(rotation);

    Vector3d worldPos = spawnPos.getPosition();

    TransformComponent hexTransform = accessor.getComponent(hexRef, TransformComponent.getComponentType());
    if (hexTransform != null) {
      Vector3d hexPos = hexTransform.getPosition();
      glyph.setPosition(new Vector3f(
          (float) (worldPos.x - hexPos.x),
          (float) (worldPos.y - hexPos.y),
          (float) (worldPos.z - hexPos.z)));
    }

    Ref<EntityStore> effectRef = GlyphNodeHandler.INSTANCE.spawnNode(accessor, hexRef, worldPos, playerRef,
        glyphComponent, hexRef);

    hexComp.addChildGlyphRef(glyph.getId(), effectRef);
    hexComp.getHex().put(glyph.getId(), glyph);
  }
}
