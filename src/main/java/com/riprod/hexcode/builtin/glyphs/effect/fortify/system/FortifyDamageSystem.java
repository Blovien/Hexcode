package com.riprod.hexcode.builtin.glyphs.effect.fortify.system;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.builtin.glyphs.effect.fortify.component.FortifyComponent;

public class FortifyDamageSystem extends DamageEventSystem {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final float MIN_DAMAGE_FLOOR = 1.0f;

    @Override
    public Query<EntityStore> getQuery() {
        return FortifyComponent.getComponentType();
    }

    @Nullable
    @Override
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getFilterDamageGroup();
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> chunk,
            @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> buffer,
            @Nonnull Damage damage) {
        try {
            FortifyComponent fortify = chunk.getComponent(index, FortifyComponent.getComponentType());
            if (fortify == null) return;

            float original = damage.getAmount();
            float reduced = Math.max(MIN_DAMAGE_FLOOR, original - fortify.getDamageReduction());
            damage.setAmount(reduced);

            LOGGER.atInfo().log("fortify: reduced damage %.2f -> %.2f (%.2f flat reduction)",
                    original, reduced, fortify.getDamageReduction());
        } catch (Exception e) {
            LOGGER.atSevere().log("[hexcode] FortifyDamageSystem failed: %s", e.getMessage());
        }
    }
}
