package com.riprod.hexcode.core.crafting.events;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.crafting.registry.PedestalBlockComponent;
import com.riprod.hexcode.core.crafting.spawners.PedestalSpawner;
import com.riprod.hexcode.core.crafting.system.PedestalSystem;
import com.riprod.hexcode.core.crafting.utils.PedestalItemUtil;

public class PedestalInteractionEvent {
    private static HytaleLogger logger = HytaleLogger.forEnclosingClass();

    public static void HandleInteraction(CommandBuffer<EntityStore> buffer, Ref<EntityStore> playerRef,
            BlockPosition targetBlock) {

        Vector3i blockPos = new Vector3i(targetBlock.x, targetBlock.y, targetBlock.z);
        World world = buffer.getExternalData().getWorld();

        Player player = buffer.getComponent(playerRef, Player.getComponentType());
        if (player == null) {
            return;
        }

        PedestalBlockComponent pedestalComponent = BlockModule.getComponent(
                PedestalBlockComponent.getComponentType(), world,
                blockPos.x, blockPos.y, blockPos.z);

        if (pedestalComponent == null) {
            logger.atInfo().log("Pedestal interaction failed: no PedestalBlockComponent at %s", blockPos);
            return;
        }

        if (pedestalComponent.getLocation() == null || !pedestalComponent.getLocation().equals(blockPos)) {
            pedestalComponent.setLocation(blockPos);
        }

        Ref<EntityStore> anchorRef = pedestalComponent.getAnchorRef();
        boolean anchorExisted = anchorRef != null && anchorRef.isValid();

        if (!anchorExisted) {
            logger.atInfo().log("pedestal: no anchor at %s, spawning", blockPos);
            anchorRef = PedestalSpawner.spawnAnchorEntity(buffer, blockPos);
            pedestalComponent.setAnchorEntityRef(anchorRef);
        }

        ItemStack itemInHand = player.getInventory().getItemInHand();

        if (PedestalItemUtil.isEssence(itemInHand) && pedestalComponent.getEssenceItemId() == null) {
            PedestalSystem.handleEssencePlacement(buffer, player, itemInHand, pedestalComponent, blockPos);
            PedestalSystem.handleReady(buffer, pedestalComponent, world);
            return;
        }

        if (PedestalItemUtil.isHexBook(itemInHand) && pedestalComponent.getStoredBook().isEmpty()) {
            PedestalSystem.handleBookPlacement(buffer, player, itemInHand, pedestalComponent, blockPos);
            PedestalSystem.handleReady(buffer, pedestalComponent, world);
            return;
        }

        if (PedestalItemUtil.isEmptyHand(itemInHand)) {
            PedestalSystem.toggleActivation(buffer, player, pedestalComponent, world);
            return;
        }
    }
}
