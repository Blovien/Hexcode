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
import com.hypixel.hytale.logger.HytaleLogger;

public class PedestalDataUtil {
    private static final HytaleLogger logger = HytaleLogger.forEnclosingClass();

    public static PedestalDataComponent getPedestalData(CommandBuffer<EntityStore> accessor,
            Ref<EntityStore> playerRef) {
        return getPedestalData(accessor, playerRef, false);
    }

    public static PedestalDataComponent getPedestalData(CommandBuffer<EntityStore> accessor,
            Ref<EntityStore> playerRef, boolean perPlayer) {

        PedestalDataComponent playerPedestalData = accessor.getComponent(playerRef,
                PedestalDataComponent.getComponentType());
        if (playerPedestalData != null) {
            return playerPedestalData;
        }

        if (perPlayer) {
            playerPedestalData = new PedestalDataComponent();
            accessor.addComponent(playerRef, PedestalDataComponent.getComponentType(), playerPedestalData);
            return playerPedestalData;
        }

        HexcasterCraftingComponent playerData = accessor.getComponent(playerRef,
                HexcasterCraftingComponent.getComponentType());
        if (playerData == null) {
            logger.atWarning().log("pedestal: getPedestalData — no HexcasterCraftingComponent on player");
            return null;
        }
        Ref<EntityStore> pedestalEntityRef = playerData.getPedestalEntityRef();
        if (pedestalEntityRef == null || !pedestalEntityRef.isValid()) {
            logger.atWarning().log("pedestal: getPedestalData — pedestalEntityRef null or invalid");
            return null;
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
