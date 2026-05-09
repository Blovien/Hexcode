package com.riprod.hexcode.core.common.imbuement.dispatch;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.api.event.HexCastEvent;
import com.riprod.hexcode.builtin.triggers.InteractionPayload;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.common.hexes.component.Hex;
import com.riprod.hexcode.core.common.imbuement.component.ImbuementData;
import com.riprod.hexcode.core.common.imbuement.utils.ImbuementUtils;
import com.riprod.hexcode.core.common.triggers.component.TriggerEvent;
import com.riprod.hexcode.core.common.triggers.registry.Trigger;
import com.riprod.hexcode.core.state.execution.component.PlayerHexRoot;
import com.riprod.hexcode.core.state.execution.component.VolatilityTracker;
import com.riprod.hexcode.core.state.execution.events.CastingEventData;
import com.riprod.hexcode.utils.SpellMana;

public final class ItemHeldCastDispatcher implements CastRootDispatcher {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Override
    public void dispatch(@Nonnull Trigger trigger, @Nonnull TriggerEvent event,
            @Nonnull CommandBuffer<EntityStore> buffer) {
        Ref<EntityStore> player = event.subjectRef();
        if (player == null || !player.isValid()) return;

        ItemStack heldItem = resolveHeldItem(event, buffer, player);
        if (heldItem == null || heldItem.isEmpty()) return;
        if (ImbuementUtils.readAll(heldItem).isEmpty()) return;

        ImbuementData data = ImbuementUtils.read(heldItem, trigger.getId());
        if (data == null) return;
        Hex hex = ImbuementUtils.resolveHex(data);
        if (hex == null) return;

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
            LOGGER.atSevere().log("%s imbuement dispatch failed: %s", trigger.getId(), e.getMessage());
        }
    }

    private static ItemStack resolveHeldItem(TriggerEvent event,
            CommandBuffer<EntityStore> buffer, Ref<EntityStore> player) {
        if (event.payload() instanceof InteractionPayload ip && ip.itemInHand() != null) {
            return ip.itemInHand();
        }
        return InventoryComponent.getItemInHand(buffer, player);
    }
}
