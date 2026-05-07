package com.riprod.hexcode.core.common.imbuement.block;

import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.block.components.ItemContainerBlock;
import com.hypixel.hytale.server.core.universe.world.World;
import com.riprod.hexcode.core.common.imbuement.component.ImbuementData;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class EssenceRefill {

    private static final Vector3i[] FACE_OFFSETS = new Vector3i[] {
            new Vector3i( 1,  0,  0),
            new Vector3i(-1,  0,  0),
            new Vector3i( 0,  1,  0),
            new Vector3i( 0, -1,  0),
            new Vector3i( 0,  0,  1),
            new Vector3i( 0,  0, -1)
    };

    private EssenceRefill() {
    }

    @Nullable
    public static ImbuementData tryRefill(@Nonnull World world, @Nonnull Vector3i blockPos) {
        for (Vector3i offset : FACE_OFFSETS) {
            int x = blockPos.x + offset.x;
            int y = blockPos.y + offset.y;
            int z = blockPos.z + offset.z;

            ItemContainerBlock container = BlockModule.getComponent(
                    ItemContainerBlock.getComponentType(), world, x, y, z);
            if (container == null) continue;

            ItemContainer inv = container.getItemContainer();
            if (inv == null) continue;

            short capacity = inv.getCapacity();
            for (short slot = 0; slot < capacity; slot++) {
                ItemStack stack = inv.getItemStack(slot);
                if (stack == null || stack.isEmpty()) continue;

                String itemId = stack.getItemId();
                java.util.Optional<EssenceMetadata.Parsed> parsed = EssenceMetadata.parse(itemId);
                if (parsed.isEmpty()) continue;

                EssenceMetadata.Parsed essence = parsed.get();
                inv.removeItemStackFromSlot(slot, 1);

                ImbuementData overlay = new ImbuementData();
                overlay.setColors(EssenceMetadata.colorsFor(itemId));
                overlay.setVolatilityMultiplier(essence.isConcentrated() ? 2.0f : 1.0f);
                return overlay;
            }
        }
        return null;
    }
}
