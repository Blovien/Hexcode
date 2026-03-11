package com.riprod.hexcode.core.state.crafting.utils;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.state.crafting.component.HexcasterCraftingComponent;
import com.riprod.hexcode.core.state.crafting.component.PedestalBlockComponent;
import com.riprod.hexcode.core.state.crafting.component.PedestalDataComponent;

public class PedestalDataUtil {
    public static PedestalDataComponent getPedestalData(CommandBuffer<EntityStore> accessor,
            Ref<EntityStore> playerRef) {

        PedestalDataComponent pedestalData = accessor.getComponent(playerRef, PedestalDataComponent.getComponentType());
        if (pedestalData != null) {
            return pedestalData; // per-player data
        }

        HexcasterCraftingComponent playerData = accessor.getComponent(playerRef,
                HexcasterCraftingComponent.getComponentType());
        if (playerData == null) {
            return null; // should not happen, player should always have crafting component
        }
        Ref<EntityStore> pedestalEntityRef = playerData.getPedestalEntityRef();
        if (pedestalEntityRef == null || !pedestalEntityRef.isValid()) {
            return null; // no valid pedestal ref, cannot get data
        }

        PedestalDataComponent anchorData = accessor.getComponent(pedestalEntityRef,
                PedestalDataComponent.getComponentType());

        if (anchorData == null) {
            anchorData = new PedestalDataComponent();
            accessor.addComponent(pedestalEntityRef, PedestalDataComponent.getComponentType(), anchorData);
        }
        return anchorData;
    }

    public static void dropContents(CommandBuffer<EntityStore> buffer, PedestalBlockComponent pedestal,
            PedestalDataComponent playerData, Vector3i pos) {

        // remove and drop book
        ItemStack bookStack = playerData.getStoredBook();
        if (bookStack != null && !bookStack.isEmpty()) {
            PedestalItemUtil.dropBookAtPosition(buffer, bookStack, pos);
        }

        Ref<EntityStore> bookRef = playerData.getBookDisplayRef();
        if (bookRef != null && bookRef.isValid()) {
            buffer.removeEntity(bookRef, RemoveReason.REMOVE);
            playerData.setBookDisplayRef(null);
        }

        // remove and drop essence
        String essenceId = playerData.getEssence();
        if (pedestal != null && !pedestal.isConsumeEssence() && essenceId != null) {
            PedestalItemUtil.dropEssenceAtPosition(buffer, essenceId, pos);
        }

        Ref<EntityStore> essenceRef = playerData.getEssenceDisplayRef();
        if (essenceRef != null && essenceRef.isValid()) {
            buffer.removeEntity(essenceRef, RemoveReason.REMOVE);
            playerData.setEssenceDisplayRef(null);
        }

    }
}
