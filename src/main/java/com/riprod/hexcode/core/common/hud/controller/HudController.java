package com.riprod.hexcode.core.common.hud.controller;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.NotificationUtil;
import com.riprod.hexcode.core.common.hud.adapter.HudAdapterFactory;
import com.riprod.hexcode.core.common.hud.api.HudAdapter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class HudController {

    private static final String INFO_ROOT = "#HexcodeInfo";
    private static final String INFO_TITLE = "#HexcodeInfoTitle.TextSpans";
    private static final String INFO_DESCRIPTION = "#HexcodeInfoDescription.TextSpans";

    private static HudController instance;

    public static synchronized HudController boot() {
        if (instance == null) {
            instance = new HudController(HudAdapterFactory.create());
        }
        return instance;
    }

    public static HudController get() {
        if (instance == null) {
            throw new IllegalStateException("HudController not booted");
        }
        return instance;
    }

    private final HudAdapter adapter;

    private HudController(@Nonnull HudAdapter adapter) {
        this.adapter = adapter;
    }

    public void showInfo(@Nonnull CommandBuffer<EntityStore> buffer,
                         @Nonnull Ref<EntityStore> playerEntity,
                         @Nullable Message title,
                         @Nullable String description) {
        Player player = buffer.getComponent(playerEntity, Player.getComponentType());
        if (player == null) return;
        PlayerRef playerRef = buffer.getComponent(playerEntity, PlayerRef.getComponentType());
        if (playerRef == null) return;

        boolean hasTitle = title != null && !title.toString().isEmpty();
        boolean hasDescription = description != null && !description.isEmpty();
        if (!hasTitle && !hasDescription) {
            hideInfoIfActive(player, playerRef);
            return;
        }

        HexcodeHud hud = ensureHud(player, playerRef);
        if (hud == null) {
            Message titleMsg = hasTitle ? title : Message.raw("");
            Message descMsg = hasDescription ? Message.raw(description) : null;
            NotificationUtil.sendNotification(playerRef.getPacketHandler(), titleMsg, descMsg);
            return;
        }

        UICommandBuilder cmd = new UICommandBuilder();
        cmd.set(INFO_ROOT + ".Visible", true);
        cmd.set(INFO_TITLE, hasTitle ? title : Message.raw(""));
        cmd.set(INFO_DESCRIPTION, Message.raw(hasDescription ? description : ""));
        hud.apply(cmd);
    }

    public void hideInfo(@Nonnull CommandBuffer<EntityStore> buffer,
                         @Nonnull Ref<EntityStore> playerEntity) {
        Player player = buffer.getComponent(playerEntity, Player.getComponentType());
        if (player == null) return;
        PlayerRef playerRef = buffer.getComponent(playerEntity, PlayerRef.getComponentType());
        if (playerRef == null) return;
        hideInfoIfActive(player, playerRef);
    }

    private void hideInfoIfActive(@Nonnull Player player, @Nonnull PlayerRef playerRef) {
        HexcodeHud hud = adapter.findOwnedHud(player, playerRef);
        if (hud == null) return;
        UICommandBuilder cmd = new UICommandBuilder();
        cmd.set(INFO_ROOT + ".Visible", false);
        hud.apply(cmd);
    }

    @Nullable
    private HexcodeHud ensureHud(@Nonnull Player player, @Nonnull PlayerRef playerRef) {
        HexcodeHud existing = adapter.findOwnedHud(player, playerRef);
        if (existing != null) return existing;
        if (!adapter.canInstall(player, playerRef)) return null;
        HexcodeHud fresh = new HexcodeHud(playerRef);
        adapter.setHud(player, playerRef, fresh);
        return fresh;
    }
}
