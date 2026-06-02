package com.riprod.hexcode.core.common.hexbook.component;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.protocol.Color;
import com.hypixel.hytale.server.core.codec.ProtocolCodecs;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.hexes.codec.HexFieldCodec;
import com.riprod.hexcode.core.common.hexes.component.Hex;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HexBookComponent implements Component<EntityStore> {

    public static final BuilderCodec<HexBookComponent> CODEC = BuilderCodec
            .builder(HexBookComponent.class, HexBookComponent::new)
            .append(new KeyedCodec<>("Hexes", new ArrayCodec<>(HexFieldCodec.PLAYER, Hex[]::new)),
                    (c, v) -> {
                        if (v != null) {
                            c.hexes = new ArrayList<>(Arrays.asList(v));
                        } else {
                            c.hexes = new ArrayList<>();
                        }
                    },
                    c -> c.hexes.stream()
                            .filter(h -> h != null && !h.getGlyphs().isEmpty())
                            .toArray(Hex[]::new))
            .add()
            .append(new KeyedCodec<>("MaxCapacity", Codec.INTEGER),
                    (c, v) -> c.maxCapacity = v,
                    c -> c.maxCapacity)
            .add()
            .append(new KeyedCodec<>("BookId", Codec.STRING),
                    (c, v) -> c.bookId = v,
                    c -> c.bookId)
            .add()
            .append(new KeyedCodec<>("HexNames", new MapCodec<>(Codec.STRING, HashMap::new, false)),
                    (c, v) -> { if (v != null) c.hexNames = new HashMap<>(v); },
                    c -> c.hexNames)
            .add()
            .append(new KeyedCodec<>("HexColors", new MapCodec<>(ProtocolCodecs.COLOR, HashMap::new, false)),
                    (c, v) -> { if (v != null) c.hexColors = new HashMap<>(v); },
                    c -> c.hexColors)
            .add()
            .build();

    private static ComponentType<EntityStore, HexBookComponent> componentType;

    @Nonnull
    private List<Hex> hexes = new ArrayList<>();
    private int maxCapacity = 10;
    @Nonnull
    private String bookId = "";
    @Nonnull
    private Map<String, String> hexNames = new HashMap<>();
    @Nonnull
    private Map<String, Color> hexColors = new HashMap<>();

    public HexBookComponent() {
    }

    public HexBookComponent(HexBookAsset hexbook) {
        this.maxCapacity = hexbook.getMaxGlyphs();
        this.bookId = hexbook.getId();
    }

    public static void setComponentType(ComponentType<EntityStore, HexBookComponent> type) {
        componentType = type;
    }

    public static ComponentType<EntityStore, HexBookComponent> getComponentType() {
        return componentType;
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(int maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    public boolean canAddHex() {
        return hexes.size() < maxCapacity;
    }

    public void addHex(@Nonnull Hex hex) {
        if (canAddHex()) {
            hexes.add(hex);
        }
    }

    public void setHex(int index, @Nonnull Hex hex) {
        if (index >= 0 && index < hexes.size()) {
            hexes.set(index, hex);
        } else if (index == hexes.size() && canAddHex()) {
            hexes.add(hex);
        }
    }

    public boolean removeHex(@Nonnull String id) {
        return hexes.removeIf(g -> g.getHexId().equals(id));
    }

    public boolean removeGlyph(@Nonnull String id) {
        return hexes.removeIf(g -> g.getGlyphs().stream().anyMatch(glyph -> glyph.getGlyphId().equals(id)));
    }

    public List<Hex> getHexes() {
        return hexes;
    }

    @Nullable
    public String getHexName(String hexId) {
        return hexNames.get(hexId);
    }

    public void setHexName(String hexId, String name) {
        hexNames.put(hexId, name);
    }

    @Nullable
    public Color getHexColor(String hexId) {
        return hexColors.get(hexId);
    }

    public void setHexColor(String hexId, Color color) {
        hexColors.put(hexId, color);
    }

    @Nonnull
    @Override
    public HexBookComponent clone() {
        HexBookComponent copy = new HexBookComponent();
        copy.hexes = new ArrayList<>(this.hexes);
        copy.maxCapacity = this.maxCapacity;
        copy.bookId = this.bookId;
        copy.hexNames = new HashMap<>(this.hexNames);
        copy.hexColors = new HashMap<>(this.hexColors);
        return copy;
    }
}
