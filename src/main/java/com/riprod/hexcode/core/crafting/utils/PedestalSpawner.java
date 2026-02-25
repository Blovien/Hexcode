package com.riprod.hexcode.core.crafting.utils;

import java.util.UUID;

import javax.annotation.Nullable;

import com.hypixel.hytale.builtin.mounts.MountedComponent;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.MountController;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.crafting.component.PedestalComponent;
import com.riprod.hexcode.core.crafting.component.PedestalState;

public class PedestalSpawner {

    public static Ref<EntityStore> createAnchorEntity(ComponentAccessor<EntityStore> accessor,
            Vector3i blockPos, @Nullable String essenceItemId, String bookItemId) {

        Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();

        Vector3d anchorPos = new Vector3d(blockPos.x + 0.5, blockPos.y + 1.0, blockPos.z + 0.5);
        holder.addComponent(TransformComponent.getComponentType(),
                new TransformComponent(anchorPos, new Vector3f(0, 0, 0)));

        holder.addComponent(UUIDComponent.getComponentType(),
                new UUIDComponent(UUID.randomUUID()));

        holder.ensureComponent(EntityStore.REGISTRY.getNonSerializedComponentType());

        int networkId = accessor.getExternalData().takeNextNetworkId();
        holder.addComponent(NetworkId.getComponentType(), new NetworkId(networkId));

        PedestalComponent pedestal = new PedestalComponent();
        pedestal.setBlockPosition(blockPos);
        pedestal.setPedestalState(PedestalState.ACTIVATING);
        pedestal.setEssenceItemId(essenceItemId);
        pedestal.setBookItemId(bookItemId);
        pedestal.setTransitionTimer(pedestal.getActivatingDuration());
        holder.addComponent(PedestalComponent.getComponentType(), pedestal);

        Ref<EntityStore> anchorRef = accessor.addEntity(holder, AddReason.SPAWN);

        if (essenceItemId != null) {
            Ref<EntityStore> essenceRef = spawnEssenceDisplay(accessor, anchorRef, anchorPos, essenceItemId);
            pedestal.setEssenceDisplayRef(essenceRef);
        }

        Ref<EntityStore> bookRef = spawnBookDisplay(accessor, anchorRef, anchorPos, bookItemId);
        pedestal.setBookDisplayRef(bookRef);

        return anchorRef;
    }

    public static Ref<EntityStore> spawnEssenceDisplay(ComponentAccessor<EntityStore> accessor,
            Ref<EntityStore> anchorRef, Vector3d anchorPos, String essenceItemId) {

        Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();

        holder.addComponent(TransformComponent.getComponentType(),
                new TransformComponent(anchorPos, new Vector3f(0, 0, 0)));

        holder.addComponent(UUIDComponent.getComponentType(),
                new UUIDComponent(UUID.randomUUID()));
        holder.ensureComponent(EntityStore.REGISTRY.getNonSerializedComponentType());

        int networkId = accessor.getExternalData().takeNextNetworkId();
        holder.addComponent(NetworkId.getComponentType(), new NetworkId(networkId));

        holder.addComponent(MountedComponent.getComponentType(),
                new MountedComponent(anchorRef, new Vector3f(0, 0.8f, 0),
                        MountController.Minecart));

        return accessor.addEntity(holder, AddReason.SPAWN);
    }

    public static Ref<EntityStore> spawnBookDisplay(ComponentAccessor<EntityStore> accessor,
            Ref<EntityStore> anchorRef, Vector3d anchorPos, String bookItemId) {

        Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();

        holder.addComponent(TransformComponent.getComponentType(),
                new TransformComponent(anchorPos, new Vector3f(0, 0, 0)));

        holder.addComponent(UUIDComponent.getComponentType(),
                new UUIDComponent(UUID.randomUUID()));
        holder.ensureComponent(EntityStore.REGISTRY.getNonSerializedComponentType());

        int networkId = accessor.getExternalData().takeNextNetworkId();
        holder.addComponent(NetworkId.getComponentType(), new NetworkId(networkId));

        holder.addComponent(MountedComponent.getComponentType(),
                new MountedComponent(anchorRef, new Vector3f(0, 1.2f, 0),
                        MountController.Minecart));

        return accessor.addEntity(holder, AddReason.SPAWN);
    }

    public static void despawnAnchor(Ref<EntityStore> anchorRef, PedestalComponent comp,
            CommandBuffer<EntityStore> buffer) {

        Ref<EntityStore> essenceRef = comp.getEssenceDisplayRef();
        if (essenceRef != null && essenceRef.isValid()) {
            buffer.removeEntity(essenceRef, RemoveReason.REMOVE);
        }

        Ref<EntityStore> bookRef = comp.getBookDisplayRef();
        if (bookRef != null && bookRef.isValid()) {
            buffer.removeEntity(bookRef, RemoveReason.REMOVE);
        }

        buffer.removeEntity(anchorRef, RemoveReason.REMOVE);
    }
}
