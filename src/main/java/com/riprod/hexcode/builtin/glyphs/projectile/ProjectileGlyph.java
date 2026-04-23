package com.riprod.hexcode.builtin.glyphs.projectile;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.modules.entity.component.BoundingBox;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.modules.entity.component.PropComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.modules.projectile.ProjectileModule;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.builtin.glyphs.projectile.component.ProjectileComponent;
import com.riprod.hexcode.builtin.glyphs.projectile.style.ProjectileStyle;
import com.riprod.hexcode.core.common.construct.system.HexConstructSpawner;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.state.execution.HexExecuter;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.core.state.execution.component.RootGlyph;
import com.riprod.hexcode.utils.SpellVarUtil;

public class ProjectileGlyph implements GlyphHandler {
  private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
  @Override
public String getId() { return ID; };

public static final String ID = "Projectile";
  public static final String HANDLER_ID = "projectile";

  private static final double MAX_DISTANCE = 64.0;
  private static final String PROJECTILE_MODEL = "Projectile_Flight";
  private static final float PROJECTILE_SCALE = 0.5f;

  @Override
  public void execute(Glyph glyph, HexContext hexContext) {
    HexVar sourceVar = glyph.readSlot(ProjectileGlyphSlots.SOURCE, hexContext);
    HexVar directionVar = glyph.readSlot(ProjectileGlyphSlots.DIRECTION, hexContext);
    HexVar speedVar = glyph.readSlot(ProjectileGlyphSlots.SPEED, hexContext);
    HexVar gravityVar = glyph.readSlot(ProjectileGlyphSlots.GRAVITY, hexContext);
    HexVar bouncesVar = glyph.readSlot(ProjectileGlyphSlots.BOUNCES, hexContext);

    if (sourceVar == null) {
      LOGGER.atWarning().log("propel: no source provided");
      HexExecuter.continueFromSlot(glyph, Glyph.NEXT_SLOT, hexContext);
      return;
    }

    Vector3d spawnPos = SpellVarUtil.resolveEyePosition(sourceVar, hexContext.getAccessor());
    if (spawnPos == null) {
      spawnPos = SpellVarUtil.resolveAsPosition(sourceVar, hexContext.getAccessor());
    }
    if (spawnPos == null) {
      LOGGER.atWarning().log("propel: could not resolve spawn position");
      HexExecuter.fail(hexContext);
      return;
    }

    Vector3d direction = SpellVarUtil.resolveDirection(directionVar, spawnPos, hexContext.getAccessor());
    if (direction == null) {
      direction = SpellVarUtil.resolveDirection(directionVar, spawnPos, hexContext.getAccessor());
    }
    if (direction == null) {
      LOGGER.atWarning().log("propel: could not resolve direction");
      HexExecuter.fail(hexContext);
      return;
    }

    spawnPos.add(new Vector3d(direction).scale(1.5));

    double speed = SpellVarUtil.resolveNumberOrDefault(speedVar, 30.0);
    if (speed <= 0) speed = 30.0;

    double gravity = SpellVarUtil.resolveNumberOrDefault(gravityVar, 0.0);

    int bounces = SpellVarUtil.resolveNumberOrDefault(bouncesVar, 0.0).intValue();
    if (bounces < 0) bounces = 0;

    Vector3f rotation = new Vector3f();
    rotation.setYaw((float) Math.atan2(-direction.x, direction.z));
    rotation.setPitch((float) Math.asin(-direction.y));

    Holder<EntityStore> holder = HexConstructSpawner.create(
        hexContext.getAccessor(), hexContext, glyph, HANDLER_ID,
        -1, 0,
        null, glyph.getNextLinks(), null,
        new Vector3d(spawnPos));

    TransformComponent transform = holder.getComponent(TransformComponent.getComponentType());
    if (transform != null) {
      transform.getRotation().assign(rotation);
    }

    holder.addComponent(HeadRotation.getComponentType(), new HeadRotation(rotation));

    ModelAsset modelAsset = ModelAsset.getAssetMap().getAsset(PROJECTILE_MODEL);
    Model model = Model.createScaledModel(modelAsset, PROJECTILE_SCALE);
    holder.addComponent(ModelComponent.getComponentType(), new ModelComponent(model));
    holder.addComponent(PersistentModel.getComponentType(),
        new PersistentModel(model.toReference()));
    holder.addComponent(BoundingBox.getComponentType(),
        new BoundingBox(model.getBoundingBox()));

    holder.ensureComponent(PropComponent.getComponentType());
    holder.ensureComponent(ProjectileModule.get().getProjectileComponentType());
    holder.addComponent(Velocity.getComponentType(), new Velocity());

    Vector3d launchVelocity = new Vector3d(direction).scale(speed);
    new ProjectilePhysicsConfig(gravity, bounces).apply(holder, hexContext.getCasterRef(),
        launchVelocity, hexContext.getAccessor(), false);

    holder.addComponent(ProjectileComponent.getComponentType(),
        new ProjectileComponent(hexContext.getCasterRef(), MAX_DISTANCE, new Vector3d(spawnPos)));

    Ref<EntityStore> projectileRef = hexContext.getAccessor().addEntity(holder, AddReason.SPAWN);

    RootGlyph execComp = hexContext.getAccessor().getComponent(
        hexContext.getRoot().getRootEntityRef(), RootGlyph.getComponentType());
    if (execComp != null) {
      execComp.addDependent(projectileRef);
    }

    ProjectileStyle.renderLaunch(spawnPos, direction, hexContext.getColors(), hexContext.getAccessor());
    LOGGER.atInfo().log("propel: launched projectile at speed %.1f", speed);
  }
}
