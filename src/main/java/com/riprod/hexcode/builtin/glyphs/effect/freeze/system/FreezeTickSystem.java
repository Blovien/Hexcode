package com.riprod.hexcode.builtin.glyphs.effect.freeze.system;

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
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.builtin.glyphs.effect.freeze.component.FreezeComponent;
import com.riprod.hexcode.builtin.glyphs.effect.freeze.component.FrozenBlock;
import com.riprod.hexcode.builtin.glyphs.effect.freeze.style.FreezeStyle;
import com.riprod.hexcode.core.state.execution.component.HexSignal;

public class FreezeTickSystem extends EntityTickingSystem<EntityStore> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Override
    public Query<EntityStore> getQuery() {
        return FreezeComponent.getComponentType();
    }

    @Override
    public void tick(float dt, int index, ArchetypeChunk<EntityStore> chunk,
            Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {
        try {
            FreezeComponent freeze = chunk.getComponent(index, FreezeComponent.getComponentType());
            Ref<EntityStore> entityRef = chunk.getReferenceTo(index);

            if (freeze == null) {
                removeEntity(entityRef, buffer);
                return;
            }

            freeze.incrementElapsed(dt);

            if (!freeze.isExpired()) {
                return;
            }

            HexSignal signal = buffer.getComponent(entityRef, HexSignal.getComponentType());

            World world = buffer.getExternalData().getWorld();
            restoreBlocks(freeze, signal, world, buffer);
            decrementWaiters(signal, buffer);
            removeEntity(entityRef, buffer);

            LOGGER.atInfo().log("freeze: restored %d blocks after %.1fs",
                    freeze.getFrozenBlocks().size(), freeze.getDurationSeconds());
        } catch (Exception e) {
            LOGGER.atSevere().log("[hexcode] FreezeTickSystem failed: %s", e.getMessage());
        }
    }

    private void restoreBlocks(FreezeComponent freeze, HexSignal signal, World world,
            CommandBuffer<EntityStore> buffer) {
        for (FrozenBlock block : freeze.getFrozenBlocks()) {
            Vector3i pos = block.getPosition();
            Vector3d blockCenter = new Vector3d(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5);

            world.setBlock(pos.x, pos.y, pos.z, block.getBlockTypeId());
            if (signal != null && signal.getPrimary() != null) {
                FreezeStyle.renderMelt(blockCenter, signal.getPrimary().getHexContext().getColors(), buffer);
            }
        }
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
