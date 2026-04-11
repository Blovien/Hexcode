package com.riprod.hexcode.core.common.hidden.utils;

import java.util.Iterator;
import java.util.List;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.hidden.component.HiddenComponent;
import com.riprod.hexcode.core.common.hover.component.HoverableComponent;

public class HiddenUtils {
    private static HytaleLogger logger = HytaleLogger.forEnclosingClass();

    public static void filterByOwner(ComponentAccessor<EntityStore> accessor,
            List<Ref<EntityStore>> refs, Ref<EntityStore> playerRef) {
        Iterator<Ref<EntityStore>> it = refs.iterator();
        while (it.hasNext()) {
            Ref<EntityStore> ref = it.next();
            if (ref == null || !ref.isValid()) {
                it.remove();
                continue;
            }
            HiddenComponent hidden = accessor.getComponent(ref, HiddenComponent.getComponentType());
            if (hidden == null) {
                HoverableComponent hoverable = accessor.getComponent(ref, HoverableComponent.getComponentType());
                continue;
            }
            Ref<EntityStore> owner = hidden.getOwnerRef();
            if (owner != null && !owner.equals(playerRef)) {
                it.remove();
            }
        }
    }

    public static void addHiddenToHolder(CommandBuffer<EntityStore> accessor, Holder<EntityStore> holder,
            Ref<EntityStore> playerRef) {
        if (playerRef != null && playerRef.isValid()) {
            holder.addComponent(HiddenComponent.getComponentType(), new HiddenComponent(playerRef));
        }
    }
}
