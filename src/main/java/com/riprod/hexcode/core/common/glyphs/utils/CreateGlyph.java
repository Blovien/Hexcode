package com.riprod.hexcode.core.common.glyphs.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import com.hypixel.hytale.builtin.mounts.MountedComponent;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.MountController;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelParticle;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.modules.entity.component.BoundingBox;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphComponent;
import com.riprod.hexcode.core.common.glyphs.registry.GlyphAsset;
import com.riprod.hexcode.core.common.hexes.component.HexComponent;
import com.riprod.hexcode.core.common.hidden.utils.HiddenUtils;
import com.riprod.hexcode.utils.GlyphMath;

public class CreateGlyph {
  private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

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

  public static Holder<EntityStore> createGlyphHolder(ComponentAccessor<EntityStore> accessor, GlyphComponent glyph,
      Vector3d parentPos) {
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

    // required
    holder.ensureComponent(EntityStore.REGISTRY.getNonSerializedComponentType());
    holder.addComponent(UUIDComponent.getComponentType(),
        new UUIDComponent(UUID.randomUUID()));
    holder.addComponent(BoundingBox.getComponentType(), new BoundingBox(model.getBoundingBox()));
    holder.addComponent(PersistentModel.getComponentType(), new PersistentModel(model.toReference()));
    int networkId = accessor.getExternalData().takeNextNetworkId();
    holder.addComponent(NetworkId.getComponentType(), new NetworkId(networkId));

    Ref<EntityStore> hexRoot = glyph.getParentRef();

    if (hexRoot != null) {
      MountedComponent mountComponent = new MountedComponent(hexRoot, glyph.getOffset(),
          MountController.Minecart);
      holder.addComponent(MountedComponent.getComponentType(), mountComponent);
    }

    return holder;
  }

  /**
   * Creates a glyph entity and all of it's children recursively - adding each
   * child ref to the parent map
   * 
   * @throws
   * @param accessor
   * @param holder
   * @return
   */
  public static Ref<EntityStore> createGlyph(CommandBuffer<EntityStore> accessor, GlyphComponent glyph,
      Vector3d parentPos, Ref<EntityStore> playerRef) {

    // Create the first glyph
    Holder<EntityStore> holder = createGlyphHolder(accessor, glyph, parentPos);
    HiddenUtils.addHiddenToHolder(accessor, holder, playerRef);
    Ref<EntityStore> ref = createEntity(accessor, holder);
    glyph.setSelfRef(ref); // backwards reference

    // effect logic
    EntityEffect effect = GlyphStyleUtil.getGlyphEffect(glyph.getVolatility(), glyph.getEfficiency());
    if (effect != null) {
      EffectControllerComponent effectController = holder
          .ensureAndGetComponent(EffectControllerComponent.getComponentType());
      effectController.addEffect(ref, effect, accessor);
    }

    return ref;
  }

  public static Ref<EntityStore> createEntity(ComponentAccessor<EntityStore> buffer, Holder<EntityStore> holder) {
    Ref<EntityStore> ref = buffer.addEntity(holder, AddReason.SPAWN);
    return ref;
  }
}
