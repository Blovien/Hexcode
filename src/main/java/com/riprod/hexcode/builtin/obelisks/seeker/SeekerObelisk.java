package com.riprod.hexcode.builtin.obelisks.seeker;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.hover.component.HoverableComponent;
import com.riprod.hexcode.core.common.obelisk.component.ObeliskBlockComponent;
import com.riprod.hexcode.core.common.obelisk.interfaces.ObeliskInterface;

public class SeekerObelisk implements ObeliskInterface {

    public void onHover(CommandBuffer<EntityStore> buffer, Ref<EntityStore> playerRef,
            Ref<EntityStore> hoveredRef, ObeliskBlockComponent obelisk) {

        HoverableComponent hoverable = buffer.getComponent(hoveredRef, HoverableComponent.getComponentType());
        if (hoverable == null) return;

        String hintText = hoverable.getHintText();
        if (hintText == null || hintText.isEmpty()) return;

        PlayerRef ref = buffer.getComponent(playerRef, PlayerRef.getComponentType());
        if (ref == null) return;

        Player player = buffer.getComponent(playerRef, Player.getComponentType());
        if (player == null) return;

        player.getHudManager().setCustomHud(ref, new GlyphInfoHud(ref, hintText));
    }

    public void onUnhover(CommandBuffer<EntityStore> buffer, Ref<EntityStore> playerRef,
            Ref<EntityStore> unhoveredRef, ObeliskBlockComponent obelisk) {

        PlayerRef ref = buffer.getComponent(playerRef, PlayerRef.getComponentType());
        if (ref == null) return;

        Player player = buffer.getComponent(playerRef, Player.getComponentType());
        if (player == null) return;

        player.getHudManager().setCustomHud(ref, null);
    }
}
