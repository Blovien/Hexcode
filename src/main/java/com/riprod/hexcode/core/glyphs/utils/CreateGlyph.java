package com.riprod.hexcode.core.glyphs.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.hypixel.hytale.builtin.mounts.MountedComponent;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.MountController;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelParticle;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.BoundingBox;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.glyphs.component.GlyphComponent;
import com.riprod.hexcode.core.glyphs.registry.GlyphAsset;
import com.riprod.hexcode.utils.GlyphMath;

public class CreateGlyph {
  private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

  public static Ref<EntityStore> createCastingRoot(ComponentAccessor<EntityStore> accessor,
      Ref<EntityStore> playerRef, float eyeHeight, ModelParticle[] particles) {

    Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
    Vector3d playerPos = accessor.getComponent(playerRef, TransformComponent.getComponentType())
        .getPosition();
    holder.addComponent(TransformComponent.getComponentType(),
        new TransformComponent(playerPos, new Vector3f(0, 0, 0)));
    holder.addComponent(MountedComponent.getComponentType(),
        new MountedComponent(playerRef, new Vector3f(0, eyeHeight, 0),
            MountController.Minecart));

    holder.addComponent(UUIDComponent.getComponentType(),
        new UUIDComponent(UUID.randomUUID()));
    holder.ensureComponent(EntityStore.REGISTRY.getNonSerializedComponentType());

    ModelAsset modelAsset = ModelAsset.getAssetMap().getAsset("Casting_Anchor");
    Model model = new Model(
        modelAsset.getId(), 1.0f, (Map<String, String>) null, modelAsset.getAttachments(null),
        modelAsset.getBoundingBox(), modelAsset.getModel(), modelAsset.getTexture(),
        modelAsset.getGradientSet(), modelAsset.getGradientId(), modelAsset.getEyeHeight(),
        modelAsset.getCrouchOffset(), modelAsset.getSittingOffset(), modelAsset.getSleepingOffset(),
        modelAsset.getAnimationSetMap(), modelAsset.getCamera(),
        modelAsset.getLight(), particles, modelAsset.getTrails(), modelAsset.getPhysicsValues(),
        modelAsset.getDetailBoxes(), modelAsset.getPhobia(), modelAsset.getPhobiaModelAssetId()
    );

    holder.addComponent(ModelComponent.getComponentType(),
        new ModelComponent(model));

    holder.addComponent(PersistentModel.getComponentType(),
        new PersistentModel(model.toReference()));

    int networkId = accessor.getExternalData().takeNextNetworkId();
    holder.addComponent(NetworkId.getComponentType(), new NetworkId(networkId));

    Ref<EntityStore> ref = accessor.addEntity(holder, AddReason.SPAWN);

    return ref;
  }

  public static Ref<EntityStore> createHeadAnchor(ComponentAccessor<EntityStore> accessor,
      Ref<EntityStore> playerRef, float eyeHeight) {
    Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
    Vector3d playerPos = accessor.getComponent(playerRef, TransformComponent.getComponentType())
        .getPosition();

    holder.addComponent(TransformComponent.getComponentType(),
        new TransformComponent(playerPos, new Vector3f(0, 0, 0)));
    holder.addComponent(UUIDComponent.getComponentType(),
        new UUIDComponent(UUID.randomUUID()));

    ModelAsset modelAsset = ModelAsset.getAssetMap().getAsset("Head_Anchor");
    Model model = Model.createUnitScaleModel(modelAsset);
    holder.addComponent(ModelComponent.getComponentType(), new ModelComponent(model));
    holder.addComponent(PersistentModel.getComponentType(), new PersistentModel(model.toReference()));
    holder.ensureComponent(EntityStore.REGISTRY.getNonSerializedComponentType());

    int networkId = accessor.getExternalData().takeNextNetworkId();
    holder.addComponent(NetworkId.getComponentType(), new NetworkId(networkId));

    holder.addComponent(MountedComponent.getComponentType(),
        new MountedComponent(playerRef, new Vector3f(0, eyeHeight, 0),
            MountController.Minecart));

    return accessor.addEntity(holder, AddReason.SPAWN);
  }

  /** @deprecated - use overflow instead. This one is for regression only */
  public static Ref<EntityStore> createCastingRoot(ComponentAccessor<EntityStore> accessor,
      Vector3d playerPos) {
    Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
    Vector3d eyePos = new Vector3d(playerPos.x, playerPos.y + 1.8, playerPos.z);
    holder.addComponent(TransformComponent.getComponentType(),
        new TransformComponent(eyePos, new Vector3f(0, 0, 0)));
    holder.addComponent(UUIDComponent.getComponentType(),
        new UUIDComponent(UUID.randomUUID()));

    // add the Casting Anchor model to the root entitiy for particles and side
    // effects to be tied to
    ModelAsset modelAsset = ModelAsset.getAssetMap().getAsset("Casting_Anchor");
    Model model = Model.createUnitScaleModel(modelAsset);

    holder.addComponent(ModelComponent.getComponentType(),
        new ModelComponent(model));

    holder.addComponent(PersistentModel.getComponentType(),
        new PersistentModel(model.toReference()));

    int networkId = accessor.getExternalData().takeNextNetworkId();
    holder.addComponent(NetworkId.getComponentType(), new NetworkId(networkId));

    Ref<EntityStore> ref = accessor.addEntity(holder, AddReason.SPAWN);

    return ref;
  }

  public static Holder<EntityStore> createGlyphHolder(GlyphComponent glyph, Vector3d parentPos) {
    Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();

    holder.addComponent(GlyphComponent.getComponentType(), glyph);

    // Required components
    TransformComponent glyphTransform = new TransformComponent(parentPos,
        new Vector3f(glyph.getPitch(), glyph.getYaw(), 0));

    holder.addComponent(TransformComponent.getComponentType(),
        glyphTransform);

    // asset logic
    GlyphAsset asset = GlyphAsset.getAssetMap().getAsset(glyph.getGlyphId());

    if (asset == null) {
      throw new IllegalArgumentException("Unknown glyph ID: " + glyph.getGlyphId());
    }

    ModelAsset modelAsset = ModelAsset.getAssetMap().getAsset(asset.getModelPath());
    if (modelAsset == null) {
      throw new IllegalArgumentException("Unknown model asset: " + asset.getModelPath());
    }

    Model model = Model.createScaledModel(modelAsset, glyph.getScale());

    holder.addComponent(ModelComponent.getComponentType(), new ModelComponent(model));

    // rest
    holder.ensureComponent(EntityStore.REGISTRY.getNonSerializedComponentType());
    holder.addComponent(UUIDComponent.getComponentType(),
        new UUIDComponent(UUID.randomUUID()));
    holder.addComponent(BoundingBox.getComponentType(), new BoundingBox(model.getBoundingBox()));
    holder.addComponent(PersistentModel.getComponentType(), new PersistentModel(model.toReference()));

    Ref<EntityStore> hexRoot = glyph.getRootRef();

    if (hexRoot != null) {
      MountedComponent mounted = new MountedComponent(hexRoot, glyph.getOffset(),
          MountController.Minecart);
      holder.addComponent(MountedComponent.getComponentType(), mounted);
    }

    return holder;
  }

  /**
   * creates the glyph entity and adds a transform component to the base glyph for
   * dragging
   * 
   * @throws
   */
  public static List<GlyphComponent> createGlyphEntity(ComponentAccessor<EntityStore> accessor,
      GlyphComponent glyph, Vector3d parentPos) {
    Holder<EntityStore> holder = createGlyphHolder(glyph, parentPos);

    int networkId = accessor.getExternalData().takeNextNetworkId();
    holder.addComponent(NetworkId.getComponentType(), new NetworkId(networkId));

    return createGlyphEntity(accessor, holder, parentPos);
  }

  /**
   * @throws
   * @param accessor
   * @param holder
   * @return
   */
  public static List<GlyphComponent> createGlyphEntity(ComponentAccessor<EntityStore> buffer,
      Holder<EntityStore> holder, Vector3d parentPos) {

    List<GlyphComponent> glyphs = new ArrayList<>();
    Ref<EntityStore> ref = createEntity(buffer, holder);

    // add any children glyphs
    GlyphComponent glyphComp = holder.getComponent(GlyphComponent.getComponentType());
    List<GlyphComponent> children = glyphComp.getChildren();

    glyphComp.setSelfRef(ref); // backwards reference
    glyphs.add(glyphComp);

    if (children == null) {
      return glyphs;
    }

    GlyphMath.distributeChildAngles(children, glyphComp.getScale());
    for (int i = 0; i < children.size(); i++) {
      GlyphComponent childGlyph = children.get(i);
      childGlyph.setParentRef(ref);
      childGlyph.setScale(glyphComp.getScale() * 0.75f); // TODO: finalize child glyph scale
      childGlyph.setOffset(childGlyph.getPitch(), childGlyph.getYaw(), 0);
      float d = (float) glyphComp.getDistance();
      childGlyph.setOffset(
          -d * (float) Math.sin(childGlyph.getYaw()),
          d * (float) Math.sin(childGlyph.getPitch()),
          0f);
      childGlyph.setRootRef(ref);

      try {
        List<GlyphComponent> childGlyphs = createGlyphEntity(buffer, childGlyph, parentPos);
        glyphs.addAll(childGlyphs);
      } catch (Exception e) {
        // just log
        LOGGER.atSevere().withCause(e)
            .log("Failed to create child glyph entity for glyph ID: "
                + childGlyph.getGlyphId());
      }
    }

    return glyphs;
  }

  public static Ref<EntityStore> createEntity(ComponentAccessor<EntityStore> buffer, Holder<EntityStore> holder) {
    Ref<EntityStore> ref = buffer.addEntity(holder, AddReason.SPAWN);
    return ref;
  }
}
