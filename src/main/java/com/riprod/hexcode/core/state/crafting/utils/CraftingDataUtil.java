package com.riprod.hexcode.core.state.crafting.utils;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.state.crafting.component.HexcasterCraftingComponent;
import com.riprod.hexcode.core.common.pedestal.component.PedestalBlockComponent;
import com.riprod.hexcode.core.state.crafting.component.CraftingData;
import com.hypixel.hytale.logger.HytaleLogger;

public class CraftingDataUtil {
    private static final HytaleLogger logger = HytaleLogger.forEnclosingClass();

    public static void dropContents(CommandBuffer<EntityStore> buffer, PedestalBlockComponent pedestal,
            CraftingData playerData, Vector3i pos) {

        // remove and drop book
        ItemStack bookStack = playerData.getStoredBook();
        if (bookStack != null && !bookStack.isEmpty() && pos != null) {
            PedestalItemUtil.dropBookAtPosition(buffer, bookStack, pos);
        }

        Ref<EntityStore> bookRef = playerData.getBookDisplayRef();
        if (bookRef != null && bookRef.isValid()) {
            buffer.removeEntity(bookRef, RemoveReason.REMOVE);
            playerData.setBookDisplayRef(null);
        }

        // remove and drop essence
        String essenceId = playerData.getEssence();
        if (pedestal != null && !pedestal.isConsumeEssence() && essenceId != null && pos != null) {
            PedestalItemUtil.dropEssenceAtPosition(buffer, essenceId, pos);
        }

        Ref<EntityStore> essenceRef = playerData.getEssenceDisplayRef();
        if (essenceRef != null && essenceRef.isValid()) {
            buffer.removeEntity(essenceRef, RemoveReason.REMOVE);
            playerData.setEssenceDisplayRef(null);
        }

    }
}
