package com.riprod.hexcode.builtin.glyphs.halt.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.modules.physics.component.PhysicsValues;
import com.hypixel.hytale.server.core.modules.projectile.config.StandardPhysicsProvider;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.builtin.glyphs.halt.component.HaltProjectileComponent;

public class HaltProjectileTickSystem extends EntityTickingSystem<EntityStore> {

    @Override
    public Query<EntityStore> getQuery() {
        return HaltProjectileComponent.getComponentType();
    }

    @Override
    public void tick(float dt, int index, ArchetypeChunk<EntityStore> chunk,
            Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {
        HaltProjectileComponent halt = chunk.getComponent(index,
                HaltProjectileComponent.getComponentType());
        if (halt == null) return;

        if (!halt.tick(dt)) return;

        Ref<EntityStore> ref = chunk.getReferenceTo(index);

        StandardPhysicsProvider physics = chunk.getComponent(index,
                StandardPhysicsProvider.getComponentType());
        if (physics != null) {
            physics.setState(StandardPhysicsProvider.STATE.ACTIVE);
        }

        PhysicsValues original = halt.getOriginalPhysicsValues();
        if (original != null) {
            buffer.putComponent(ref, PhysicsValues.getComponentType(), new PhysicsValues(original));
        }

        buffer.removeComponent(ref, HaltProjectileComponent.getComponentType());
    }
}
