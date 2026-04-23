package com.riprod.hexcode.builtin.glyphs.arc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.builtin.glyphs.arc.component.ArcComponent;
import com.riprod.hexcode.builtin.glyphs.arc.style.ArcStyle;
import com.riprod.hexcode.builtin.glyphs.arc.utils.ArcUtils;
import com.riprod.hexcode.core.common.construct.component.ConstructTickContext;
import com.riprod.hexcode.core.common.construct.component.HexStatus;
import com.riprod.hexcode.core.common.construct.state.NoState;
import com.riprod.hexcode.core.common.construct.handler.ConstructHandler;
import com.riprod.hexcode.core.common.construct.system.HexConstructSpawner;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.registry.GlyphAsset;
import com.riprod.hexcode.core.common.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.state.execution.HexExecuter;
import com.riprod.hexcode.core.state.execution.component.HexColors;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.core.state.execution.component.VolatilityTracker;

public class ArcConstructHandler implements ConstructHandler<NoState> {

    private static final float DEFAULT_JUMP_RANGE = 15.0f;
    private static final float SHOCK_OVERLAP = 0.25f;

    @Override
    public boolean onTick(float dt, HexStatus<NoState> status, ConstructTickContext ctx) {
        ArcComponent arc = ctx.getChunk().getComponent(
                ctx.getIndex(), ArcComponent.getComponentType());
        if (arc == null)
            return true;

        if (!arc.hasFired()) {
            return fireAndPrepareHop(arc, status, ctx);
        }

        float timer = status.incrementElapsedTime(dt);
        if (timer < arc.getDelay())
            return false;

        return hop(arc, status, ctx);
    }

    @Override
    public void onCleanup(HexStatus<NoState> status, ConstructTickContext ctx) {
        ArcComponent arc = ctx.getBuffer().getComponent(
                ctx.getEntityRef(), ArcComponent.getComponentType());
        if (arc != null && arc.isNaturalCompletion())
            return;

        TransformComponent tc = ctx.getBuffer().getComponent(
                ctx.getEntityRef(), TransformComponent.getComponentType());
        if (tc != null) {
            ArcStyle.renderFizzle(ctx.getBuffer(), tc.getPosition(),
                    status.getHexContext().getColors());
        }
    }

    private boolean fireAndPrepareHop(ArcComponent arc, HexStatus<NoState> status,
            ConstructTickContext ctx) {
        String branch = arc.getCurrentBranch();
        if (branch == null) {
            arc.setNaturalCompletion(true);
            return true;
        }

        HexContext hexContext = status.getHexContext();
        if (!tryConsumePerHopVolatility(arc, hexContext, ctx)) {
            return true;
        }

        final Ref<EntityStore> targetRef = arc.getCurrentTargetRef();
        final UUID targetUuid = arc.getCurrentTargetUuid();
        final Glyph triggeringGlyph = status.getTriggeringGlyph();

        if (targetRef != null && targetRef.isValid() && targetUuid != null
                && triggeringGlyph != null) {
            triggeringGlyph.writeOutput(
                    new EntityVar(targetUuid, targetRef), hexContext);
        }

        HexExecuter.continueExecution(List.of(branch), hexContext);

        arc.advanceBranch();
        arc.setHasFired(true);
        arc.resetTimer();

        if (!arc.hasMoreBranches()) {
            arc.setNaturalCompletion(true);
            return true;
        }

        return false;
    }

    private boolean tryConsumePerHopVolatility(ArcComponent arc, HexContext hexContext,
            ConstructTickContext ctx) {
        Glyph arcGlyph = arc.getArcGlyph();
        if (arcGlyph == null)
            return true;

        GlyphAsset asset = GlyphAsset.getAssetMap().getAsset(arcGlyph.getGlyphId());
        if (asset == null)
            return true;

        float baseCost = asset.getVolatilityCost() * arcGlyph.getVolatility();

        VolatilityTracker tracker = hexContext.getVolatilityTracker();
        if (tracker == null) {
            return false;
        }

        float glyphUsage = tracker.getGlyphUsageScaled(arcGlyph.getId());

        float rangeScale = arc.getMaxJumpDistance() / DEFAULT_JUMP_RANGE;
        float finalCost = baseCost * glyphUsage * rangeScale;

        if (hexContext.getRoot() == null)
            return true;
        return tracker.consumeVolatility(finalCost);
    }

    private boolean hop(ArcComponent arc, HexStatus<NoState> status, ConstructTickContext ctx) {
        TransformComponent tc = ctx.getBuffer().getComponent(
                ctx.getEntityRef(), TransformComponent.getComponentType());
        if (tc == null)
            return true;

        Vector3d fromPos = tc.getPosition();
        HexContext hexContext = status.getHexContext();

        Ref<EntityStore> nextTarget = ArcUtils.getNextArcTarget(
                fromPos, arc.getMaxJumpDistance(), arc.getVisitedEntities(), ctx.getBuffer());

        if (nextTarget == null)
            return true;

        UUIDComponent nextUuid = ctx.getBuffer().getComponent(nextTarget, UUIDComponent.getComponentType());
        if (nextUuid != null) {
            arc.getVisitedEntities().add(nextUuid.getUuid());
        }

        TransformComponent nextTc = ctx.getBuffer().getComponent(nextTarget, TransformComponent.getComponentType());
        Vector3d nextPos = nextTc != null ? nextTc.getPosition() : fromPos;

        HexColors colors = hexContext.getColors();
        World world = ctx.getBuffer().getExternalData().getWorld();
        ArcStyle.renderArc(ctx.getBuffer(), world, fromPos, nextPos, colors);
        ArcUtils.applyShockEffect(ctx.getBuffer(), nextTarget, arc.getDelay() + SHOCK_OVERLAP);
        ArcStyle.renderHit(ctx.getBuffer(), nextPos, colors);

        List<String> remainingBranches = new ArrayList<>(
                arc.getBranches().subList(arc.getBranchIndex(), arc.getBranches().size()));

        Set<UUID> visited = new HashSet<>(arc.getVisitedEntities());

        Holder<EntityStore> holder = HexConstructSpawner.create(
                ctx.getBuffer(), hexContext, status.getTriggeringGlyph(),
                ArcGlyph.ID,
                nextPos);

        ArcComponent nextHop = new ArcComponent(
                arc.getArcGlyph(), remainingBranches, visited,
                arc.getMaxJumpDistance(), arc.getDelay(),
                nextTarget, nextUuid != null ? nextUuid.getUuid() : null);
        holder.addComponent(ArcComponent.getComponentType(), nextHop);

        Ref<EntityStore> newRef = ctx.getBuffer().addEntity(holder, AddReason.SPAWN);

        status.getHexContext().getRoot().addDependency(hexContext, newRef);
        

        arc.setNaturalCompletion(true);
        return true;
    }
}
