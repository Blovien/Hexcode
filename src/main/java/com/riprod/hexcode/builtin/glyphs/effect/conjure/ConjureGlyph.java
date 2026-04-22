package com.riprod.hexcode.builtin.glyphs.effect.conjure;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.shape.Box;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.DebugShape;
import com.hypixel.hytale.server.core.modules.debug.DebugUtils;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.BoundingBox;
import com.hypixel.hytale.server.core.modules.entity.component.PropComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.modules.entity.hitboxcollision.HitboxCollision;
import com.hypixel.hytale.server.core.modules.entity.hitboxcollision.HitboxCollisionConfig;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.modules.projectile.ProjectileModule;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.builtin.glyphs.effect.conjure.component.ConjureZoneComponent;
import com.riprod.hexcode.builtin.glyphs.effect.conjure.style.ConjureStyle;
import com.riprod.hexcode.core.common.construct.HexConstructSpawner;
import com.riprod.hexcode.core.common.construct.component.HexConstruct;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.common.glyphs.variables.NumberVar;
import com.riprod.hexcode.core.common.glyphs.variables.PositionVar;
import com.riprod.hexcode.core.common.utilities.component.DebugComponent;
import com.riprod.hexcode.core.state.execution.Executor;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.core.state.execution.component.RootGlyph;
import com.riprod.hexcode.utils.SpellVarUtil;

public class ConjureGlyph implements GlyphHandler {
  private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
  public static final String ID = "Glyph_Conjure";
  private static final String HARD_COLLISION_ID = "Hexcode_Conjure_HardCollision";

  @Override
  public void execute(Glyph glyph, HexContext hexContext) {
    HexVar coordsAVar = glyph.readSlot(ConjureGlyphSlots.COORDS_A, hexContext,
        new PositionVar(new Vector3d(0.5, 0.5, 0.5)));
    HexVar coordsBVar = glyph.readSlot(ConjureGlyphSlots.COORDS_B, hexContext,
        new PositionVar(new Vector3d(-0.5, -0.5, -0.5)));
    HexVar durationVar = glyph.readSlot(ConjureGlyphSlots.DURATION, hexContext);
    HexVar intervalVar = glyph.readSlot(ConjureGlyphSlots.INTERVAL, hexContext);
    HexVar anchorVar = glyph.readSlot(ConjureGlyphSlots.ANCHOR, hexContext);

    if (anchorVar == null) {
      LOGGER.atInfo().log("conjure: no anchor, failing");
      Executor.fail(hexContext);
      return;
    }
    if (anchorVar instanceof NumberVar anchorNum) {
      HexVar resolvedVar = hexContext.getVariable(anchorNum.getValue().toString());
      if (resolvedVar == null) {
        LOGGER.atInfo().log("conjure: anchor number %s does not resolve to a variable, failing",
            anchorNum.getValue());
        Executor.fail(hexContext);
        return;
      }
      anchorVar = resolvedVar;
    }
    Vector3d anchorPos = SpellVarUtil.resolvePosition(anchorVar, hexContext.getAccessor());
    if (anchorPos == null) {
      LOGGER.atInfo().log("conjure: invalid anchor position, failing");
      Executor.fail(hexContext);
      return;
    }

    Vector3d coordsA = SpellVarUtil.resolvePosition(coordsAVar, hexContext.getAccessor());
    Vector3d coordsB = SpellVarUtil.resolvePosition(coordsBVar, hexContext.getAccessor());

    if (coordsA == null || coordsB == null) {
      LOGGER.atInfo().log("conjure: invalid coords, failing");
      Executor.fail(hexContext);
      return;
    }

    boolean absA = (coordsAVar instanceof PositionVar pa && pa.isAbsolute())
        || !(coordsAVar instanceof PositionVar);
    boolean absB = (coordsBVar instanceof PositionVar pb && pb.isAbsolute())
        || !(coordsBVar instanceof PositionVar);
    Vector3d cornerA = absA ? coordsA : new Vector3d(anchorPos).add(coordsA);
    Vector3d cornerB = absB ? coordsB : new Vector3d(anchorPos).add(coordsB);

    Vector3d min = new Vector3d(
        Math.min(cornerA.x, cornerB.x),
        Math.min(cornerA.y, cornerB.y),
        Math.min(cornerA.z, cornerB.z));
    Vector3d max = new Vector3d(
        Math.max(cornerA.x, cornerB.x),
        Math.max(cornerA.y, cornerB.y),
        Math.max(cornerA.z, cornerB.z));
    Vector3d center = new Vector3d(
        (min.x + max.x) / 2,
        (min.y + max.y) / 2,
        (min.z + max.z) / 2);
    Vector3d halfExtents = new Vector3d(
        (max.x - min.x) / 2,
        (max.y - min.y) / 2,
        (max.z - min.z) / 2);
    Vector3d size = new Vector3d(max.x - min.x, max.y - min.y, max.z - min.z);

    float durationSeconds = SpellVarUtil.resolveNumberOrDefault(durationVar, 5.0).floatValue();
    float interval = SpellVarUtil.resolveNumberOrDefault(intervalVar, -1.0).floatValue();

    String[] immediate = glyph.getSlot(ConjureGlyphSlots.IMMEDIATE).getLinks();
    String[] next = glyph.getSlot(Glyph.NEXT_SLOT).getLinks();

    List<String> immediateBranchIds = Arrays.asList(immediate);
    List<String> conditionalBranchIds = Arrays.asList(next);

    ConjureZoneComponent zoneComp = new ConjureZoneComponent(halfExtents, interval);

    Vector3f debugColor = ConjureStyle.resolveColor(hexContext.getColors());

    HitboxCollisionConfig collisionConfig = HitboxCollisionConfig.getAssetMap()
        .getAsset(HARD_COLLISION_ID);

    Holder<EntityStore> holder = HexConstructSpawner.create(
        hexContext.getAccessor(), hexContext, glyph,
        "conjure", durationSeconds, 0f,
        immediateBranchIds, conditionalBranchIds, null,
        new Vector3d(center));

    holder.ensureComponent(PropComponent.getComponentType());
    holder.ensureComponent(ProjectileModule.get().getProjectileComponentType());
    holder.ensureComponent(EffectControllerComponent.getComponentType());
    DebugComponent debugComp = new DebugComponent(DebugShape.Cube, debugColor, size, 0.1f);
    debugComp.setOpacity(0.3f);
    debugComp.setIntervalMultiplier(0.01f);
    debugComp.setFlags(DebugUtils.FLAG_NO_WIREFRAME);
    holder.addComponent(DebugComponent.getComponentType(), debugComp);
    holder.addComponent(BoundingBox.getComponentType(),
        new BoundingBox(Box.horizontallyCentered(halfExtents.x * 2, halfExtents.y * 2,
            halfExtents.z * 2)));
    holder.addComponent(Velocity.getComponentType(), new Velocity());

    if (collisionConfig != null) {
      holder.addComponent(HitboxCollision.getComponentType(),
          new HitboxCollision(collisionConfig));
    }

    ConjurePhysicsConfig.INSTANCE.apply(holder, hexContext.getCasterRef(),
        new Vector3d(0, 0, 0), hexContext.getAccessor(), false);

    holder.addComponent(ConjureZoneComponent.getComponentType(), zoneComp);

    ModelAsset anchorAsset = ModelAsset.getAssetMap().getAsset("Conjured_Anchor");
    if (anchorAsset != null) {
      Model anchorModel = Model.createUnitScaleModel(anchorAsset);
      holder.addComponent(ModelComponent.getComponentType(), new ModelComponent(anchorModel));
      holder.addComponent(PersistentModel.getComponentType(),
          new PersistentModel(anchorModel.toReference()));
    }

    Ref<EntityStore> zoneRef = hexContext.getAccessor().addEntity(holder, AddReason.SPAWN);
    zoneComp.setZoneRef(zoneRef);

    ConjureStyle.renderSpawn(center, hexContext.getColors(), hexContext.getAccessor());

    UUIDComponent zoneUuidComp = holder.getComponent(UUIDComponent.getComponentType());
    UUID zoneUuid = zoneUuidComp != null ? zoneUuidComp.getUuid() : UUID.randomUUID();
    EntityVar zoneEntityVar = new EntityVar(zoneUuid, zoneRef);
    glyph.writeSelfOutput(zoneEntityVar, hexContext);

    RootGlyph execComp = hexContext.getAccessor().getComponent(
        hexContext.getRoot().getRootEntityRef(), RootGlyph.getComponentType());
    if (execComp != null) {
      execComp.addDependent(zoneRef);
    }

  }
}
