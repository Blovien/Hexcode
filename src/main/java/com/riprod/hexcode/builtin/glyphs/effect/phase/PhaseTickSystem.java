package com.riprod.hexcode.builtin.glyphs.effect.phase;

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
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.riprod.hexcode.core.state.execution.component.HexSignal;

public class PhaseTickSystem extends EntityTickingSystem<EntityStore> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final float BASE_CRUSH_DAMAGE = 4.0f;
    private static int damageCauseIndex = -1;

    @Override
    public Query<EntityStore> getQuery() {
        return PhaseComponent.getComponentType();
    }

    @Override
    public void tick(float dt, int index, ArchetypeChunk<EntityStore> chunk,
            Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {
        PhaseComponent phase = chunk.getComponent(index, PhaseComponent.getComponentType());
        Ref<EntityStore> entityRef = chunk.getReferenceTo(index);

        if (phase == null) {
            removeEntity(entityRef, buffer);
            return;
        }

        phase.incrementElapsed();

        if (!phase.isExpired()) {
            return;
        }

        HexSignal signal = buffer.getComponent(entityRef, HexSignal.getComponentType());

        World world = buffer.getExternalData().getWorld();
        restoreBlocks(phase, signal, world, buffer);
        continueExecution(signal, buffer);
        removeEntity(entityRef, buffer);

        LOGGER.atInfo().log("phase: restored %d blocks after %d ticks",
                phase.getPhasedBlocks().size(), phase.getDurationTicks());
    }

    private void restoreBlocks(PhaseComponent phase, HexSignal signal, World world,
            CommandBuffer<EntityStore> buffer) {
        for (PhasedBlock block : phase.getPhasedBlocks()) {
            Vector3i pos = block.getPosition();
            Vector3d blockCenter = new Vector3d(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5);

            applyCrushDamage(pos, blockCenter, buffer);
            world.setBlock(pos.x, pos.y, pos.z, block.getBlockTypeId());
            if (signal != null && signal.getPrimary() != null) {
                PhaseStyle.renderPhaseIn(blockCenter, signal.getPrimary().getHexContext().getColors(), buffer);
            }
        }
    }

    private void applyCrushDamage(Vector3i pos, Vector3d blockCenter,
            CommandBuffer<EntityStore> buffer) {
        Vector3d min = new Vector3d(pos.x, pos.y, pos.z);
        Vector3d max = new Vector3d(pos.x + 1.0, pos.y + 1.0, pos.z + 1.0);
        List<Ref<EntityStore>> entities = TargetUtil.getAllEntitiesInBox(min, max, buffer);

        if (damageCauseIndex < 0) {
            damageCauseIndex = DamageCause.getAssetMap().getIndex("Environment");
        }

        for (Ref<EntityStore> ref : entities) {
            if (ref == null || !ref.isValid()) continue;

            TransformComponent tc = buffer.getComponent(ref, TransformComponent.getComponentType());
            if (tc == null) continue;

            if (damageCauseIndex >= 0) {
                DamageCause cause = DamageCause.getAssetMap().getAsset(damageCauseIndex);
                if (cause != null) {
                    Damage damage = new Damage(
                            new Damage.EnvironmentSource("hex_phase"), cause, BASE_CRUSH_DAMAGE);
                    DamageSystems.executeDamage(ref, buffer, damage);
                }
            }

            PhaseStyle.renderCrush(blockCenter, null, buffer);
            LOGGER.atInfo().log("phase: crush damage to entity at %s", pos);
        }
    }

    private void continueExecution(HexSignal signal, CommandBuffer<EntityStore> buffer) {
        if (signal == null) return;
        signal.fireAllEntries(buffer);
        signal.decrementAllWaiters(buffer);
    }

    private void removeEntity(Ref<EntityStore> entityRef, CommandBuffer<EntityStore> buffer) {
        Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
        buffer.removeEntity(entityRef, holder, RemoveReason.REMOVE);
    }
}
