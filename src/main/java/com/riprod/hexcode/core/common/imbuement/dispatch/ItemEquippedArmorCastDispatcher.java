package com.riprod.hexcode.core.common.imbuement.dispatch;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.api.event.HexCastEvent;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.common.hexes.component.Hex;
import com.riprod.hexcode.core.common.imbuement.asset.ImbuementProfileAsset;
import com.riprod.hexcode.core.common.imbuement.component.ImbuementData;
import com.riprod.hexcode.core.common.imbuement.registry.ImbuementProfileRegistry;
import com.riprod.hexcode.core.common.imbuement.utils.ImbuementUtils;
import com.riprod.hexcode.core.common.triggers.component.TriggerEvent;
import com.riprod.hexcode.core.common.triggers.registry.Trigger;
import com.riprod.hexcode.core.state.execution.component.PlayerHexRoot;
import com.riprod.hexcode.core.state.execution.component.VolatilityTracker;
import com.riprod.hexcode.core.state.execution.events.CastingEventData;
import com.riprod.hexcode.utils.SpellMana;

public final class ItemEquippedArmorCastDispatcher implements CastRootDispatcher {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Override
    public void dispatch(@Nonnull Trigger trigger, @Nonnull TriggerEvent event,
            @Nonnull CommandBuffer<EntityStore> buffer) {
        Ref<EntityStore> player = event.subjectRef();
        if (player == null || !player.isValid()) return;

        Store<EntityStore> store = player.getStore();
        InventoryComponent.Armor armor = store.getComponent(player, InventoryComponent.Armor.getComponentType());
        if (armor == null) return;

        ItemContainer container = armor.getInventory();
        if (container == null) return;

        short capacity = container.getCapacity();
        for (short i = 0; i < capacity; i++) {
            ItemStack stack = container.getItemStack(i);
            if (stack == null || stack.isEmpty()) continue;
            if (ImbuementUtils.readAll(stack).isEmpty()) continue;

            ImbuementProfileAsset profile = ImbuementProfileRegistry.first(stack);
            if (profile == null || profile.findSlot(trigger.getId()) == null) continue;

            ImbuementData data = ImbuementUtils.read(stack, trigger.getId());
            if (data == null) continue;
            Hex hex = ImbuementUtils.resolveHex(data);
            if (hex == null) continue;

            fireOne(trigger, event, buffer, player, hex, data);
        }
    }

    private void fireOne(Trigger trigger, TriggerEvent event,
            CommandBuffer<EntityStore> buffer, Ref<EntityStore> player,
            Hex hex, ImbuementData data) {
        PlayerHexRoot hexRoot = new PlayerHexRoot(player);
        float volatilityMax = hexRoot.resolveVolatility(buffer);
        float baseMana = SpellMana.computeTotalMana(hex);
        float resolvedPower = hexRoot.resolveSpellPower(buffer);
        VolatilityTracker tracker = new VolatilityTracker(volatilityMax, 1.0f, resolvedPower);

        HexVar defaultVar = trigger.resolveDefaultVariable(event);
        CastingEventData castData = new CastingEventData(
                hex, player, baseMana, hexRoot, data.getColors(), tracker);
        if (defaultVar != null) castData.setDefaultVariable(defaultVar);
        castData.setCastSlotKey(trigger.getId());

        try {
            buffer.invoke(new HexCastEvent(player, castData));
        } catch (Exception e) {
            LOGGER.atSevere().log("%s armor imbuement dispatch failed: %s", trigger.getId(), e.getMessage());
        }
    }
}
