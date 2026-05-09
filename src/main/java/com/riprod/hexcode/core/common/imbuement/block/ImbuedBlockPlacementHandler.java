package com.riprod.hexcode.core.common.imbuement.block;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.imbuement.component.ImbuedBlockComponent;
import com.riprod.hexcode.core.common.imbuement.component.ImbuementData;
import com.riprod.hexcode.core.common.imbuement.utils.ImbuementUtils;

public class ImbuedBlockPlacementHandler extends EntityEventSystem<EntityStore, PlaceBlockEvent> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public ImbuedBlockPlacementHandler() {
        super(PlaceBlockEvent.class);
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> chunk,
            @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> buffer,
            @Nonnull PlaceBlockEvent event) {

        try {
            ItemStack itemInHand = event.getItemInHand();
            ImbuementData data = ImbuementUtils.read(itemInHand, ImbuementUtils.DEFAULT_SLOT);
            if (data == null) return;

            Vector3i pos = event.getTargetBlock();
            World world = buffer.getExternalData().getWorld();
            ImbuementData snapshot = data.copy();

            world.execute(() -> {
                ImbuedBlockComponent comp = ImbuedBlockAttach.attach(
                        world, pos.x, pos.y, pos.z, ImbuedBlockComponent.getComponentType());
                if (comp == null) {
                    LOGGER.atWarning().log(
                            "[hexcode] failed to attach ImbuedBlockComponent at %s during place",
                            pos);
                    return;
                }
                comp.write(ImbuementUtils.DEFAULT_SLOT, snapshot);
                comp.setSlotsReady(0);
                comp.setLastChargeTick(world.getTick());
            });
        } catch (Exception e) {
            LOGGER.atSevere().log("[hexcode] ImbuedBlockPlacementHandler failed: %s", e.getMessage());
        }
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }
}
