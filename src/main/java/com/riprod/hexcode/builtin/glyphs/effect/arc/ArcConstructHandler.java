package com.riprod.hexcode.builtin.glyphs.effect.arc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.builtin.glyphs.effect.arc.component.ArcComponent;
import com.riprod.hexcode.builtin.glyphs.effect.arc.style.ArcStyle;
import com.riprod.hexcode.builtin.glyphs.effect.arc.utils.ArcUtils;
import com.riprod.hexcode.core.common.construct.ConstructHandler;
import com.riprod.hexcode.core.common.construct.ConstructTickContext;
import com.riprod.hexcode.core.common.construct.HexConstructSpawner;
import com.riprod.hexcode.core.common.construct.component.HexConstruct;
import com.riprod.hexcode.core.common.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.state.execution.component.HexColors;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.core.state.execution.component.RootGlyph;

public class ArcConstructHandler implements ConstructHandler {

    @Override
    public boolean onTick(float dt, HexConstruct construct, ConstructTickContext ctx) {
        ArcComponent arc = ctx.getChunk().getComponent(
                ctx.getIndex(), ArcComponent.getComponentType());
        if (arc == null) return true;

        if (!arc.hasFired()) {
            return fireAndPrepareHop(arc, construct, ctx);
        }

        float timer = arc.incrementTimer(dt);
        if (timer < arc.getDelay()) return false;

        return hop(arc, construct, ctx);
    }

    @Override
    public void onCleanup(HexConstruct construct, ConstructTickContext ctx) {
        TransformComponent tc = ctx.getBuffer().getComponent(
                ctx.getEntityRef(), TransformComponent.getComponentType());
        if (tc != null) {
            ArcStyle.renderFizzle(ctx.getBuffer(), tc.getPosition(),
                    construct.getHexContext().getColors());
        }
    }

    private boolean fireAndPrepareHop(ArcComponent arc, HexConstruct construct,
            ConstructTickContext ctx) {
        String branch = arc.getCurrentBranch();
        if (branch == null) return true;

        final Ref<EntityStore> entityRef = ctx.getEntityRef();
        UUIDComponent uuid = ctx.getBuffer().getComponent(entityRef, UUIDComponent.getComponentType());

        ctx.fireBranch(List.of(branch), hexCtx -> {
            if (uuid != null && construct.getTriggeringGlyph() != null) {
                construct.getTriggeringGlyph().writeSlot("result",
                        new EntityVar(EntityVar.createRef(uuid.getUuid(), entityRef)), hexCtx);
            }
        });

        arc.advanceBranch();
        arc.setHasFired(true);
        arc.resetTimer();

        if (!arc.hasMoreBranches()) return true;

        return false;
    }

    private boolean hop(ArcComponent arc, HexConstruct construct, ConstructTickContext ctx) {
        TransformComponent tc = ctx.getBuffer().getComponent(
                ctx.getEntityRef(), TransformComponent.getComponentType());
        if (tc == null) return true;

        Vector3d fromPos = tc.getPosition();
        HexContext hexContext = construct.getHexContext();

        Ref<EntityStore> nextTarget = ArcUtils.getNextArcTarget(
                fromPos, arc.getMaxJumpDistance(), arc.getVisitedEntities(),
                hexContext.getCasterRef(), ctx.getBuffer());

        if (nextTarget == null) return true;

        UUIDComponent nextUuid = ctx.getBuffer().getComponent(nextTarget, UUIDComponent.getComponentType());
        if (nextUuid != null) {
            arc.getVisitedEntities().add(nextUuid.getUuid());
        }

        TransformComponent nextTc = ctx.getBuffer().getComponent(nextTarget, TransformComponent.getComponentType());
        Vector3d nextPos = nextTc != null ? nextTc.getPosition() : fromPos;

        HexColors colors = hexContext.getColors();
        World world = ctx.getBuffer().getExternalData().getWorld();
        ArcStyle.renderArc(ctx.getBuffer(), world, fromPos, nextPos, colors);
        ArcUtils.applyShockEffect(ctx.getBuffer(), nextTarget);
        ArcStyle.renderHit(ctx.getBuffer(), nextPos, colors);

        List<String> remainingBranches = new ArrayList<>(
                arc.getBranches().subList(arc.getBranchIndex(), arc.getBranches().size()));

        Set<UUID> visited = new HashSet<>(arc.getVisitedEntities());

        Holder<EntityStore> holder = HexConstructSpawner.create(
                ctx.getBuffer(), hexContext, construct.getTriggeringGlyph(),
                "arc", -1, 0,
                null, construct.getConditionalBranchIds(), null,
                nextPos);

        ArcComponent nextHop = new ArcComponent(
                arc.getArcGlyph(), remainingBranches, visited,
                arc.getMaxJumpDistance(), arc.getDelay());
        holder.addComponent(ArcComponent.getComponentType(), nextHop);

        Ref<EntityStore> newRef = ctx.getBuffer().addEntity(holder, AddReason.SPAWN);

        Ref<EntityStore> rootRef = construct.getRootEntityRef();
        if (rootRef != null && rootRef.isValid()) {
            RootGlyph rootGlyph = ctx.getBuffer().getComponent(rootRef, RootGlyph.getComponentType());
            if (rootGlyph != null) {
                rootGlyph.addDependent(newRef);
            }
        }

        return true;
    }
}
