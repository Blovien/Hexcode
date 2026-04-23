package com.riprod.hexcode.builtin.glyphs.glaciate;

import java.util.List;
import java.util.UUID;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.knockback.KnockbackComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.hypixel.hytale.protocol.ChangeVelocityType;
import com.riprod.hexcode.builtin.glyphs.glaciate.component.GlaciateComponent;
import com.riprod.hexcode.builtin.glyphs.glaciate.style.GlaciateStyle;
import com.riprod.hexcode.core.common.construct.handler.ConstructHandler;
import com.riprod.hexcode.core.common.construct.component.ConstructTickContext;
import com.riprod.hexcode.core.common.construct.component.HexStatus;
import com.riprod.hexcode.core.common.construct.state.NoState;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.state.execution.HexExecuter;
import com.riprod.hexcode.core.state.execution.component.HexContext;

public class GlaciateConstructHandler implements ConstructHandler<NoState> {

    private static int damageCauseIndex = -1;

    @Override
    public boolean onTick(float dt, HexStatus<NoState> status, ConstructTickContext ctx) {
        GlaciateComponent glaciate = ctx.getChunk().getComponent(
                ctx.getIndex(), GlaciateComponent.getComponentType());
        TransformComponent transform = ctx.getChunk().getComponent(
                ctx.getIndex(), TransformComponent.getComponentType());
        if (glaciate == null || transform == null) return false;

        Velocity vel = ctx.getChunk().getComponent(ctx.getIndex(), Velocity.getComponentType());
        Vector3d iceVelocity = vel != null ? new Vector3d(vel.getVelocity()) : Vector3d.ZERO;
        double speed = vel != null ? vel.getSpeed() : 0;

        Vector3d center = transform.getPosition();
        List<Ref<EntityStore>> found = TargetUtil.getAllEntitiesInSphere(
                center, glaciate.getDamageRadius(), ctx.getBuffer());

        Ref<EntityStore> casterRef = status.getHexContext().getCasterRef();

        for (Ref<EntityStore> ref : found) {
            if (ref == null || !ref.isValid()) continue;
            if (ref.equals(ctx.getEntityRef())) continue;
            if (ref.equals(casterRef)) continue;
            if (ctx.getBuffer().getComponent(ref, GlaciateComponent.getComponentType()) != null) continue;

            UUIDComponent uuid = ctx.getBuffer().getComponent(ref, UUIDComponent.getComponentType());
            if (uuid == null) continue;

            if (glaciate.getHitEntities().contains(uuid.getUuid())) continue;
            glaciate.getHitEntities().add(uuid.getUuid());

            if (speed > 0.1) {
                float damage = (float) (speed * glaciate.getDamageMultiplier());
                damage *= status.getHexContext().getMagicPowerMultiplier();
                applyDamage(ref, damage, ctx);
                applyKnockback(ref, iceVelocity, speed, ctx);
            }

            GlaciateStyle.renderImpact(center,
                    status.getHexContext().getColors(), ctx.getBuffer());

            final Ref<EntityStore> entityRef = ref;
            final UUIDComponent entityUuid = uuid;
            Glyph triggering = status.getTriggeringGlyph();
            if (triggering != null) {
                HexContext __hexCtx = status.getHexContext();
                triggering.writeDefaultOutput(
                        new EntityVar(entityUuid.getUuid(), entityRef), __hexCtx);
                HexExecuter.continueExecution(triggering.getNextLinks(), __hexCtx);
            }
        }

        return false;
    }

    @Override
    public void onCleanup(HexStatus<NoState> status, ConstructTickContext ctx) {
        TransformComponent transform = ctx.getBuffer().getComponent(
                ctx.getEntityRef(), TransformComponent.getComponentType());
        if (transform != null) {
            GlaciateStyle.renderMelt(transform.getPosition(),
                    status.getHexContext().getColors(), ctx.getBuffer());
        }
    }

    private void applyDamage(Ref<EntityStore> targetRef, float amount, ConstructTickContext ctx) {
        if (damageCauseIndex < 0) {
            damageCauseIndex = DamageCause.getAssetMap().getIndex("Environment");
        }
        if (damageCauseIndex == Integer.MIN_VALUE) return;

        DamageCause cause = DamageCause.getAssetMap().getAsset(damageCauseIndex);
        if (cause == null) return;

        Damage damage = new Damage(
                new Damage.EnvironmentSource("hex_glaciate"), cause, amount);
        DamageSystems.executeDamage(targetRef, ctx.getBuffer(), damage);
    }

    private void applyKnockback(Ref<EntityStore> ref, Vector3d iceVelocity, double speed,
            ConstructTickContext ctx) {
        Vector3d kbVelocity = new Vector3d(iceVelocity).normalize().scale(speed * 0.3);
        kbVelocity.setY(Math.max(kbVelocity.getY(), 2.0));
        KnockbackComponent kb = new KnockbackComponent();
        kb.setVelocity(kbVelocity);
        kb.setVelocityType(ChangeVelocityType.Add);
        kb.setDuration(0.0f);
        ctx.getBuffer().putComponent(ref, KnockbackComponent.getComponentType(), kb);
    }
}
