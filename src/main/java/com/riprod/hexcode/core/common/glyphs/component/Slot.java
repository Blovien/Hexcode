package com.riprod.hexcode.core.common.glyphs.component;

import java.util.ArrayList;
import java.util.Arrays;

import javax.annotation.Nullable;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.DebugShape;
import com.riprod.hexcode.core.common.glyphs.registry.SlotAsset;

public class Slot {
    private String[] links = new String[0];

    private transient String key;
    private transient String label;
    private transient String description;
    private transient Vector3f color;
    private transient Vector3f offset;
    private transient DebugShape shape;
    @Nullable
    private transient String defaultDisplay;

    public Slot() {
    }

    public String[] getLinks() {
        return this.links;
    }

    public void setLinks(String[] links) {
        this.links = links != null ? links : new String[0];
    }

    public void addLink(String glyphId) {
        if (glyphId == null) return;
        for (String existing : this.links) {
            if (existing.equals(glyphId)) return;
        }
        String[] grown = Arrays.copyOf(this.links, this.links.length + 1);
        grown[this.links.length] = glyphId;
        this.links = grown;
    }

    public void removeLink(String glyphId) {
        if (glyphId == null || this.links.length == 0) return;
        ArrayList<String> kept = new ArrayList<>(this.links.length);
        for (String existing : this.links) {
            if (!existing.equals(glyphId)) kept.add(existing);
        }
        this.links = kept.toArray(String[]::new);
    }

    public void clearLinks() {
        this.links = new String[0];
    }

    @Nullable
    public String getFirstLink() {
        return this.links.length > 0 ? this.links[0] : null;
    }

    public void hydrateFrom(SlotAsset asset, String key, Vector3f resolvedOffset) {
        this.key = key;
        this.label = asset.getLabel();
        this.description = asset.getDescription();
        this.color = asset.getColor();
        this.offset = resolvedOffset;
        this.shape = asset.getShape();
        this.defaultDisplay = asset.getDefaultDisplay();
    }

    public String getKey() {
        return this.key;
    }

    public String getLabel() {
        return this.label;
    }

    public String getDescription() {
        return this.description;
    }

    public Vector3f getColor() {
        return this.color;
    }

    public Vector3f getOffset() {
        return this.offset;
    }

    public DebugShape getShape() {
        return this.shape;
    }

    @Nullable
    public String getDefaultDisplay() {
        return this.defaultDisplay;
    }

    public static final BuilderCodec<Slot> CODEC = BuilderCodec.builder(Slot.class, Slot::new)
            .append(new KeyedCodec<>("Links", Codec.STRING_ARRAY),
                    (s, v) -> s.links = v != null ? v : new String[0],
                    s -> s.links)
            .add()
            .build();

    @Override
    public Slot clone() {
        Slot copy = new Slot();
        copy.links = Arrays.copyOf(this.links, this.links.length);
        copy.key = this.key;
        copy.label = this.label;
        copy.description = this.description;
        copy.color = this.color;
        copy.offset = this.offset;
        copy.shape = this.shape;
        copy.defaultDisplay = this.defaultDisplay;
        return copy;
    }

    @Override
    public String toString() {
        return this.key != null ? this.key : "Slot";
    }
}
