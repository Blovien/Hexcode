package com.riprod.hexcode.builtin.glyphs.effect.glaciate.system;

import java.util.List;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.projectile.config.StandardPhysicsProvider;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.riprod.hexcode.builtin.glyphs.effect.glaciate.component.GlaciateComponent;
import com.riprod.hexcode.builtin.glyphs.effect.glaciate.component.GlaciateState;
import com.riprod.hexcode.builtin.glyphs.effect.glaciate.style.GlaciateStyle;
import com.riprod.hexcode.core.state.execution.component.HexSignal;

public class GlaciateSystem extends EntityTickingSystem<EntityStore> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static int damageCauseIndex = -1;

    @Override
    public Query<EntityStore> getQuery() {
        return GlaciateComponent.getComponentType();
    }

    @Override
    public void tick(float dt, int index, ArchetypeChunk<EntityStore> chunk,
            Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {
        GlaciateComponent glaciate = chunk.getComponent(index, GlaciateComponent.getComponentType());
        Ref<EntityStore> entityRef = chunk.getReferenceTo(index);
        TransformComponent transform = chunk.getComponent(index, TransformComponent.getComponentType());

        if (glaciate == null || transform == null) {
            removeEntity(entityRef, buffer);
            return;
        }

        HexSignal signal = buffer.getComponent(entityRef, HexSignal.getComponentType());
        if (signal == null || signal.getEntries().isEmpty()) {
            removeEntity(entityRef, buffer);
            return;
        }

        HexSignal.SignalEntry entry = signal.getPrimary();
        if (entry == null) {
            removeEntity(entityRef, buffer);
            return;
        }

        switch (glaciate.getState()) {
            case FALLING:
                tickFalling(glaciate, signal, entityRef, transform, index, chunk, buffer);
                break;
            case LANDED:
                glaciate.setState(GlaciateState.PERSIST);
                break;
            case PERSIST:
                glaciate.decrementPersistTimer(dt);
                if (glaciate.getPersistTimer() <= 0) {
                    glaciate.setState(GlaciateState.DESPAWN);
                }
                break;
            case DESPAWN:
                GlaciateStyle.renderMelt(transform.getPosition(), entry.getHexContext().getColors(), buffer);
                decrementWaiters(signal, buffer);
                removeEntity(entityRef, buffer);
                break;
        }
    }

    private void tickFalling(GlaciateComponent glaciate, HexSignal signal, Ref<EntityStore> entityRef,
            TransformComponent transform, int index, ArchetypeChunk<EntityStore> chunk,
            CommandBuffer<EntityStore> buffer) {
        StandardPhysicsProvider physics = chunk.getComponent(
                index, StandardPhysicsProvider.getComponentType());
        if (physics == null)
            return;

        StandardPhysicsProvider.STATE state = physics.getState();
        if (state == StandardPhysicsProvider.STATE.RESTING || state == StandardPhysicsProvider.STATE.INACTIVE) {
            Vector3d impactPos = transform.getPosition();
            HexSignal.SignalEntry entry = signal.getPrimary();
            applyAreaDamage(impactPos, glaciate, entry, buffer);
            GlaciateStyle.renderImpact(impactPos, entry.getHexContext().getColors(), buffer);
            continueExecution(signal, buffer);
            glaciate.setState(GlaciateState.LANDED);
        }
    }

    private void applyAreaDamage(Vector3d center, GlaciateComponent glaciate,
            HexSignal.SignalEntry entry, CommandBuffer<EntityStore> buffer) {
        List<Ref<EntityStore>> entities = TargetUtil.getAllEntitiesInSphere(
                center, glaciate.getDamageRadius(), buffer);

        if (damageCauseIndex < 0) {
            damageCauseIndex = DamageCause.getAssetMap().getIndex("Environment");
        }
        if (damageCauseIndex == Integer.MIN_VALUE)
            return;

        DamageCause cause = DamageCause.getAssetMap().getAsset(damageCauseIndex);
        if (cause == null)
            return;

        Ref<EntityStore> casterRef = entry.getHexContext().getCasterRef();

        for (Ref<EntityStore> ref : entities) {
            if (ref == null || !ref.isValid())
                continue;
            if (ref.equals(casterRef))
                continue;
            if (buffer.getComponent(ref, GlaciateComponent.getComponentType()) != null)
                continue;

            Damage damage = new Damage(
                    new Damage.EnvironmentSource("hex_glaciate"), cause, glaciate.getBaseDamage());
            DamageSystems.executeDamage(ref, buffer, damage);
        }
    }

    private void continueExecution(HexSignal signal, CommandBuffer<EntityStore> buffer) {
        if (signal == null) return;
        signal.fireAllEntries(buffer);
    }

    private void decrementWaiters(HexSignal signal, CommandBuffer<EntityStore> buffer) {
        if (signal == null) return;
        signal.decrementAllWaiters(buffer);
    }

    private void removeEntity(Ref<EntityStore> entityRef, CommandBuffer<EntityStore> buffer) {
        Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
        buffer.removeEntity(entityRef, holder, RemoveReason.REMOVE);
    }
}
