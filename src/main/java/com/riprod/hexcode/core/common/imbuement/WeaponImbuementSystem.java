package com.riprod.hexcode.core.common.imbuement;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.hexes.component.Hex;

public class WeaponImbuementSystem extends DamageEventSystem {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String SLOT_KEY = "weapon";

    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }

    @Nullable
    @Override
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getInspectDamageGroup();
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> chunk,
            @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> buffer,
            @Nonnull Damage damage) {
        if (damage.isCancelled()) return;
        if (!(damage.getSource() instanceof Damage.EntitySource entitySource)) return;

        Ref<EntityStore> attackerRef = entitySource.getRef();
        if (!attackerRef.isValid()) return;

        Ref<EntityStore> targetRef = chunk.getReferenceTo(index);

        ItemStack weapon = InventoryComponent.getItemInHand(buffer, attackerRef);
        if (weapon == null || weapon.isEmpty()) return;

        ImbuementData data = ImbuementUtils.read(weapon);
        if (data == null) return;

        ImbuementCooldownComponent cooldown = buffer.ensureAndGetComponent(
                attackerRef, ImbuementCooldownComponent.getComponentType());
        if (cooldown.isOnCooldown(SLOT_KEY)) return;

        Hex hex = ImbuementUtils.resolveHex(data);
        if (hex == null) return;

        ImbuementExecutor.Request request = new ImbuementExecutor.Request(
                attackerRef, targetRef, hex, data, buffer, data.getColors());
        boolean success = ImbuementExecutor.execute(request);

        if (success) {
            long cooldownMs = data.getCooldownTicks() * 50L;
            cooldown.setCooldown(SLOT_KEY, cooldownMs);
        }
    }
}
