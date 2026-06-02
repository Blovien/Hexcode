package com.riprod.hexcode.core.common.hexbook;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.registry.GlyphAsset;
import com.riprod.hexcode.core.common.hexcaster.utils.CasterInventory;
import com.riprod.hexcode.core.common.hexes.component.Hex;
import com.riprod.hexcode.utils.HexSlot;

import javax.annotation.Nonnull;
import java.util.List;

public class GlyphBookPage extends InteractiveCustomUIPage<GlyphBookPage.BookEventData> {

    private final HexSlot bookSlot;

    public GlyphBookPage(@Nonnull PlayerRef playerRef, HexSlot bookSlot) {
        super(playerRef, CustomPageLifetime.CanDismiss, BookEventData.CODEC);
        this.bookSlot = bookSlot;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd,
                      @Nonnull UIEventBuilder evt, @Nonnull Store<EntityStore> store) {
        cmd.append("Hexcode/Pages/GlyphBookPage.ui");

        List<Hex> hexes = CasterInventory.getBookHexes(store, ref, bookSlot);

        if (hexes.isEmpty()) {
            cmd.set("#EmptyLabel.Visible", true);
        } else {
            cmd.set("#EmptyLabel.Visible", false);
            cmd.clear("#HexList");
            for (int i = 0; i < hexes.size(); i++) {
                cmd.append("#HexList", "Hexcode/Pages/GlyphBookSlot.ui");
                cmd.set("#HexList[" + i + "] #HexName.Text", resolveDefaultName(hexes.get(i)));
            }
        }

        evt.addEventBinding(CustomUIEventBindingType.Activating, "#CloseBtn",
                EventData.of("Action", "Close"));
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
                                @Nonnull BookEventData data) {
        close();
    }

    private static Message resolveDefaultName(Hex hex) {
        if (hex.getFirstGlyphId() == null) return Message.raw("Unnamed Hex");
        Glyph first = hex.get(hex.getFirstGlyphId());
        if (first == null) return Message.raw("Unnamed Hex");
        GlyphAsset asset = GlyphAsset.getAssetMap().getAsset(first.getGlyphId());
        if (asset != null && asset.getTitle() != null) return Message.translation(asset.getTitle());
        return Message.raw(first.getGlyphId());
    }

    public static class BookEventData {
        public static final BuilderCodec<BookEventData> CODEC = BuilderCodec
                .builder(BookEventData.class, BookEventData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING),
                        (d, v) -> d.action = v, d -> d.action)
                .add()
                .build();

        private String action;
    }
}
