package com.riprod.hexcode.core.state.execution;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.registry.GlyphRegistry;
import com.riprod.hexcode.core.common.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.common.hexcaster.component.HexcasterComponent;
import com.riprod.hexcode.core.common.hexcaster.utils.CasterInventory;
import com.riprod.hexcode.core.common.hexcaster.utils.PlayerUtils;
import com.riprod.hexcode.core.common.hexstaff.component.HexStaffAsset;
import com.riprod.hexcode.core.common.hexstaff.component.HexStaffComponent;
import com.riprod.hexcode.core.common.stats.HexcodeEntityStatTypes;
import com.riprod.hexcode.utils.HexSlot;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.core.state.execution.component.HexcasterExecutionComponent;
import com.riprod.hexcode.core.state.execution.component.RootGlyph;
import com.riprod.hexcode.core.state.execution.component.VolatilityTracker;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nonnull;

import com.riprod.hexcode.core.common.glyphs.component.Slot;

public class Executor {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private Executor() {
    }

    public static void continueFromSlot(Glyph glyph, String slotKey, HexContext hexContext) {
        Slot slot = glyph.getSlot(slotKey);
        if (slot == null)
            return;
        String[] links = slot.getLinks();
        if (links.length == 0)
            return;
        continueExecution(Arrays.asList(links), hexContext);
    }

    public static void beginExecution(List<String> nextGlyphs, @Nonnull HexContext hexContext) {

        UUIDComponent uuidComponent = hexContext.getAccessor().getComponent(
                hexContext.getCasterRef(), UUIDComponent.getComponentType());
        EntityVar casterVar = new EntityVar(EntityVar.createRef(uuidComponent.getUuid(), hexContext.getCasterRef()));

        hexContext.setVariable("1", casterVar);

        HexStaffComponent staff = CasterInventory.getHexStaffComponent(
                hexContext.getAccessor(), hexContext.getCasterRef());

        HexcasterExecutionComponent execComp = hexContext.getAccessor().getComponent(
                hexContext.getCasterRef(), HexcasterExecutionComponent.getComponentType());

        if (staff != null) {
            RootGlyph rootGlyph = hexContext.getAccessor().getComponent(
                    hexContext.getRoot().getRootEntityRef(), RootGlyph.getComponentType());

            float statMax = 0f;
            EntityStatMap statMap = hexContext.getAccessor().getComponent(
                    hexContext.getCasterRef(), EntityStatMap.getComponentType());
            if (statMap != null) {
                int volIndex = HexcodeEntityStatTypes.getVolatility();
                if (volIndex != Integer.MIN_VALUE) {
                    EntityStatValue volStat = statMap.get(volIndex);
                    if (volStat != null)
                        statMax = volStat.getMax();
                }
            }

            float startingBudget = Math.max(0, statMax - execComp.getCumulativeDecay());
            VolatilityTracker tracker = new VolatilityTracker(
                    startingBudget,
                    rootGlyph.getVolatilityMultiplier(),
                    rootGlyph.getManaCostMultiplier(),
                    rootGlyph.getPowerModifier());
            hexContext.setVolatilityTracker(tracker);

            execComp.advanceCast(staff.getCastDecayRate(), statMax);
            CasterInventory.saveHexStaffComponent(hexContext.getAccessor(), hexContext.getCasterRef(), staff);

            HexStaffAsset staffAsset = CasterInventory.getHexStaffAsset(
                    PlayerUtils.getHandItem(hexContext.getAccessor(), hexContext.getCasterRef(), HexSlot.MainHand));
            if (staffAsset != null && staffAsset.getColors() != null) {
                hexContext.setColors(staffAsset.getColors().clone());
            }
        }

        registerActiveHex(hexContext);

        RootGlyph rootGlyph = hexContext.getAccessor().getComponent(
                hexContext.getRoot().getRootEntityRef(), RootGlyph.getComponentType());
        if (rootGlyph != null) {
            rootGlyph.setOriginContext(hexContext);
        }

        continueExecution(nextGlyphs, hexContext);
    }

    public static void fail(HexContext hexContext) {
        VolatilityTracker tracker = hexContext.getVolatilityTracker();
        if (tracker != null) {
            tracker.setFailed(true);
            tracker.setFizzled(true);
        }
        unregisterActiveHex(hexContext);
    }

    public static void registerActiveHex(HexContext hexContext) {
        try {
            if (hexContext.getAccessor() == null || hexContext.getCasterRef() == null)
                return;
            HexcasterComponent comp = hexContext.getAccessor().getComponent(
                    hexContext.getCasterRef(), HexcasterComponent.getComponentType());
            if (comp != null) {
                comp.registerActiveHex(hexContext);
            }
        } catch (Exception e) {
            LOGGER.atWarning().log("failed to register active hex: %s", e.getMessage());
        }
    }

    public static void unregisterActiveHex(HexContext hexContext) {
        try {
            if (hexContext.getAccessor() == null || hexContext.getCasterRef() == null)
                return;
            if (!hexContext.getCasterRef().isValid())
                return;
            HexcasterComponent comp = hexContext.getAccessor().getComponent(
                    hexContext.getCasterRef(), HexcasterComponent.getComponentType());
            if (comp != null) {
                comp.removeActiveHex(hexContext);
            }
        } catch (Exception e) {
            LOGGER.atWarning().log("failed to unregister active hex: %s", e.getMessage());
        }
    }

    public static void continueExecution(List<String> nextGlyphs, HexContext hexContext) {
        if (nextGlyphs.isEmpty()) {
            return;
        }

        VolatilityTracker tracker = hexContext.getVolatilityTracker();
        if (tracker != null && (tracker.isFizzled() || tracker.isFailed())) {
            LOGGER.atInfo().log("hex %s - halting execution",
                    tracker.isFailed() ? "failed" : "fizzled");
            fail(hexContext);
            return;
        }

        boolean multiBranch = nextGlyphs.size() > 1;

        for (String nextNodeId : nextGlyphs) {

            try {
                executeNode(nextNodeId, multiBranch ? hexContext.clone() : hexContext);
            } catch (Exception e) {
                LOGGER.atSevere().log("error executing glyph %s: %s", nextNodeId, e.getMessage());
            }
        }
    }

    private static void executeNode(String nodeId, HexContext hexContext) {
        Glyph nextNode = hexContext.getGlyph(nodeId);
        if (nextNode == null) {
            LOGGER.atSevere().log("dangling glyph reference: %s not found in hex graph", nodeId);
            fail(hexContext);
            return;
        }
        GlyphHandler nextHandler = GlyphRegistry.get(nextNode.getGlyphId());
        if (nextHandler == null) {
            LOGGER.atSevere().log("no handler found for glyph %s, skipping", nextNode.getGlyphId());
            fail(hexContext);
            return;
        }
        try {
            if (!nextHandler.consumeResources(nextNode, hexContext)) {
                fail(hexContext);
                return;
            }
            LOGGER.atInfo().log("Executing glyph %s - ID: %s", nextNode.getGlyphId(), nextNode.getId());
            nextHandler.execute(nextNode, hexContext);
        } catch (Exception e) {
            LOGGER.atSevere().log("error executing glyph %s: %s", nextNode.getGlyphId(), e.getMessage());
            fail(hexContext);
        }
    }

}
