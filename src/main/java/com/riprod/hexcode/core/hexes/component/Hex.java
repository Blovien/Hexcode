package com.riprod.hexcode.core.hexes.component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    private String firstGlyphId;

    public Hex() {
        this.hexGraph = new HashMap<>();
        this.hexId = UUID.randomUUID().toString();
    }

    public Hex(Map<String, Glyph> hexGraph, String hexId, String firstGlyphId) {
        this.hexGraph = hexGraph;
        this.hexId = hexId;
        this.firstGlyphId = firstGlyphId;
    }

    public Hex(Glyph ... glyphs) {
        this.hexGraph = new HashMap<>();
        for (Glyph glyph : glyphs) {
            this.hexGraph.put(glyph.getId(), glyph);
        }
        this.firstGlyphId = glyphs.length > 0 ? glyphs[0].getId() : null;
        this.hexId = UUID.randomUUID().toString();
    }

    public void absorb(Hex other, String insertLocation) {
        
        // insertLocation -> otherHexEntry
        other.hexGraph.get(other.getFirstGlyphId()).setPrevious(new ArrayList<>(List.of(insertLocation)));
        hexGraph.get(insertLocation).addNext(other.getFirstGlyphId());

        hexGraph.putAll(other.hexGraph);
    }

    public Glyph get(String id) {
        return hexGraph.get(id);
    }

    public List<Glyph> getGlyphs() {
        return new ArrayList<>(hexGraph.values());
    }

    public List<Glyph> getGlyphs(List<String> ids) {
        List<Glyph> glyphs = new ArrayList<>();
        for (String id : ids) {
            Glyph glyph = hexGraph.get(id);
            if (glyph != null) {
                glyphs.add(glyph);
            }
        }
        return glyphs;
    }

    public List<Glyph> getNextGlyphs(String id) {
        Glyph glyph = hexGraph.get(id);
        if (glyph == null) {
            return new ArrayList<>();
        }
        return getGlyphs(glyph.getNext());
    }

    public void put(String id, Glyph glyph) {
        hexGraph.put(id, glyph);
    }

    public String getHexId() {
        return hexId;
    }

    public String getFirstGlyphId() {
        return firstGlyphId;
    }

    public void setFirstGlyphId(String firstGlyphId) {
        this.firstGlyphId = firstGlyphId;
    }

    public void set(String hexId) {
        this.hexId = hexId;
    }

    public static final BuilderCodec<Hex> CODEC = BuilderCodec
            .builder(Hex.class, Hex::new)
            .append(new KeyedCodec<>("HexGraph", new MapCodec<>(Glyph.CODEC, HashMap::new, false)),
                    (c, v) -> c.hexGraph = v,
                    c -> c.hexGraph)
            .add()
            .append(new KeyedCodec<>("HexId", Codec.STRING),
                    (c, v) -> c.hexId = v,
                    c -> c.hexId)
            .add()
            .append(new KeyedCodec<>("FirstGlyphId", Codec.STRING),
                    (c, v) -> c.firstGlyphId = v,
                    c -> c.firstGlyphId)
            .add()
            .build();

    public Hex clone() {
        Hex newHex = new Hex();
        for (Map.Entry<String, Glyph> entry : hexGraph.entrySet()) {
            newHex.put(entry.getKey(), entry.getValue().clone());
        }
        newHex.setFirstGlyphId(this.firstGlyphId);
        newHex.set(this.hexId);
        return newHex;
    }
}
