package com.riprod.hexcode.core.common.hexbook;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.Color;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.registry.GlyphAsset;
import com.riprod.hexcode.core.common.hexbook.component.HexBookComponent;
import com.riprod.hexcode.core.common.hexcaster.utils.CasterInventory;
import com.riprod.hexcode.core.common.hexes.component.Hex;
import com.riprod.hexcode.utils.HexSlot;

import javax.annotation.Nonnull;
import java.util.List;

public class GlyphBookPage extends InteractiveCustomUIPage<GlyphBookPage.BookEventData> {

    private static final Color[] PALETTE = {
            new Color((byte) 0xFF, (byte) 0x44, (byte) 0x00),
            new Color((byte) 0xFF, (byte) 0xAA, (byte) 0x00),
            new Color((byte) 0xFF, (byte) 0xFF, (byte) 0x00),
            new Color((byte) 0x44, (byte) 0xFF, (byte) 0x44),
            new Color((byte) 0x00, (byte) 0xCC, (byte) 0xFF),
            new Color((byte) 0x44, (byte) 0x44, (byte) 0xFF),
            new Color((byte) 0xAA, (byte) 0x44, (byte) 0xFF),
            new Color((byte) 0xFF, (byte) 0x44, (byte) 0xAA),
            new Color((byte) 0xFF, (byte) 0xFF, (byte) 0xFF),
            new Color((byte) 0xAA, (byte) 0xAA, (byte) 0xAA),
    };

    private final HexSlot bookSlot;
    private int selectedIndex = -1;
    private String selectedHexId;

    public GlyphBookPage(@Nonnull PlayerRef playerRef, HexSlot bookSlot) {
        super(playerRef, CustomPageLifetime.CanDismiss, BookEventData.CODEC);
        this.bookSlot = bookSlot;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd,
                      @Nonnull UIEventBuilder evt, @Nonnull Store<EntityStore> store) {
        cmd.append("Hexcode/Pages/GlyphBookPage.ui");

        io.sentry.util.Pair<HexSlot, HexBookComponent> bookResult =
                CasterInventory.getHexBookComponent(store, ref, bookSlot);
        HexBookComponent book = bookResult != null ? bookResult.getSecond() : null;
        if (book == null) {
            cmd.set("#EmptyLabel.Visible", true);
            bindClose(evt);
            return;
        }

        List<Hex> hexes = book.getHexes();

        if (hexes.isEmpty()) {
            cmd.set("#EmptyLabel.Visible", true);
        } else {
            cmd.set("#EmptyLabel.Visible", false);
            cmd.clear("#HexList");

            for (int i = 0; i < hexes.size(); i++) {
                Hex hex = hexes.get(i);
                String hexId = hex.getHexId();
                String name = resolveDefaultName(hex);

                Color color = book.getHexColor(hexId);
                String colorHex = color != null ? toHexString(color) : "#c4b5ff";

                cmd.append("#HexList", "Hexcode/Pages/GlyphBookSlot.ui");
                cmd.set("#HexList[" + i + "] #HexName.Text", name);
                cmd.set("#HexList[" + i + "] #ColorSwatch.Background", colorHex);

                if (selectedIndex == i) {
                    cmd.set("#HexList[" + i + "].Background", "#1a1a2e(0.9)");
                }

                evt.addEventBinding(CustomUIEventBindingType.Activating, "#HexList[" + i + "] #ColorBtn",
                        EventData.of("Action", "ShowColors").append("Index", String.valueOf(i)));
            }
        }

        if (selectedIndex >= 0 && selectedIndex < hexes.size()) {
            Hex selectedHex = hexes.get(selectedIndex);
            selectedHexId = selectedHex.getHexId();

            cmd.set("#ColorPalette.Visible", true);
            cmd.clear("#ColorRow");
            for (int c = 0; c < PALETTE.length; c++) {
                String palHex = toHexString(PALETTE[c]);
                cmd.append("#ColorRow", "Hexcode/Pages/GlyphBookColorSwatch.ui");
                cmd.set("#ColorRow[" + c + "].Style.Default.Background", palHex);
                cmd.set("#ColorRow[" + c + "].Style.Hovered.Background", palHex);
                cmd.set("#ColorRow[" + c + "].Style.Pressed.Background", palHex);
                evt.addEventBinding(CustomUIEventBindingType.Activating, "#ColorRow[" + c + "]",
                        EventData.of("Action", "SetColor").append("ColorIndex", String.valueOf(c)));
            }
        } else {
            cmd.set("#ColorPalette.Visible", false);
        }

        bindClose(evt);
    }

    private void bindClose(@Nonnull UIEventBuilder evt) {
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#CloseBtn",
                EventData.of("Action", "Close"));
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
                                @Nonnull BookEventData data) {
        String action = data.action == null ? "" : data.action;

        switch (action) {
            case "ShowColors" -> {
                int index = parseIndex(data.index);
                if (index < 0) break;
                selectedIndex = index;
                rebuild();
            }
            case "SetColor" -> {
                int colorIndex = parseIndex(data.colorIndex);
                if (colorIndex < 0 || colorIndex >= PALETTE.length || selectedHexId == null) break;
                Color chosen = PALETTE[colorIndex].clone();
                CasterInventory.withHexBook(store, ref, bookSlot,
                        book -> book.setHexColor(selectedHexId, chosen));
                rebuild();
            }
            default -> close();
        }
    }

    private static String resolveDefaultName(Hex hex) {
        if (hex.getFirstGlyphId() == null) return "Unnamed Hex";
        Glyph first = hex.get(hex.getFirstGlyphId());
        if (first == null) return "Unnamed Hex";
        GlyphAsset asset = GlyphAsset.getAssetMap().getAsset(first.getGlyphId());
        if (asset != null && asset.getTitle() != null) return asset.getTitle();
        return first.getGlyphId();
    }

    private static String toHexString(Color color) {
        return String.format("#%02x%02x%02x", color.red & 0xFF, color.green & 0xFF, color.blue & 0xFF);
    }

    private static int parseIndex(String value) {
        if (value == null) return -1;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static class BookEventData {
        public static final BuilderCodec<BookEventData> CODEC = BuilderCodec
                .builder(BookEventData.class, BookEventData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING),
                        (d, v) -> d.action = v, d -> d.action)
                .add()
                .append(new KeyedCodec<>("Index", Codec.STRING),
                        (d, v) -> d.index = v, d -> d.index)
                .add()
                .append(new KeyedCodec<>("ColorIndex", Codec.STRING),
                        (d, v) -> d.colorIndex = v, d -> d.colorIndex)
                .add()
                .build();

        private String action;
        private String index;
        private String colorIndex;
    }
}
