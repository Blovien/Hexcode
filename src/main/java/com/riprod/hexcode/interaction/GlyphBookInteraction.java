package com.riprod.hexcode.interaction;

import javax.annotation.Nonnull;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.hexbook.GlyphBookPage;
import com.riprod.hexcode.core.common.hexbook.component.HexBookComponent;
import com.riprod.hexcode.core.common.hexcaster.utils.CasterInventory;
import com.riprod.hexcode.utils.HexSlot;
import io.sentry.util.Pair;

public class GlyphBookInteraction extends SimpleInteraction {

    @Nonnull
    public static final BuilderCodec<GlyphBookInteraction> CODEC = BuilderCodec
            .builder(GlyphBookInteraction.class, GlyphBookInteraction::new, SimpleInteraction.CODEC)
            .build();

    public GlyphBookInteraction() {
    }

    @Override
    protected void tick0(boolean firstRun, float time, @Nonnull InteractionType type,
                         @Nonnull InteractionContext ctx, @Nonnull CooldownHandler cooldown) {
        if (!firstRun) {
            ctx.getState().state = InteractionState.Finished;
            return;
        }

        Ref<EntityStore> playerRef = ctx.getEntity();
        if (playerRef == null || !playerRef.isValid()) {
            ctx.getState().state = InteractionState.Failed;
            return;
        }

        Store<EntityStore> store = playerRef.getStore();
        Player player = store.getComponent(playerRef, Player.getComponentType());
        if (player == null) {
            ctx.getState().state = InteractionState.Failed;
            return;
        }

        Pair<HexSlot, HexBookComponent> bookResult =
                CasterInventory.getHexBookComponent(store, playerRef, HexSlot.OffHand);
        if (bookResult == null) {
            ctx.getState().state = InteractionState.Failed;
            return;
        }

        PlayerRef pRef = player.getPlayerRef();
        GlyphBookPage page = new GlyphBookPage(pRef, bookResult.getFirst());
        player.getPageManager().openCustomPage(playerRef, store, page);

        ctx.getState().state = InteractionState.Finished;
    }

    @Override
    protected void simulateTick0(boolean firstRun, float time, @Nonnull InteractionType type,
                                 @Nonnull InteractionContext ctx, @Nonnull CooldownHandler cooldown) {
    }
}
