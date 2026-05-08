package com.riprod.hexcode.builtin.triggers.use;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.api.event.HexCastEvent;
import com.riprod.hexcode.builtin.triggers.InteractionPayload;
import com.riprod.hexcode.builtin.triggers.TriggerKey;
import com.riprod.hexcode.core.common.hexes.component.Hex;
import com.riprod.hexcode.core.common.imbuement.component.ImbuementData;
import com.riprod.hexcode.core.common.imbuement.utils.ImbuementUtils;
import com.riprod.hexcode.core.common.triggers.component.TriggerEvent;
import com.riprod.hexcode.core.common.triggers.component.TriggerSubscription;
import com.riprod.hexcode.core.common.triggers.handler.TriggerCallback;
import com.riprod.hexcode.core.common.triggers.registry.TriggerListenerRegistry;
import com.riprod.hexcode.core.state.execution.component.PlayerHexRoot;
import com.riprod.hexcode.core.state.execution.component.VolatilityTracker;
import com.riprod.hexcode.core.state.execution.events.CastingEventData;
import com.riprod.hexcode.utils.SpellMana;

public final class UseImbuementBinder {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private UseImbuementBinder() {
    }

    public static void register(@Nonnull TriggerListenerRegistry registry) {
        registry.subscribe(TriggerSubscription.bootstrap(TriggerKey.USE, UseImbuementBinder::onFire));
    }

    private static void onFire(CommandBuffer<EntityStore> buffer, TriggerSubscription sub, TriggerEvent event) {
        if (!(event.payload() instanceof InteractionPayload ip)) return;
        Ref<EntityStore> player = ip.player();
        if (player == null || !player.isValid()) return;

        ItemStack heldItem = com.hypixel.hytale.server.core.inventory.InventoryComponent.getItemInHand(buffer, player);
        if (heldItem == null || heldItem.isEmpty()) return;

        ImbuementData data = ImbuementUtils.read(heldItem, TriggerKey.USE);
        if (data == null) return;
        Hex hex = ImbuementUtils.resolveHex(data);
        if (hex == null) return;

        PlayerHexRoot hexRoot = new PlayerHexRoot(player);
        float volatilityMax = hexRoot.resolveVolatility(buffer);
        float baseMana = SpellMana.computeTotalMana(hex);
        float resolvedPower = hexRoot.resolveSpellPower(buffer);
        VolatilityTracker tracker = new VolatilityTracker(volatilityMax, 1.0f, resolvedPower);

        CastingEventData castData = new CastingEventData(hex, player, baseMana, hexRoot, data.getColors(), tracker);
        castData.setCastSlotKey(TriggerKey.USE);

        try {
            buffer.invoke(new HexCastEvent(player, castData));
        } catch (Exception e) {
            LOGGER.atSevere().log("Use imbuement dispatch failed: %s", e.getMessage());
        }
    }
}
