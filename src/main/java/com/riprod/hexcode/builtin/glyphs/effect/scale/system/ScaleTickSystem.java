package com.riprod.hexcode.builtin.glyphs.effect.scale.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.entity.component.BoundingBox;
import com.hypixel.hytale.server.core.modules.entity.component.EntityScaleComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.builtin.glyphs.effect.scale.component.ScaleComponent;

public class ScaleTickSystem extends EntityTickingSystem<EntityStore> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Override
    public Query<EntityStore> getQuery() {
        return ScaleComponent.getComponentType();
    }

    @Override
    public void tick(float dt, int index, ArchetypeChunk<EntityStore> chunk,
            Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {
        try {
            ScaleComponent scale = chunk.getComponent(index, ScaleComponent.getComponentType());
            if (scale == null) return;
            if (!scale.tick(dt)) return;

            Ref<EntityStore> ref = chunk.getReferenceTo(index);

            EntityScaleComponent scaleComp = buffer.getComponent(ref,
                    EntityScaleComponent.getComponentType());
            if (scaleComp != null) {
                scaleComp.setScale(scale.getOriginalScale());
            }
            BoundingBox box = buffer.getComponent(ref, BoundingBox.getComponentType());
            if (box != null && scale.getOriginalBoundingBox() != null) {
                box.setBoundingBox(scale.getOriginalBoundingBox());
            }

            buffer.removeComponent(ref, ScaleComponent.getComponentType());
            LOGGER.atInfo().log("scale: effect expired on entity");
        } catch (Exception e) {
            LOGGER.atSevere().log("[hexcode] ScaleTickSystem failed: %s", e.getMessage());
        }
    }
}
