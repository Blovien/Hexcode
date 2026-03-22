package com.riprod.hexcode.core.common.hexes.component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;

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
        int offset = this.hexGraph.size();
        rekeyIncoming(other, offset);

        other.hexGraph.get(other.getFirstGlyphId()).setPrevious(new ArrayList<>(List.of(insertLocation)));
        hexGraph.get(insertLocation).addNext(other.getFirstGlyphId());

        hexGraph.putAll(other.hexGraph);
    }

    private static void rekeyIncoming(Hex hex, int offset) {
        List<Glyph> glyphs = hex.getGlyphs();
        if (glyphs.isEmpty()) return;

        Map<String, String> idMap = new HashMap<>();
        for (Glyph glyph : glyphs) {
            idMap.put(glyph.getId(), String.valueOf(offset++));
        }

        for (Glyph glyph : glyphs) {
            hex.remove(glyph.getId());
        }

        for (Glyph glyph : glyphs) {
            glyph.setId(idMap.get(glyph.getId()));

            List<String> newNext = new ArrayList<>();
            for (String id : glyph.getNext()) {
                String mapped = idMap.get(id);
                if (mapped != null) newNext.add(mapped);
            }
            glyph.setNext(newNext);

            List<String> newPrev = new ArrayList<>();
            for (String id : glyph.getPrevious()) {
                String mapped = idMap.get(id);
                if (mapped != null) newPrev.add(mapped);
            }
            glyph.setPrevious(newPrev);

            Map<String, String> newInputs = new HashMap<>();
            for (Map.Entry<String, String> entry : glyph.getInputs().entrySet()) {
                String mapped = idMap.get(entry.getValue());
                if (mapped != null) newInputs.put(entry.getKey(), mapped);
            }
            glyph.getInputs().clear();
            glyph.getInputs().putAll(newInputs);

            Map<String, String> newOutputs = new HashMap<>();
            for (Map.Entry<String, String> entry : glyph.getOutputs().entrySet()) {
                String mapped = idMap.get(entry.getValue());
                if (mapped != null) newOutputs.put(entry.getKey(), mapped);
            }
            glyph.getOutputs().clear();
            glyph.getOutputs().putAll(newOutputs);

            hex.put(glyph.getId(), glyph);
        }

        String oldFirstId = hex.getFirstGlyphId();
        if (oldFirstId != null) {
            hex.setFirstGlyphId(idMap.get(oldFirstId));
        }
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

    public void remove(String id) {
        hexGraph.remove(id);
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

    @Override
    public String toString() {
        return "Hex{id=" + hexId + ", firstGlyphId=" + firstGlyphId + ", glyphs=" + hexGraph.values() + "}";
    }
}
