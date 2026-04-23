package com.riprod.hexcode.builtin.glyphs.shatter;

import java.util.List;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.projectile.config.StandardPhysicsProvider;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.riprod.hexcode.builtin.glyphs.shatter.component.ShatterComponent;
import com.riprod.hexcode.builtin.glyphs.shatter.style.ShatterStyle;
import com.riprod.hexcode.core.common.construct.handler.ConstructHandler;
import com.riprod.hexcode.core.common.construct.component.ConstructTickContext;
import com.riprod.hexcode.core.common.construct.component.HexStatus;
import com.riprod.hexcode.core.common.construct.state.NoState;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.variables.BlockVar;
import com.riprod.hexcode.core.common.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.state.execution.HexExecuter;
import com.riprod.hexcode.core.state.execution.component.HexContext;

public class ShatterConstructHandler implements ConstructHandler<NoState> {

    private static final double HIT_IDENTIFICATION_RADIUS = 1.5;

    @Override
    public boolean onTick(float dt, HexStatus<NoState> status, ConstructTickContext ctx) {
        ShatterComponent shatter = ctx.getChunk().getComponent(
                ctx.getIndex(), ShatterComponent.getComponentType());
        TransformComponent transform = ctx.getChunk().getComponent(
                ctx.getIndex(), TransformComponent.getComponentType());
        StandardPhysicsProvider physics = ctx.getChunk().getComponent(
                ctx.getIndex(), StandardPhysicsProvider.getComponentType());

        if (shatter == null || transform == null || physics == null) return true;

        StandardPhysicsProvider.STATE state = physics.getState();
        Vector3d currentPos = transform.getPosition();
        HexContext hexContext = status.getHexContext();

        if (state == StandardPhysicsProvider.STATE.INACTIVE) {
            Vector3d contactPos = physics.getContactPosition();
            Ref<EntityStore> hitEntity = findEntityAtPosition(contactPos, shatter.getCasterRef(),
                    ctx.getEntityRef(), ctx.getBuffer());

            HexVar resultVar = null;
            if (hitEntity != null) {
                UUIDComponent uuidComp = ctx.getBuffer().getComponent(
                        hitEntity, UUIDComponent.getComponentType());
                if (uuidComp != null) {
                    resultVar = new EntityVar(uuidComp.getUuid(), hitEntity);
                }
            } else {
                resultVar = new BlockVar(contactPos.toVector3i());
            }

            ShatterStyle.renderShardHit(contactPos, hexContext.getColors(), ctx.getBuffer());
            fireWithResult(status, ctx, resultVar);
            return true;
        }

        if (state == StandardPhysicsProvider.STATE.RESTING) {
            Vector3d contactPos = physics.getContactPosition();
            HexVar resultVar = new BlockVar(contactPos.toVector3i());

            ShatterStyle.renderShardHit(contactPos, hexContext.getColors(), ctx.getBuffer());
            fireWithResult(status, ctx, resultVar);
            return true;
        }

        double distanceTraveled = new Vector3d(currentPos).subtract(shatter.getSpawnPosition()).length();
        if (distanceTraveled >= shatter.getMaxDistance()) {
            ShatterStyle.renderMiss(currentPos, hexContext.getColors(), ctx.getBuffer());
            fireWithResult(status, ctx, null);
            return true;
        }

        return false;
    }

    private void fireWithResult(HexStatus<NoState> status, ConstructTickContext ctx, HexVar resultVar) {
        Glyph triggering = status.getTriggeringGlyph();
        if (triggering == null) return;
        HexContext __hexCtx = status.getHexContext();
        if (resultVar != null) {
            triggering.writeOutput(resultVar, __hexCtx);
        }
        HexExecuter.continueExecution(triggering.getNextLinks(), __hexCtx);
    }

    private Ref<EntityStore> findEntityAtPosition(Vector3d position, Ref<EntityStore> casterRef,
            Ref<EntityStore> shardRef, CommandBuffer<EntityStore> buffer) {
        List<Ref<EntityStore>> nearby = TargetUtil.getAllEntitiesInSphere(
                position, HIT_IDENTIFICATION_RADIUS, buffer);

        Ref<EntityStore> closest = null;
        double closestDist = Double.MAX_VALUE;

        for (Ref<EntityStore> ref : nearby) {
            if (ref == null || !ref.isValid()) continue;
            if (ref.equals(casterRef) || ref.equals(shardRef)) continue;
            if (buffer.getComponent(ref, ShatterComponent.getComponentType()) != null) continue;

            TransformComponent tc = buffer.getComponent(ref, TransformComponent.getComponentType());
            UUIDComponent uuid = buffer.getComponent(ref, UUIDComponent.getComponentType());
            if (tc == null || uuid == null) continue;

            double dist = new Vector3d(position).subtract(tc.getPosition()).length();
            if (dist < closestDist) {
                closestDist = dist;
                closest = ref;
            }
        }

        return closest;
    }
}
