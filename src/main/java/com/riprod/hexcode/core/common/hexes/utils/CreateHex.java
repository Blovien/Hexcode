package com.riprod.hexcode.core.common.hexes.utils;

import java.util.UUID;

import com.hypixel.hytale.builtin.mounts.MountedComponent;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.MountController;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.hexes.component.HexComponent;

public class CreateHex {
    public static Holder<EntityStore> createHexHolder(ComponentAccessor<EntityStore> accessor, HexComponent hex,
            Vector3d parentPos) {
        Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();

        holder.addComponent(HexComponent.getComponentType(), hex);

        // Position
        TransformComponent hexTransform = new TransformComponent(parentPos,
                new Vector3f(hex.getPitch(), hex.getYaw(), 0));

        holder.addComponent(TransformComponent.getComponentType(), hexTransform);

        // persistance
        holder.ensureComponent(EntityStore.REGISTRY.getNonSerializedComponentType());
        holder.addComponent(UUIDComponent.getComponentType(), new UUIDComponent(UUID.randomUUID()));
        int networkId = accessor.getExternalData().takeNextNetworkId();
        holder.addComponent(NetworkId.getComponentType(), new NetworkId(networkId));

        Ref<EntityStore> hexParent = hex.getParentRef();

        if (hexParent != null) {
            MountedComponent mounted = new MountedComponent(hexParent, hex.getOffset(),
                    MountController.Minecart);
            holder.addComponent(MountedComponent.getComponentType(), mounted);
        }

        return holder;
    }

    /**
     * @throws
     * @param accessor
     * @param holder
     * @return
     */
    public static Ref<EntityStore> createHexEntity(ComponentAccessor<EntityStore> accessor,
            HexComponent hexComp, Vector3d parentPos) {

        Holder<EntityStore> holder = createHexHolder(accessor, hexComp, parentPos);

        Ref<EntityStore> ref = createEntity(accessor, holder);

        return ref;
    }

    public static Ref<EntityStore> createEntity(ComponentAccessor<EntityStore> buffer, Holder<EntityStore> holder) {
        Ref<EntityStore> ref = buffer.addEntity(holder, AddReason.SPAWN);
        return ref;
    }
}
