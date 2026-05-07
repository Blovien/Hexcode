package com.riprod.hexcode.core.common.hud.controller;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.hud.adapter.HudAdapterFactory;
import com.riprod.hexcode.core.common.hud.api.HudAdapter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
    private final Map<UUID, HexcodeHud> activeHuds = new ConcurrentHashMap<>();

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
        if (hud == null) return;

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

    public void tearDown(@Nonnull Player player, @Nonnull PlayerRef playerRef) {
        UUID uuid = playerRef.getUuid();
        HexcodeHud ours = activeHuds.remove(uuid);
        if (ours != null && adapter.getCurrentHud(player, playerRef) == ours) {
            adapter.clearHud(player, playerRef);
        }
    }

    private void hideInfoIfActive(@Nonnull Player player, @Nonnull PlayerRef playerRef) {
        HexcodeHud hud = activeHuds.get(playerRef.getUuid());
        if (hud == null || adapter.getCurrentHud(player, playerRef) != hud) return;
        UICommandBuilder cmd = new UICommandBuilder();
        cmd.set(INFO_ROOT + ".Visible", false);
        hud.apply(cmd);
    }

    @Nullable
    private HexcodeHud ensureHud(@Nonnull Player player, @Nonnull PlayerRef playerRef) {
        UUID uuid = playerRef.getUuid();
        HexcodeHud existing = activeHuds.get(uuid);
        CustomUIHud current = adapter.getCurrentHud(player, playerRef);
        if (existing != null && current == existing) {
            return existing;
        }

        if (current != null && current != existing) {
            return null;
        }

        HexcodeHud fresh = new HexcodeHud(playerRef);
        adapter.setHud(player, playerRef, fresh);
        activeHuds.put(uuid, fresh);
        return fresh;
    }
}
