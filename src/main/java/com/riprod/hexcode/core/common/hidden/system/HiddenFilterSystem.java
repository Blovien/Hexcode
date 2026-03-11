package com.riprod.hexcode.core.common.hidden.system;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.tracker.EntityTrackerSystems;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.hidden.component.HiddenComponent;

public class HiddenFilterSystem extends EntityTickingSystem<EntityStore> {

    @Nonnull
    private final Set<Dependency<EntityStore>> dependencies = Collections.singleton(
            new SystemDependency<>(Order.AFTER, EntityTrackerSystems.CollectVisible.class));

    @Nullable
    @Override
    public SystemGroup<EntityStore> getGroup() {
        return EntityTrackerSystems.FIND_VISIBLE_ENTITIES_GROUP;
    }

    @Nonnull
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return this.dependencies;
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(EntityTrackerSystems.EntityViewer.getComponentType(), Player.getComponentType());
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        EntityTrackerSystems.EntityViewer viewer = archetypeChunk.getComponent(index,
                EntityTrackerSystems.EntityViewer.getComponentType());
        if (viewer == null)
            return;

        Ref<EntityStore> playerRef = archetypeChunk.getReferenceTo(index);

        Iterator<Ref<EntityStore>> it = viewer.visible.iterator();
        while (it.hasNext()) {
            Ref<EntityStore> ref = it.next();
            HiddenComponent hidden = commandBuffer.getComponent(ref, HiddenComponent.getComponentType());
            if (hidden == null)
                continue;
            Ref<EntityStore> owner = hidden.getOwnerRef();
            if (owner != null && !owner.equals(playerRef)) {
                viewer.hiddenCount++;
                it.remove();
            }
        }
    }
}
