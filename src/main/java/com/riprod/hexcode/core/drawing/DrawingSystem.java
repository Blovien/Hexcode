package com.riprod.hexcode.core.drawing;

import java.util.ArrayList;
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
import com.hypixel.hytale.protocol.DebugShape;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.entity.component.BoundingBox;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.time.TimeResource;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.casting.utils.GlyphSpawner;
import com.riprod.hexcode.core.casting.utils.GlyphStyler;
import com.riprod.hexcode.core.crafting.component.HexcasterCraftingComponent;
import com.riprod.hexcode.core.crafting.component.PedestalAnchorComponent;
import com.riprod.hexcode.core.crafting.registry.PedestalBlockComponent;
import com.riprod.hexcode.core.crafting.spawners.PedestalSpawner;
import com.riprod.hexcode.core.debug.DebugComponent;
import com.riprod.hexcode.core.drawing.component.DrawnShapeComponent;
import com.riprod.hexcode.core.drawing.component.HexcasterDrawingComponent;
import com.riprod.hexcode.core.drawing.system.GlyphCreationManager;
import com.riprod.hexcode.core.drawing.system.InterfaceManager;
import com.riprod.hexcode.core.drawing.utils.DrawRasterizer;
import com.riprod.hexcode.core.drawing.utils.ShapeComparator;
import com.riprod.hexcode.core.glyphs.component.Glyph;
import com.riprod.hexcode.core.glyphs.component.GlyphComponent;
import com.riprod.hexcode.core.glyphs.registry.GlyphAsset;
import com.riprod.hexcode.core.hexcaster.component.HexcasterComponent;
import com.riprod.hexcode.core.hexes.component.Hex;
import com.riprod.hexcode.core.hexes.component.HexComponent;
import com.riprod.hexcode.core.hexes.utils.CreateHex;
import com.riprod.hexcode.state.HexcodeManager;

import it.unimi.dsi.fastutil.floats.FloatArrayList;

public class DrawingSystem extends HexcodeManager {
  private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
  private static final Box PREVIEW_BOUNDING_BOX = new Box(-0.25, -0.25, -0.25, 0.25, 0.25, 0.25);
  private static final float GLYPH_DISPLAY_DISTANCE = 1.0f;
  private static final float PEDESTAL_GLYPH_PITCH = (float) (-Math.PI / 2);

  @Override
  public void firstTick(Ref<EntityStore> ref, HexcasterComponent hexcaster,
      Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {
    // first tick stuffs
    HexcasterDrawingComponent drawingComponent = new HexcasterDrawingComponent();
    buffer.putComponent(ref, HexcasterDrawingComponent.getComponentType(), drawingComponent);
  }

  @Override
  public void lastTick(Ref<EntityStore> ref, HexcasterComponent comp,
      Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {

    List<DrawnShapeComponent> drawnShapes = comp.getDrawnGlyphs();
    if (drawnShapes != null && !drawnShapes.isEmpty()) {
      GlyphCreationManager.NormalizeShapeSizes(drawnShapes);

      GlyphAsset matchedGlyph = GlyphCreationManager.MatchGlyph(drawnShapes);

      if (matchedGlyph == null) {
        LOGGER.atInfo().log("no matching glyph found for drawn shape");
      } else {
        Float efficiency = ShapeComparator.calculateEfficiency(drawnShapes);
        Float volatility = ShapeComparator.calculateVolatility(drawnShapes);

        Glyph glyph = new Glyph(matchedGlyph, volatility, efficiency);
        Hex hex = new Hex(glyph);

        Ref<EntityStore> anchorRef = resolveAnchorRef(ref, buffer);
        HeadRotation hRotation = buffer.getComponent(ref, HeadRotation.getComponentType());
        PedestalBlockComponent pedestal = resolvePedestal(anchorRef, buffer);
        Vector3d spawnPos = calculateDrawCenter(drawnShapes);
        Transform transform = new Transform(spawnPos, hRotation.getRotation());

        if (pedestal == null || anchorRef == null || spawnPos == null) {
          LOGGER.atInfo().log("cannot spawn drawn hex: missing pedestal or draw position");
        } else {
          Vector3d anchorPos = PedestalSpawner.getAnchorPosition(pedestal.getLocation());
          double maxRadius = pedestal.getMaxRadius();
          double distSq = spawnPos.distanceSquaredTo(anchorPos);

          if (distSq > maxRadius * maxRadius) {
            LOGGER.atInfo().log("drawn hex outside pedestal radius");
          } else {
            spawnDrawnHex(buffer, hex, glyph, anchorRef, pedestal, transform);
          }
        }
      }
    }

    Ref<EntityStore> trailRef = comp.getTrailRef();
    if (trailRef != null && trailRef.isValid()) {
      buffer.removeEntity(trailRef, RemoveReason.REMOVE);
    }

    buffer.removeComponent(ref, HexcasterDrawingComponent.getComponentType());
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
  public InteractionState enterInteraction(Ref<EntityStore> ref, HexcasterComponent comp,
      CommandBuffer<EntityStore> accessor) {

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
  public InteractionState tickInteraction(Ref<EntityStore> ref, HexcasterComponent comp,
      CommandBuffer<EntityStore> accessor) {

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
      float dy = yaw - lastYaw;
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

  public InteractionState exitInteraction(Ref<EntityStore> ref, HexcasterComponent comp,
      CommandBuffer<EntityStore> accessor) {

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
    Color color = InterfaceManager.getColorFromQuality(result.getVolatility());
    result.setColor(color);
    result.setPoints(drawnGlyphs);

    // time calculations
    long drawSpeed = System.currentTimeMillis() - comp.getDrawStartTimeMillis();
    result.setSpeed(drawSpeed);

    float maxSize = Math.max(Math.abs(maxYaw - minYaw), Math.abs(maxPitch - minPitch));
    result.setSize(maxSize);

    LOGGER.atInfo().log("Drawn shape with speed: %d ms (%d score), size: %f, volatility: %f", drawSpeed,
        result.getEfficiency(), maxSize, result.getVolatility());

    InterfaceManager.removeTrails(accessor, ref);
    comp.addDrawnGlyph(result);
    comp.clearStrokes();
    comp.setLastParticleSpawnMillis(0L);
    InterfaceManager.createIndicator(accessor, ref, comp);

    return InteractionState.Finished;
  }

  private static Ref<EntityStore> resolveAnchorRef(Ref<EntityStore> playerRef,
      CommandBuffer<EntityStore> buffer) {
    HexcasterCraftingComponent craftingComp = buffer.getComponent(playerRef,
        HexcasterCraftingComponent.getComponentType());
    if (craftingComp == null) {
      return null;
    }
    return craftingComp.getPedestalRef();
  }

  private static PedestalBlockComponent resolvePedestal(Ref<EntityStore> anchorRef,
      CommandBuffer<EntityStore> buffer) {
    if (anchorRef == null || !anchorRef.isValid()) {
      return null;
    }
    PedestalAnchorComponent anchor = buffer.getComponent(anchorRef,
        PedestalAnchorComponent.getComponentType());
    if (anchor == null || anchor.getPedestalLoc() == null) {
      return null;
    }
    return BlockModule.getComponent(
        PedestalBlockComponent.getComponentType(),
        buffer.getExternalData().getWorld(),
        anchor.getPedestalLoc().getX(),
        anchor.getPedestalLoc().getY(),
        anchor.getPedestalLoc().getZ());
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

  private static void spawnDrawnHex(CommandBuffer<EntityStore> buffer, Hex hex, Glyph glyph,
      Ref<EntityStore> anchorRef, PedestalBlockComponent pedestal, Transform spawnPos) {

    HexComponent hexComponent = new HexComponent(hex);
    hexComponent.setRootRef(anchorRef);
    hexComponent.setParentRef(null);

    Holder<EntityStore> holder = CreateHex.createHexHolder(buffer, hexComponent, spawnPos.getPosition());
    holder.addComponent(BoundingBox.getComponentType(), new BoundingBox(PREVIEW_BOUNDING_BOX));
    holder.addComponent(DebugComponent.getComponentType(),
        new DebugComponent(DebugShape.Sphere, new Vector3f(0f, 1f, 0f), 0.15, 2.0f, 1));

    Ref<EntityStore> hexRef = CreateHex.createEntity(buffer, holder);
    hexComponent.setSelfRef(hexRef);

    int numGlyphs = hex.getGlyphs().size();
    float scaleMultiplier = 1 + (numGlyphs * GlyphStyler.SCALE_PER_GLYPH);

    String firstGlyphId = hex.getFirstGlyphId();
    Glyph firstGlyph = hex.get(firstGlyphId);
    GlyphComponent firstGlyphComponent = new GlyphComponent(firstGlyph);

    firstGlyphComponent.setHexRef(hexRef);
    firstGlyphComponent.setParentRef(hexRef);
    firstGlyphComponent.setOffset(Vector3f.ZERO);
    firstGlyphComponent.setRotation(spawnPos.getRotation());
    firstGlyphComponent.setScale(scaleMultiplier);
    hexComponent.setScale(scaleMultiplier);

    GlyphSpawner.spawnGlyphs(buffer, hexComponent, firstGlyphComponent, spawnPos.getPosition());

    List<Ref<EntityStore>> refs = new ArrayList<>(pedestal.getHexPreviewRefs());
    refs.add(hexRef);
    pedestal.setHexPreviewRefs(refs);
    LOGGER.atInfo().log("spawned drawn hex at %.1f", spawnPos);
  }
}
