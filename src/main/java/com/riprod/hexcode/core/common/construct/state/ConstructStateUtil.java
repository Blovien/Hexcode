package com.riprod.hexcode.core.common.construct.state;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.construct.component.HexEffectsComponent;
import com.riprod.hexcode.core.common.construct.component.HexStatus;

public final class ConstructStateUtil {

    private ConstructStateUtil() {
    }

    @Nullable
    public static <S extends ConstructState> S findState(
            @Nonnull CommandBuffer<EntityStore> buffer,
            @Nonnull Ref<EntityStore> entity,
            @Nonnull String handlerId,
            @Nonnull Class<S> stateClass) {
        HexEffectsComponent effects = buffer.getComponent(entity, HexEffectsComponent.getComponentType());
        if (effects == null) return null;
        for (HexStatus<?> status : effects.getEffects().values()) {
            if (handlerId.equals(status.getHandlerId())) {
                Object s = status.getState();
                if (s != null && stateClass.isInstance(s)) {
                    return stateClass.cast(s);
                }
            }
        }
        return null;
    }

    @Nonnull
    public static <S extends ConstructState> List<S> findAllStates(
            @Nonnull CommandBuffer<EntityStore> buffer,
            @Nonnull Ref<EntityStore> entity,
            @Nonnull String handlerId,
            @Nonnull Class<S> stateClass) {
        List<S> result = new ArrayList<>();
        HexEffectsComponent effects = buffer.getComponent(entity, HexEffectsComponent.getComponentType());
        if (effects == null) return result;
        for (HexStatus<?> status : effects.getEffects().values()) {
            if (handlerId.equals(status.getHandlerId())) {
                Object s = status.getState();
                if (s != null && stateClass.isInstance(s)) {
                    result.add(stateClass.cast(s));
                }
            }
        }
        return result;
    }

    public static void forEachStatus(
            @Nonnull CommandBuffer<EntityStore> buffer,
            @Nonnull Ref<EntityStore> entity,
            @Nonnull String handlerId,
            @Nonnull BiConsumer<UUID, HexStatus<?>> consumer) {
        HexEffectsComponent effects = buffer.getComponent(entity, HexEffectsComponent.getComponentType());
        if (effects == null) return;
        for (Map.Entry<UUID, HexStatus<?>> entry : effects.getEffects().entrySet()) {
            if (handlerId.equals(entry.getValue().getHandlerId())) {
                consumer.accept(entry.getKey(), entry.getValue());
            }
        }
    }

    public static int requestKillByHandlerId(
            @Nonnull CommandBuffer<EntityStore> buffer,
            @Nonnull Ref<EntityStore> entity,
            @Nonnull String handlerId) {
        HexEffectsComponent effects = buffer.getComponent(entity, HexEffectsComponent.getComponentType());
        if (effects == null) return 0;
        int count = 0;
        for (HexStatus<?> status : effects.getEffects().values()) {
            if (handlerId.equals(status.getHandlerId())) {
                status.requestKill();
                count++;
            }
        }
        return count;
    }
}
