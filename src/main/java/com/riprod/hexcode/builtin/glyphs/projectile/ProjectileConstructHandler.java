package com.riprod.hexcode.builtin.glyphs.projectile;

import java.util.List;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.projectile.config.StandardPhysicsProvider;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.riprod.hexcode.builtin.glyphs.projectile.component.ProjectileComponent;
import com.riprod.hexcode.builtin.glyphs.projectile.style.ProjectileStyle;
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

public class ProjectileConstructHandler implements ConstructHandler<NoState> {

    private static final double HIT_IDENTIFICATION_RADIUS = 1.5;

    @Override
    public boolean onTick(float dt, HexStatus<NoState> status, ConstructTickContext ctx) {
        ProjectileComponent propel = ctx.getChunk().getComponent(ctx.getIndex(),
                ProjectileComponent.getComponentType());
        TransformComponent transform = ctx.getChunk().getComponent(ctx.getIndex(),
                TransformComponent.getComponentType());
        StandardPhysicsProvider physics = ctx.getChunk().getComponent(ctx.getIndex(),
                StandardPhysicsProvider.getComponentType());

        if (propel == null || transform == null || physics == null) return true;

        CommandBuffer<EntityStore> buffer = ctx.getBuffer();
        HexContext hexContext = status.getHexContext();
        Vector3d currentPos = transform.getPosition();

        StandardPhysicsProvider.STATE state = physics.getState();

        if (state == StandardPhysicsProvider.STATE.INACTIVE) {
            Vector3d contactPos = physics.getContactPosition();
            Ref<EntityStore> hitEntity = findEntityAtPosition(contactPos, propel.getCasterRef(),
                    ctx.getEntityRef(), buffer);

            HexVar resultVar = null;
            if (hitEntity != null) {
                UUIDComponent uuidComp = buffer.getComponent(hitEntity, UUIDComponent.getComponentType());
                if (uuidComp != null) {
                    resultVar = new EntityVar(uuidComp.getUuid(), hitEntity);
                }
                TransformComponent hitTransform = buffer.getComponent(hitEntity,
                        TransformComponent.getComponentType());
                Vector3d hitPos = hitTransform != null ? hitTransform.getPosition() : contactPos;
                ProjectileStyle.renderEntityHit(currentPos, hitPos, hexContext.getColors(), buffer);
            } else {
                ProjectileStyle.renderBlockHit(contactPos, hexContext.getColors(), buffer);
            }

            fireWithResult(status, ctx, resultVar);
            return true;
        }

        if (state == StandardPhysicsProvider.STATE.RESTING) {
            Vector3d contactPos = physics.getContactPosition();
            HexVar resultVar = new BlockVar(contactPos.toVector3i());

            ProjectileStyle.renderBlockHit(contactPos, hexContext.getColors(), buffer);
            fireWithResult(status, ctx, resultVar);
            return true;
        }

        double distanceTraveled = new Vector3d(currentPos).subtract(propel.getSpawnPosition()).length();
        if (distanceTraveled >= propel.getMaxDistance()) {
            ProjectileStyle.renderMiss(currentPos, hexContext.getColors(), buffer);
            fireWithResult(status, ctx, null);
            return true;
        }

        hexContext.getVolatilityTracker().consumeVolatility(dt);
        return false;
    }

    @Override
    public void onCleanup(HexStatus<NoState> status, ConstructTickContext ctx) {
        ctx.getBuffer().tryRemoveEntity(ctx.getEntityRef(), RemoveReason.REMOVE);
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
            Ref<EntityStore> projectileRef, CommandBuffer<EntityStore> buffer) {
        List<Ref<EntityStore>> nearby = TargetUtil.getAllEntitiesInSphere(
                position, HIT_IDENTIFICATION_RADIUS, buffer);

        Ref<EntityStore> closest = null;
        double closestDist = Double.MAX_VALUE;

        for (Ref<EntityStore> ref : nearby) {
            if (ref == null || !ref.isValid()) continue;
            if (ref.equals(casterRef) || ref.equals(projectileRef)) continue;
            if (buffer.getComponent(ref, ProjectileComponent.getComponentType()) != null) continue;

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
