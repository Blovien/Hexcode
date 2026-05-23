package com.riprod.hexcode.core.common.hud.controller;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.entity.entities.player.hud.HudManager;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;

public final class HudController {

    private static final String INFO_ROOT = "#HexcodeInfo";
    private static final String INFO_TITLE = "#HexcodeInfoTitle.TextSpans";
    private static final String INFO_DESCRIPTION = "#HexcodeInfoDescription.TextSpans";

    private HudController() {}

    public static void show(@Nonnull PlayerRef playerRef, @Nonnull Consumer<UICommandBuilder> build) {
        HexcodeHud hud = ensureHud(playerRef);
        UICommandBuilder cmd = new UICommandBuilder();
        build.accept(cmd);
        hud.apply(cmd);
    }

    public static void clear(@Nonnull PlayerRef playerRef) {
        playerRef.getComponent(Player.getComponentType()).getHudManager().removeCustomHud(playerRef, HexcodeHud.KEY);
    }

    public static void showInfo(@Nonnull CommandBuffer<EntityStore> buffer,
                                @Nonnull Ref<EntityStore> playerEntity,
                                @Nullable Message title,
                                @Nullable String description) {
        PlayerRef playerRef = buffer.getComponent(playerEntity, PlayerRef.getComponentType());
        if (playerRef == null) return;

        boolean hasTitle = title != null && !title.toString().isEmpty();
        boolean hasDescription = description != null && !description.isEmpty();
        if (!hasTitle && !hasDescription) {
            hideInfoIfActive(playerRef);
            return;
        }

        Message titleMsg = hasTitle ? title : Message.raw("");
        Message descMsg = Message.raw(hasDescription ? description : "");

        show(playerRef, cmd -> {
            cmd.set(INFO_ROOT + ".Visible", true);
            cmd.set(INFO_TITLE, titleMsg);
            cmd.set(INFO_DESCRIPTION, descMsg);
        });
    }

    public static void hideInfo(@Nonnull CommandBuffer<EntityStore> buffer,
                                @Nonnull Ref<EntityStore> playerEntity) {
        PlayerRef playerRef = buffer.getComponent(playerEntity, PlayerRef.getComponentType());
        if (playerRef == null) return;
        hideInfoIfActive(playerRef);
    }

    private static void hideInfoIfActive(@Nonnull PlayerRef playerRef) {
        HudManager manager = playerRef.getComponent(Player.getComponentType()).getHudManager();
        CustomUIHud existing = manager.getCustomHud(HexcodeHud.KEY);
        if (!(existing instanceof HexcodeHud hud)) return;
        UICommandBuilder cmd = new UICommandBuilder();
        cmd.set(INFO_ROOT + ".Visible", false);
        hud.apply(cmd);
    }

    @Nonnull
    private static HexcodeHud ensureHud(@Nonnull PlayerRef playerRef) {
        HudManager manager = playerRef.getComponent(Player.getComponentType()).getHudManager();
        CustomUIHud existing = manager.getCustomHud(HexcodeHud.KEY);
        if (existing instanceof HexcodeHud hud) return hud;
        HexcodeHud fresh = new HexcodeHud(playerRef);
        manager.addCustomHud(playerRef, fresh);
        return fresh;
    }
}
