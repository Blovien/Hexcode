package com.riprod.hexcode.interaction;

import javax.annotation.Nonnull;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInteraction;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.crafting.component.PedestalBlockState;
import com.riprod.hexcode.core.crafting.component.PedestalComponent;
import com.riprod.hexcode.core.crafting.component.PedestalState;
import com.riprod.hexcode.core.crafting.system.PedestalTickSystem;
import com.riprod.hexcode.core.crafting.utils.PedestalBlockUtil;
import com.riprod.hexcode.core.crafting.utils.PedestalItemUtil;
import com.riprod.hexcode.core.crafting.utils.PedestalSpawner;

public class PedestalInteraction extends SimpleInteraction {

    @Nonnull
    public static final BuilderCodec<PedestalInteraction> CODEC = BuilderCodec
            .builder(PedestalInteraction.class, PedestalInteraction::new, SimpleInteraction.CODEC)
            .build();

    public PedestalInteraction() {
    }

    @Override
    protected void tick0(boolean firstRun, float time, @Nonnull InteractionType type,
            @Nonnull InteractionContext ctx, @Nonnull CooldownHandler cooldown) {

        if (!firstRun) {
            ctx.getState().state = InteractionState.Finished;
            return;
        }

        CommandBuffer<EntityStore> buffer = ctx.getCommandBuffer();
        if (buffer == null) {
            ctx.getState().state = InteractionState.Failed;
            return;
        }

        Ref<EntityStore> playerRef = ctx.getEntity();
        if (playerRef == null || !playerRef.isValid()) {
            ctx.getState().state = InteractionState.Failed;
            return;
        }

        BlockPosition targetBlock = ctx.getTargetBlock();
        if (targetBlock == null) {
            ctx.getState().state = InteractionState.Failed;
            return;
        }

        Vector3i blockPos = new Vector3i(targetBlock.x, targetBlock.y, targetBlock.z);
        World world = buffer.getExternalData().getWorld();

        Player player = buffer.getComponent(playerRef, Player.getComponentType());
        if (player == null) {
            ctx.getState().state = InteractionState.Failed;
            return;
        }

        ItemStack itemInHand = player.getInventory().getItemInHand();
        Ref<EntityStore> existingAnchor = PedestalTickSystem.getAnchorAt(blockPos);

        if (PedestalItemUtil.isEssence(itemInHand)) {
            handleEssencePlacement(buffer, player, itemInHand, blockPos, existingAnchor, world);
            ctx.getState().state = InteractionState.Finished;
            super.tick0(firstRun, time, type, ctx, cooldown);
            return;
        }

        if (PedestalItemUtil.isHexBook(itemInHand) && existingAnchor == null) {
            handleActivation(buffer, player, itemInHand, blockPos, world);
            ctx.getState().state = InteractionState.Finished;
            super.tick0(firstRun, time, type, ctx, cooldown);
            return;
        }

        if (PedestalItemUtil.isEmptyHand(itemInHand) && existingAnchor != null && existingAnchor.isValid()) {
            handleDeactivation(buffer, player, existingAnchor, blockPos, world);
            ctx.getState().state = InteractionState.Finished;
            super.tick0(firstRun, time, type, ctx, cooldown);
            return;
        }

        ctx.getState().state = InteractionState.Failed;
    }

    private void handleEssencePlacement(CommandBuffer<EntityStore> buffer,
            Player player, ItemStack essenceItem, Vector3i blockPos,
            Ref<EntityStore> existingAnchor, World world) {

        String essenceItemId = essenceItem.getItem().getId();

        if (existingAnchor != null && existingAnchor.isValid()) {
            PedestalComponent pedestal = buffer.getComponent(existingAnchor,
                    PedestalComponent.getComponentType());
            if (pedestal == null) {
                return;
            }

            Ref<EntityStore> oldEssenceRef = pedestal.getEssenceDisplayRef();
            if (oldEssenceRef != null && oldEssenceRef.isValid()) {
                buffer.removeEntity(oldEssenceRef, RemoveReason.REMOVE);
            }

            Ref<EntityStore> newEssenceRef = PedestalSpawner.spawnEssenceDisplay(
                    buffer, existingAnchor, essenceItemId);
            pedestal.setEssenceDisplayRef(newEssenceRef);
            pedestal.setEssenceItemId(essenceItemId);
        } else {
            PedestalBlockState blockComp = BlockModule.getComponent(
                    PedestalBlockState.getComponentType(), world,
                    blockPos.x, blockPos.y, blockPos.z);
            if (blockComp != null) {
                blockComp.setEssenceItemId(essenceItemId);
            }
        }

        consumeOneFromHand(player);
    }

    private void handleActivation(CommandBuffer<EntityStore> buffer,
            Player player, ItemStack bookItem, Vector3i blockPos, World world) {

        String bookItemId = bookItem.getItem().getId();
        String essenceItemId = null;

        PedestalBlockState blockComp = BlockModule.getComponent(
                PedestalBlockState.getComponentType(), world,
                blockPos.x, blockPos.y, blockPos.z);
        if (blockComp != null) {
            essenceItemId = blockComp.getEssenceItemId();
        }

        PedestalSpawner.createAnchorEntity(buffer, blockPos, essenceItemId, bookItemId);
        PedestalBlockUtil.changeBlockState(world, blockPos, "Activating");

        short activeSlot = player.getInventory().getActiveHotbarSlot();
        player.getInventory().getHotbar().setItemStackForSlot(activeSlot, ItemStack.EMPTY);
    }

    private void handleDeactivation(CommandBuffer<EntityStore> buffer,
            Player player, Ref<EntityStore> anchorRef,
            Vector3i blockPos, World world) {

        PedestalComponent pedestal = buffer.getComponent(anchorRef,
                PedestalComponent.getComponentType());
        if (pedestal == null) {
            return;
        }

        String essenceItemId = pedestal.getEssenceItemId();
        if (essenceItemId != null) {
            PedestalBlockState blockComp = BlockModule.getComponent(
                    PedestalBlockState.getComponentType(), world,
                    blockPos.x, blockPos.y, blockPos.z);
            if (blockComp != null) {
                blockComp.setEssenceItemId(essenceItemId);
            }
        }

        String bookItemId = pedestal.getBookItemId();
        if (bookItemId != null) {
            ItemStack bookStack = new ItemStack(bookItemId, 1);
            short activeSlot = player.getInventory().getActiveHotbarSlot();
            player.getInventory().getHotbar().setItemStackForSlot(activeSlot, bookStack);
        }

        Ref<EntityStore> bookRef = pedestal.getBookDisplayRef();
        if (bookRef != null && bookRef.isValid()) {
            buffer.removeEntity(bookRef, RemoveReason.REMOVE);
            pedestal.setBookDisplayRef(null);
        }

        pedestal.setPedestalState(PedestalState.DEACTIVATING);
        pedestal.setTransitionTimer(pedestal.getDeactivatingDuration());
        PedestalBlockUtil.changeBlockState(world, blockPos, "Deactivating");
    }

    private void consumeOneFromHand(Player player) {
        ItemStack current = player.getInventory().getItemInHand();
        if (current == null || current.isEmpty()) {
            return;
        }

        short activeSlot = player.getInventory().getActiveHotbarSlot();
        if (current.getQuantity() <= 1) {
            player.getInventory().getHotbar().setItemStackForSlot(activeSlot, ItemStack.EMPTY);
        } else {
            player.getInventory().getHotbar().setItemStackForSlot(activeSlot,
                    current.withQuantity(current.getQuantity() - 1));
        }
    }

    @Override
    protected void simulateTick0(boolean firstRun, float time, @Nonnull InteractionType type,
            @Nonnull InteractionContext ctx, @Nonnull CooldownHandler cooldown) {
    }
}
