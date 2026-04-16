package com.riprod.hexcode.builtin.glyphs.effect.arc.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.selector.Selector;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.OverlapBehavior;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.riprod.hexcode.builtin.glyphs.effect.arc.component.ArcComponent;

public class ArcUtils {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String SHOCK_EFFECT_ID = "Hexcode_Shock";

    private ArcUtils() {
    }

    public static void applyShockEffect(CommandBuffer<EntityStore> accessor,
            Ref<EntityStore> targetRef, float durationSeconds) {
        EntityEffect shockEffect = EntityEffect.getAssetMap().getAsset(SHOCK_EFFECT_ID);
        if (shockEffect == null) {
            LOGGER.atWarning().log("arc: Hexcode_Shock effect asset not found");
            return;
        }

        EffectControllerComponent controller = accessor.getComponent(
                targetRef, EffectControllerComponent.getComponentType());
        if (controller == null) {
            LOGGER.atFine().log("arc: target has no EffectControllerComponent, skipping shock");
            return;
        }

        controller.addEffect(targetRef, shockEffect, durationSeconds, OverlapBehavior.OVERWRITE, accessor);
    }

    public static Ref<EntityStore> getNextArcTarget(
            Vector3d fromPosition, float maxDistance, Set<UUID> visited,
            Ref<EntityStore> casterRef, CommandBuffer<EntityStore> buffer) {

        List<Ref<EntityStore>> candidates = new ArrayList<>();

        Selector.selectNearbyEntities(buffer, fromPosition, maxDistance, candidates::add, ref -> {
            if (ref.equals(casterRef)) return false;

            if (buffer.getComponent(ref, ArcComponent.getComponentType()) != null) return false;

            UUIDComponent uuid = buffer.getComponent(ref, UUIDComponent.getComponentType());
            if (uuid == null) return false;
            if (visited.contains(uuid.getUuid())) return false;

            TransformComponent tc = buffer.getComponent(ref, TransformComponent.getComponentType());
            return tc != null;
        });

        if (candidates.isEmpty()) return null;
        return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
    }
}
