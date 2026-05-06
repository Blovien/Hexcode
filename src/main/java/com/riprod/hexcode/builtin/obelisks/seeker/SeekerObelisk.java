package com.riprod.hexcode.builtin.obelisks.seeker;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.hover.component.HoverableComponent;
import com.riprod.hexcode.core.common.obelisk.component.ObeliskBlockComponent;
import com.riprod.hexcode.core.common.obelisk.interfaces.ObeliskInterface;

public class SeekerObelisk implements ObeliskInterface {

    @Override
    public void onHover(CommandBuffer<EntityStore> buffer, Ref<EntityStore> playerRef,
            Ref<EntityStore> hoveredRef, ObeliskBlockComponent obelisk) {

        HoverableComponent hoverable = buffer.getComponent(hoveredRef, HoverableComponent.getComponentType());
        if (hoverable == null) return;

        String title = hoverable.getHintText("title");
        String description = hoverable.getHintText("description");
        if ((title == null || title.isEmpty()) && (description == null || description.isEmpty())) return;

        GlyphInfoHud hud = ensureOurHud(buffer, playerRef);
        if (hud == null) return;

        hud.showContent(title, description);
    }

    @Override
    public void onUnhover(CommandBuffer<EntityStore> buffer, Ref<EntityStore> playerRef,
            Ref<EntityStore> unhoveredRef, ObeliskBlockComponent obelisk) {
        clearOurHud(buffer, playerRef);
    }

    @Override
    public void onExitCrafting(CommandBuffer<EntityStore> buffer, Ref<EntityStore> playerRef,
            ObeliskBlockComponent obelisk) {
        clearOurHud(buffer, playerRef);
    }

    private GlyphInfoHud ensureOurHud(CommandBuffer<EntityStore> buffer, Ref<EntityStore> playerRef) {
        Player player = buffer.getComponent(playerRef, Player.getComponentType());
        if (player == null) return null;

        PlayerRef ref = buffer.getComponent(playerRef, PlayerRef.getComponentType());
        if (ref == null) return null;

        CustomUIHud current = player.getHudManager().getCustomHud();
        if (current instanceof GlyphInfoHud existing) {
            return existing;
        }
        if (current != null) {
            // foreign hud owns the slot; yield rather than clobber
            return null;
        }

        GlyphInfoHud fresh = new GlyphInfoHud(ref);
        player.getHudManager().setCustomHud(ref, fresh);
        return fresh;
    }

    private void clearOurHud(CommandBuffer<EntityStore> buffer, Ref<EntityStore> playerRef) {
        Player player = buffer.getComponent(playerRef, Player.getComponentType());
        if (player == null) return;

        CustomUIHud current = player.getHudManager().getCustomHud();
        if (current instanceof GlyphInfoHud ours) {
            ours.clearContent();
        }
    }
}
