package com.riprod.hexcode.core.common.imbuement;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.api.event.HexCastEvent;
import com.riprod.hexcode.core.common.hexes.component.Hex;
import com.riprod.hexcode.core.state.execution.component.PlayerHexRoot;
import com.riprod.hexcode.core.state.execution.component.VolatilityTracker;
import com.riprod.hexcode.core.state.execution.events.CastingEventData;
import com.riprod.hexcode.utils.SpellMana;

public class ImbuementExecutor {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private ImbuementExecutor() {
    }

    public static boolean execute(CommandBuffer<EntityStore> accessor, ImbuementData castData,
            Ref<EntityStore> sourceRef, Ref<EntityStore> targetRef) {
        try {
            return executeInternal(accessor, castData, sourceRef, targetRef);
        } catch (Exception e) {
            LOGGER.atSevere().log("[hexcode] imbuement execution failed: %s", e.getMessage());
            return false;
        }
    }

    private static boolean executeInternal(CommandBuffer<EntityStore> accessor, ImbuementData imbuementData,
            Ref<EntityStore> sourceRef, Ref<EntityStore> targetRef) {
        Hex hex = ImbuementUtils.resolveHex(imbuementData);

        if (hex == null) {
            LOGGER.atWarning().log("[hexcode] failed to resolve hex for imbuement execution");
            return false;
        }

        PlayerHexRoot hexRoot = new PlayerHexRoot(sourceRef);

        float volatilityMax = hexRoot.resolveVolatility(accessor);
        // Compute base mana cost by walking the glyphs (no variables yet resolved).
        float baseMana = SpellMana.computeTotalMana(hex);
        float resolvedPower = hexRoot.resolveSpellPower(accessor);

        VolatilityTracker volatilityTracker = new VolatilityTracker(volatilityMax, 1.0f, resolvedPower);
        CastingEventData castData = new CastingEventData(hex, sourceRef, baseMana, hexRoot, imbuementData.getColors(),
                volatilityTracker);

        HexCastEvent castEvent = new HexCastEvent(sourceRef, castData);
        HytaleServer.get().getEventBus().dispatchFor(HexCastEvent.class)
                .dispatch(castEvent);

        return true;
    }
}
