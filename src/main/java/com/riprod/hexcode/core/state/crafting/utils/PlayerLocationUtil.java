package com.riprod.hexcode.core.state.crafting.utils;

import java.util.ArrayList;
import java.util.List;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import org.joml.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.riprod.hexcode.core.common.hexcaster.component.HexcasterComponent;

import io.sentry.util.Pair;

public class PlayerLocationUtil {
    public static List<Pair<Ref<EntityStore>, HexcasterComponent>> findNearbyPlayers(
            CommandBuffer<EntityStore> accessor, Vector3d anchor, double radius) {
        List<Ref<EntityStore>> nearbyEntities = TargetUtil.getAllEntitiesInSphere(anchor, radius, accessor);
        List<Pair<Ref<EntityStore>, HexcasterComponent>> entityPairs = new ArrayList<>();

        for (Ref<EntityStore> entity : nearbyEntities) {
            if (entity == null || !entity.isValid())
                continue;

            HexcasterComponent component = accessor.getComponent(entity, HexcasterComponent.getComponentType());

            if (component == null)
                continue;

            Pair<Ref<EntityStore>, HexcasterComponent> itemPair = new Pair<>(entity, component);

            entityPairs.add(itemPair);
        }

        return entityPairs;
    }
}
