package com.riprod.hexcode.core.hexbook.component;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.glyphs.component.GlyphComponent;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class HexBookComponent implements Component<EntityStore> {

    public static final BuilderCodec<HexBookComponent> CODEC = BuilderCodec
            .builder(HexBookComponent.class, HexBookComponent::new)
            .append(new KeyedCodec<>("GlyphData", new ArrayCodec<>(GlyphComponent.CODEC, GlyphComponent[]::new)),
                    (c, v) -> {
                        if (v != null) {
                            c.glyphs = new ArrayList<>(Arrays.asList(v));
                        } else {
                            c.glyphs = new ArrayList<>();
                        }
                    },
                    c -> c.glyphs.toArray(GlyphComponent[]::new))
            .add()
            .append(new KeyedCodec<>("MaxCapacity", Codec.INTEGER),
                    (c, v) -> c.maxCapacity = v,
                    c -> c.maxCapacity)
            .add()
            .build();

    private static ComponentType<EntityStore, HexBookComponent> componentType;

    @Nonnull
    private List<GlyphComponent> glyphs = new ArrayList<>();
    @Nonnull
    private int maxCapacity = 10;

    public HexBookComponent() {
    }

    public HexBookComponent(int maxCapacity) {
        this.maxCapacity = maxCapacity;
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

    public boolean canAddGlyph() {
        return glyphs.size() < maxCapacity;
    }

    public void addGlyph(@Nonnull GlyphComponent glyph) {
        // todo: add glyph to book
        if (canAddGlyph()) {
            glyphs.add(glyph);
        }
    }

    public void removeGlyph(@Nonnull UUID id) {
        // todo: remove glyph from book by id
        glyphs.removeIf(g -> g.getId().equals(id));
    }

    public void removeGlyph(@Nonnull String id) { // removes ALL glyphs with a specific ID
        glyphs.removeIf(g -> g.getGlyphId().equals(id));
    }

    public List<GlyphComponent> getGlyphs() {
        return glyphs;
    }

    @Nonnull
    @Override
    public HexBookComponent clone() {
        HexBookComponent copy = new HexBookComponent();
        copy.glyphs = new ArrayList<>(this.glyphs);
        copy.maxCapacity = this.maxCapacity;
        return copy;
    }
}
