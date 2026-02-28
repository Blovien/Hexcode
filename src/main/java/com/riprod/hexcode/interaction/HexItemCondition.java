package com.riprod.hexcode.interaction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.utils.HexStaffUtil;

public class HexItemCondition extends SimpleInteraction {

    public enum CheckMode {
        MainHand,
        OffHand,
        Both
    }

    public enum HexItemType {
        HexStaff,
        HexBook
    }

    @Nonnull
    public static final BuilderCodec<HexItemCondition> CODEC = BuilderCodec
            .builder(HexItemCondition.class, HexItemCondition::new, SimpleInteraction.CODEC)
            .<CheckMode>appendInherited(
                    new KeyedCodec<>("CheckMode", new EnumCodec<>(CheckMode.class)),
                    (i, v) -> i.checkMode = v,
                    i -> i.checkMode,
                    (i, p) -> i.checkMode = p.checkMode)
            .add()
            .<HexItemType>appendInherited(
                    new KeyedCodec<>("MainHandItem", new EnumCodec<>(HexItemType.class)),
                    (i, v) -> i.mainHandItem = v,
                    i -> i.mainHandItem,
                    (i, p) -> i.mainHandItem = p.mainHandItem)
            .add()
            .<HexItemType>appendInherited(
                    new KeyedCodec<>("OffHandItem", new EnumCodec<>(HexItemType.class)),
                    (i, v) -> i.offHandItem = v,
                    i -> i.offHandItem,
                    (i, p) -> i.offHandItem = p.offHandItem)
            .add()
            .build();

    @Nullable
    private CheckMode checkMode;

    @Nullable
    private HexItemType mainHandItem;

    @Nullable
    private HexItemType offHandItem;

    public HexItemCondition() {
    }

    @Override
    protected void tick0(boolean firstRun, float time, @Nonnull InteractionType type,
            @Nonnull InteractionContext ctx, @Nonnull CooldownHandler cooldown) {

        CommandBuffer<EntityStore> buffer = ctx.getCommandBuffer();
        if (buffer == null) {
            ctx.getState().state = InteractionState.Failed;
            super.tick0(firstRun, time, type, ctx, cooldown);
            return;
        }

        Ref<EntityStore> playerRef = ctx.getEntity();
        if (playerRef == null || !playerRef.isValid()) {
            ctx.getState().state = InteractionState.Failed;
            super.tick0(firstRun, time, type, ctx, cooldown);
            return;
        }

        Player player = buffer.getComponent(playerRef, Player.getComponentType());
        if (player == null) {
            ctx.getState().state = InteractionState.Failed;
            super.tick0(firstRun, time, type, ctx, cooldown);
            return;
        }

        boolean success = checkHexItems(player);
        ctx.getState().state = success ? InteractionState.Finished : InteractionState.Failed;
        super.tick0(firstRun, time, type, ctx, cooldown);
    }

    private boolean checkHexItems(Player player) {
        CheckMode mode = checkMode != null ? checkMode : CheckMode.Both;

        switch (mode) {
            case MainHand:
                return checkSlot(player.getInventory().getItemInHand(), mainHandItem);
            case OffHand:
                return checkSlot(player.getInventory().getUtilityItem(), offHandItem);
            case Both:
                return checkSlot(player.getInventory().getItemInHand(), mainHandItem)
                        && checkSlot(player.getInventory().getUtilityItem(), offHandItem);
            default:
                return false;
        }
    }

    private boolean checkSlot(ItemStack itemStack, HexItemType expectedType) {
        if (expectedType == null) {
            return true;
        }

        if (itemStack == null || itemStack.isEmpty()) {
            return false;
        }

        switch (expectedType) {
            case HexStaff:
                return HexStaffUtil.isHexStaff(itemStack);
            case HexBook:
                return HexStaffUtil.isHexBook(itemStack);
            default:
                return false;
        }
    }

    @Override
    protected void simulateTick0(boolean firstRun, float time, @Nonnull InteractionType type,
            @Nonnull InteractionContext ctx, @Nonnull CooldownHandler cooldown) {
    }
}
