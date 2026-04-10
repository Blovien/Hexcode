package com.riprod.hexcode.core.state.execution;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.registry.GlyphRegistry;
import com.riprod.hexcode.core.common.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.common.hexcaster.component.HexcasterComponent;
import com.riprod.hexcode.core.common.hexcaster.utils.CasterInventory;
import com.riprod.hexcode.core.common.hexcaster.utils.PlayerUtils;
import com.riprod.hexcode.core.common.hexstaff.component.HexStaffAsset;
import com.riprod.hexcode.core.common.hexstaff.component.HexStaffComponent;
import com.riprod.hexcode.utils.HexSlot;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.core.state.execution.component.PendingContinue;
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
        if (slot == null) return;
        String[] links = slot.getLinks();
        if (links.length == 0) return;
        continueExecution(Arrays.asList(links), hexContext);
    }

    public static void delayFromSlot(Glyph glyph, String slotKey, HexContext hexContext, float delaySeconds) {
        Slot slot = glyph.getSlot(slotKey);
        if (slot == null) return;
        String[] links = slot.getLinks();
        if (links.length == 0) return;
        delayContinuation(Arrays.asList(links), hexContext, delaySeconds);
    }

    public static void beginExecution(List<String> nextGlyphs, @Nonnull HexContext hexContext) {

        UUIDComponent uuidComponent = hexContext.getAccessor().getComponent(
                hexContext.getCasterRef(), UUIDComponent.getComponentType());
        EntityVar casterVar = new EntityVar(EntityVar.createRef(uuidComponent.getUuid(), hexContext.getCasterRef()));

        hexContext.setVariable("1", casterVar);

        HexStaffComponent staff = CasterInventory.getHexStaffComponent(
                hexContext.getAccessor(), hexContext.getCasterRef());
        HexcasterComponent casterComp = hexContext.getAccessor().getComponent(
                hexContext.getCasterRef(), HexcasterComponent.getComponentType());
        if (staff != null) {
            RootGlyph rootGlyph = hexContext.getAccessor().getComponent(
                    hexContext.getRoot().getRootEntityRef(), RootGlyph.getComponentType());

            VolatilityTracker tracker = new VolatilityTracker(
                    casterComp.getCastCount(), staff.getStaffModifier(),
                    rootGlyph.getPowerModifier(),
                    rootGlyph.getVolatilityMultiplier(),
                    rootGlyph.getManaCostMultiplier());
            hexContext.setVolatilityTracker(tracker);

            casterComp.incrementCastCount();
            CasterInventory.saveHexStaffComponent(hexContext.getAccessor(), hexContext.getCasterRef(), staff);

            HexStaffAsset staffAsset = CasterInventory.getHexStaffAsset(
                    PlayerUtils.getHandItem(hexContext.getAccessor(), hexContext.getCasterRef(), HexSlot.MainHand));
            if (staffAsset != null && staffAsset.getColors() != null) {
                hexContext.setColors(staffAsset.getColors().clone());
            }
        }

        continueExecution(nextGlyphs, hexContext);
    }

    public static void continueExecution(List<String> nextGlyphs, HexContext hexContext) {

        if (nextGlyphs.isEmpty()) {
            return;
        }

        VolatilityTracker tracker = hexContext.getVolatilityTracker();
        if (tracker != null && tracker.isFizzled()) {
            LOGGER.atInfo().log("hex fizzled — halting execution");
            return;
        }

        if (nextGlyphs.size() == 1) {
            String nextId = nextGlyphs.get(0);
            Glyph nextNode = hexContext.getGlyph(nextId);
            if (nextNode == null) {
                LOGGER.atSevere().log("dangling glyph reference: %s not found in hex graph", nextId);
                return;
            }
            GlyphHandler nextHandler = GlyphRegistry.get(nextNode.getGlyphId());
            if (nextHandler == null) {
                LOGGER.atSevere().log("no handler found for glyph %s, skipping", nextNode.getGlyphId());
                return;
            }
            try {
                if (!nextHandler.canExecute(nextNode, hexContext)) {
                    return;
                }
                LOGGER.atInfo().log("Executing glyph %s - ID: %s", nextNode.getGlyphId(), nextNode.getId());
                nextHandler.execute(nextNode, hexContext);
            } catch (Exception e) {
                LOGGER.atSevere().log("error executing glyph %s: %s", nextNode.getGlyphId(), e.getMessage());
            }
            return;
        }

        for (String nextNodeId : nextGlyphs) {
            Glyph nextNode = hexContext.getGlyph(nextNodeId);
            if (nextNode == null) {
                LOGGER.atSevere().log("dangling glyph reference: %s not found in hex graph", nextNodeId);
                continue;
            }
            GlyphHandler nextHandler = GlyphRegistry.get(nextNode.getGlyphId());
            if (nextHandler == null) {
                LOGGER.atSevere().log("no handler found for glyph %s, skipping", nextNode.getGlyphId());
                continue;
            }
            HexContext copiedBranch = hexContext.copy();
            try {
                if (!nextHandler.canExecute(nextNode, copiedBranch)) {
                    continue;
                }
                LOGGER.atInfo().log("Executing glyph %s - ID: %s", nextNode.getGlyphId(), nextNode.getId());
                nextHandler.execute(nextNode, copiedBranch);
            } catch (Exception e) {
                LOGGER.atSevere().log("error executing glyph %s: %s", nextNode.getGlyphId(), e.getMessage());
            }
        }
    }

    public static void delayContinuation(List<String> nextGlyphs, HexContext hexContext, float delaySeconds) {
        RootGlyph rootGlyph = hexContext.getAccessor().getComponent(
                hexContext.getRoot().getRootEntityRef(), RootGlyph.getComponentType());

        PendingContinue pending = new PendingContinue(
                nextGlyphs,
                hexContext,
                delaySeconds);
        rootGlyph.addPendingContinue(pending);
    }
}
