package com.riprod.hexcode.builtin.glyphs.effect.propel;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.BoundingBox;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.modules.entity.component.PropComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.modules.projectile.ProjectileModule;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.glyphs.component.Glyph;
import com.riprod.hexcode.core.execution.component.RootGlyph;
import com.riprod.hexcode.core.execution.Executor;
import com.riprod.hexcode.core.execution.component.HexContext;
import com.riprod.hexcode.core.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.glyphs.variables.HexVar;
import com.riprod.hexcode.utils.SpellVarUtil;

public class PropelGlyph implements GlyphHandler {
  private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
  public static final String ID = "Glyph_Propel";

  private static final double DEFAULT_SPEED = 30.0;
  private static final double MAX_DISTANCE = 64.0;
  private static final String PROJECTILE_MODEL = "Glyph_Propel";
  private static final float PROJECTILE_SCALE = 0.5f;

  @Override
  public void execute(Glyph glyph, HexContext hexContext) {
    int outputSlot = glyph.getOutputOrNumber(0, hexContext);

    HexVar sourceVar = glyph.getInput(0, hexContext);

    if (sourceVar == null) {
      LOGGER.atWarning().log("propel: no source provided");
      Executor.continueExecution(glyph.getNext(), hexContext);
      return;
    }

    Vector3d spawnPos = SpellVarUtil.resolveEyePosition(sourceVar, hexContext.getAccessor());
    if (spawnPos == null) {
      spawnPos = SpellVarUtil.resolveEyePosition(
          hexContext.getVariable(1), hexContext.getAccessor());
    }
    if (spawnPos == null) {
      LOGGER.atWarning().log("propel: could not resolve spawn position");
      return;
    }

    Vector3d direction = SpellVarUtil.resolveDirection(sourceVar, spawnPos, hexContext.getAccessor());
    if (direction == null) {
      direction = SpellVarUtil.resolveDirection(
          hexContext.getVariable(1), spawnPos, hexContext.getAccessor());
    }
    if (direction == null) {
      LOGGER.atWarning().log("propel: could not resolve direction");
      return;
    }

    double speed = SpellVarUtil.resolveNumberOrDefault(
        glyph.getInput(1, hexContext), DEFAULT_SPEED);
    if (speed <= 0)
      speed = DEFAULT_SPEED;

    Vector3f rotation = new Vector3f();
    rotation.setYaw((float) Math.atan2(-direction.x, direction.z));
    rotation.setPitch((float) Math.asin(-direction.y));

    ModelAsset modelAsset = ModelAsset.getAssetMap().getAsset(PROJECTILE_MODEL);
    Model model = Model.createScaledModel(modelAsset, PROJECTILE_SCALE);

    Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
    holder.addComponent(TransformComponent.getComponentType(),
        new TransformComponent(new Vector3d(spawnPos), rotation));
    holder.addComponent(HeadRotation.getComponentType(), new HeadRotation(rotation));
    holder.ensureComponent(UUIDComponent.getComponentType());
    holder.addComponent(ModelComponent.getComponentType(), new ModelComponent(model));
    holder.addComponent(PersistentModel.getComponentType(),
        new PersistentModel(model.toReference()));
    holder.addComponent(BoundingBox.getComponentType(),
        new BoundingBox(model.getBoundingBox()));
    holder.addComponent(NetworkId.getComponentType(),
        new NetworkId(hexContext.getAccessor().getExternalData().takeNextNetworkId()));
    holder.ensureComponent(PropComponent.getComponentType());
    holder.ensureComponent(ProjectileModule.get().getProjectileComponentType());
    holder.addComponent(Velocity.getComponentType(), new Velocity());

    Vector3d launchVelocity = new Vector3d(direction).scale(speed);
    PropelPhysicsConfig.INSTANCE.apply(holder, hexContext.getCasterRef(), launchVelocity,
        hexContext.getAccessor(), false);

    holder.addComponent(PropelComponent.getComponentType(),
        new PropelComponent(
            glyph,
            hexContext.getRoot().getRootEntityRef(),
            hexContext.copy(),
            outputSlot,
            hexContext.getCasterRef(),
            MAX_DISTANCE,
            new Vector3d(spawnPos)));

    Ref<EntityStore> projectileRef = hexContext.getAccessor().addEntity(holder, AddReason.SPAWN);

    RootGlyph execComp = hexContext.getAccessor().getComponent(
        hexContext.getRoot().getRootEntityRef(), RootGlyph.getComponentType());
    if (execComp != null) {
      execComp.incrementExternalWaiters();
    }

    PropelGlyphStyle.renderLaunch(spawnPos, direction, hexContext.getAccessor());
    LOGGER.atInfo().log("propel: launched projectile at speed %.1f", speed);
  }
}
