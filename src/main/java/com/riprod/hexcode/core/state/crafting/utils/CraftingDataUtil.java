package com.riprod.hexcode.core.state.crafting.utils;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.pedestal.component.PedestalBlockComponent;
import com.riprod.hexcode.core.state.crafting.session.HexcodeSessionComponent;
import com.hypixel.hytale.logger.HytaleLogger;

public class CraftingDataUtil {
    private static final HytaleLogger logger = HytaleLogger.forEnclosingClass();

    public static void dropContents(CommandBuffer<EntityStore> buffer, PedestalBlockComponent pedestal,
            HexcodeSessionComponent session, Vector3i pos) {

        ItemStack itemStack = session.getStoredItem();
        if (itemStack != null && !itemStack.isEmpty() && pos != null) {
            PedestalItemUtil.dropBookAtPosition(buffer, itemStack, pos);
        }

        Ref<EntityStore> displayRef = session.getImbuedItemDisplayRef();
        if (displayRef != null && displayRef.isValid()) {
            buffer.removeEntity(displayRef, RemoveReason.REMOVE);
            session.setImbuedItemDisplayRef(null);
        }
    }
}
