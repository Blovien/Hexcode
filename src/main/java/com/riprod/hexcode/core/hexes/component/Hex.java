package com.riprod.hexcode.core.hexes.component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.riprod.hexcode.core.glyphs.component.Glyph;

public class Hex {

    private Map<String, Glyph> hexGraph;
    private String hexId;

    public Hex() {
        this.hexGraph = new HashMap<>();
        this.hexId = UUID.randomUUID().toString();
    }

    public Hex(Map<String, Glyph> hexGraph, String hexId) {
        this.hexGraph = hexGraph;
        this.hexId = hexId;
    }

    public Hex(Map<String, Glyph> hexGraph) {
        this.hexGraph = hexGraph;
        this.hexId = UUID.randomUUID().toString();
    }

    public Hex(Glyph ... glyphs) {
        this.hexGraph = new HashMap<>();
        for (Glyph glyph : glyphs) {
            this.hexGraph.put(glyph.getId(), glyph);
        }
        this.hexId = UUID.randomUUID().toString();
    }

    public void absorb(Hex other) {
        hexGraph.putAll(other.hexGraph);
    }

    public Glyph get(String id) {
        return hexGraph.get(id);
    }

    public void put(String id, Glyph glyph) {
        hexGraph.put(id, glyph);
    }

    public String get() {
        return hexId;
    }

    public void set(String hexId) {
        this.hexId = hexId;
    }

    public static final BuilderCodec<Hex> CODEC = BuilderCodec
            .builder(Hex.class, Hex::new)
            .append(new KeyedCodec<>("HexGraph", new MapCodec<>(Glyph.CODEC, HashMap::new)),
                    (c, v) -> c.hexGraph = v,
                    c -> c.hexGraph)
            .add()
            .append(new KeyedCodec<>("HexId", Codec.STRING),
                    (c, v) -> c.hexId = v,
                    c -> c.hexId)
            .add()
            .build();

    public Hex clone() {
        Hex newHex = new Hex();
        for (Map.Entry<String, Glyph> entry : hexGraph.entrySet()) {
            newHex.put(entry.getKey(), entry.getValue().clone());
        }
        newHex.set(this.hexId);
        return newHex;
    }
}
