package com.riprod.hexcode.core.drawing.system;

import java.util.List;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.spatial.SpatialResource;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.Color;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.modules.entity.component.PropComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.player.component.HexcasterComponent;
import com.riprod.hexcode.utils.GlyphMath;

public class InterfaceManager {

    private static final String DRAWING_PARTICLE = "Hexcode_Drawing_Dot";
    private static final Color DRAWING_COLOR = new Color((byte) 34.5, (byte) 59, (byte) 84);

    public static void spawnTrails(ComponentAccessor<EntityStore> accessor, Ref<EntityStore> playerRef,
            HeadRotation head) {
        TransformComponent transform = accessor.getComponent(playerRef, TransformComponent.getComponentType());
        ModelComponent playerModel = accessor.getComponent(playerRef, ModelComponent.getComponentType());
        HexcasterComponent hexcaster = accessor.getComponent(playerRef, HexcasterComponent.getComponentType());

        if (hexcaster.getTrailRef() != null) {
            return; // already spawned
        }

        if (head == null || transform == null || playerModel == null) {
            return; // skip
        }

        float eyeHeight = playerModel.getModel().getEyeHeight();
        Vector3d eyePos = new Vector3d(transform.getPosition()).add(0, eyeHeight, 0);

        Vector3f rotation = head.getRotation();

        Vector3d position = GlyphMath.sphericalToCartesian(eyePos, head.getRotation().getYaw(),
                head.getRotation().getPitch(), 2.0);

        // spawning the trail anchor entity
        ModelAsset modelAsset = ModelAsset.getAssetMap().getAsset("Trail_Anchor");
        Model model = Model.createUnitScaleModel(modelAsset);

        Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();

        holder.addComponent(TransformComponent.getComponentType(),
                new TransformComponent(position, rotation));

        holder.ensureComponent(UUIDComponent.getComponentType());

        holder.addComponent(ModelComponent.getComponentType(),
                new ModelComponent(model));

        holder.addComponent(PersistentModel.getComponentType(),
                new PersistentModel(model.toReference()));

        holder.ensureComponent(PropComponent.getComponentType());

        Ref<EntityStore> trailRef = accessor.addEntity(holder, AddReason.SPAWN);
        hexcaster.setTrailRef(trailRef);

    }

    public static void removeTrails(ComponentAccessor<EntityStore> accessor, Ref<EntityStore> playerRef) {
        HexcasterComponent hexcaster = accessor.getComponent(playerRef, HexcasterComponent.getComponentType());
        if (hexcaster.getTrailRef() != null) {
            Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
            accessor.removeEntity(hexcaster.getTrailRef(), holder, RemoveReason.REMOVE);
            hexcaster.setTrailRef(null);
        }
    }

    public static void positionTrail(ComponentAccessor<EntityStore> accessor, Ref<EntityStore> playerRef,
            HeadRotation head) {
        HexcasterComponent hexcaster = accessor.getComponent(playerRef, HexcasterComponent.getComponentType());
        Ref<EntityStore> trailRef = hexcaster.getTrailRef();
        if (trailRef == null || trailRef.isValid() == false)
            return;

        TransformComponent transform = accessor.getComponent(playerRef, TransformComponent.getComponentType());
        ModelComponent playerModel = accessor.getComponent(playerRef, ModelComponent.getComponentType());

        if (head == null || transform == null || playerModel == null) {
            return; // skip
        }

        float eyeHeight = playerModel.getModel().getEyeHeight();
        Vector3d eyePos = new Vector3d(transform.getPosition()).add(0, eyeHeight, 0);

        Vector3f rotation = head.getRotation();

        Vector3d position = GlyphMath.sphericalToCartesian(eyePos, head.getRotation().getYaw(),
                head.getRotation().getPitch(), 2.0);

        TransformComponent trailTransform = accessor.getComponent(trailRef, TransformComponent.getComponentType());
        trailTransform.setPosition(position);
    }
}
