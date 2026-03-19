package com.riprod.hexcode.core.common.hexes.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.registry.GlyphRegistry;
import com.riprod.hexcode.core.common.glyphs.registry.HexValueRegistry;
import com.riprod.hexcode.core.common.glyphs.utils.GlyphType;
import com.riprod.hexcode.core.common.hexes.component.Hex;

public class HexUtils {

    public static void validate(Hex hex) {
        List<Glyph> glyphs = hex.getGlyphs();
        if (glyphs.isEmpty()) return;

        removeUnregisteredGlyphs(hex);
        cleanDanglingLinks(hex);
        cleanDanglingSlots(hex);
        removeOrphans(hex);
        fixFirstGlyphId(hex);
        repairAsymmetricLinks(hex);
    }
    
    public static void compress(Hex hex) {
        cleanDanglingLinks(hex);
        cleanDanglingSlots(hex);
        fixFirstGlyphId(hex);
        repairAsymmetricLinks(hex);
        rekeyGlyphs(hex);
    }

    private static void rekeyGlyphs(Hex hex) {
        List<Glyph> glyphs = hex.getGlyphs();
        if (glyphs.isEmpty()) return;

        Map<String, String> idMap = new HashMap<>();
        int counter = 0;
        for (Glyph glyph : glyphs) {
            idMap.put(glyph.getId(), String.valueOf(counter++));
        }

        String oldFirstId = hex.getFirstGlyphId();

        for (Glyph glyph : glyphs) {
            hex.remove(glyph.getId());
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

        if (oldFirstId != null) {
            String newFirstId = idMap.get(oldFirstId);
            hex.setFirstGlyphId(newFirstId);
        }
    }

    public static String serialize(Hex hex) {
        return HexSerializer.serialize(hex);
    }

    public static Hex deserialize(String data) {
        return HexSerializer.deserialize(data);
    }

    private static void removeUnregisteredGlyphs(Hex hex) {
        Set<String> toRemove = new HashSet<>();
        for (Glyph glyph : hex.getGlyphs()) {
            String glyphId = glyph.getGlyphId();
            if (glyph.getType() == GlyphType.Value) {
                if (HexValueRegistry.get(glyphId) == null) {
                    toRemove.add(glyph.getId());
                }
            } else {
                if (GlyphRegistry.get(glyphId) == null) {
                    toRemove.add(glyph.getId());
                }
            }
        }
        for (String id : toRemove) {
            hex.remove(id);
        }
    }

    private static void cleanDanglingLinks(Hex hex) {
        for (Glyph glyph : hex.getGlyphs()) {
            glyph.getNext().removeIf(id -> hex.get(id) == null);
            glyph.getPrevious().removeIf(id -> hex.get(id) == null);
        }
    }

    private static void cleanDanglingSlots(Hex hex) {
        for (Glyph glyph : hex.getGlyphs()) {
            glyph.getInputs().values().removeIf(id -> hex.get(id) == null);
            glyph.getOutputs().values().removeIf(id -> hex.get(id) == null);
        }
    }

    private static void removeOrphans(Hex hex) {
        boolean changed = true;
        while (changed) {
            changed = false;

            Set<String> referenced = new HashSet<>();
            for (Glyph glyph : hex.getGlyphs()) {
                referenced.addAll(glyph.getInputs().values());
                referenced.addAll(glyph.getOutputs().values());
            }

            String firstId = hex.getFirstGlyphId();
            Set<String> toRemove = new HashSet<>();

            for (Glyph glyph : hex.getGlyphs()) {
                String id = glyph.getId();
                if (id.equals(firstId)) continue;

                boolean hasLinks = !glyph.getNext().isEmpty() || !glyph.getPrevious().isEmpty();
                boolean isReferenced = referenced.contains(id);

                if (!hasLinks && !isReferenced) {
                    toRemove.add(id);
                }
            }

            if (!toRemove.isEmpty()) {
                for (String id : toRemove) {
                    hex.remove(id);
                }
                cleanDanglingLinks(hex);
                cleanDanglingSlots(hex);
                changed = true;
            }
        }
    }

    private static void fixFirstGlyphId(Hex hex) {
        String firstId = hex.getFirstGlyphId();
        if (firstId != null && hex.get(firstId) != null) return;

        List<Glyph> glyphs = hex.getGlyphs();
        if (glyphs.isEmpty()) {
            hex.setFirstGlyphId(null);
            return;
        }

        for (Glyph glyph : glyphs) {
            if (glyph.getPrevious().isEmpty()) {
                hex.setFirstGlyphId(glyph.getId());
                return;
            }
        }

        hex.setFirstGlyphId(glyphs.get(0).getId());
    }

    private static void repairAsymmetricLinks(Hex hex) {
        for (Glyph glyph : hex.getGlyphs()) {
            String id = glyph.getId();

            for (String nextId : glyph.getNext()) {
                Glyph target = hex.get(nextId);
                if (target != null && !target.getPrevious().contains(id)) {
                    target.addPrevious(id);
                }
            }

            for (String prevId : glyph.getPrevious()) {
                Glyph target = hex.get(prevId);
                if (target != null && !target.getNext().contains(id)) {
                    target.addNext(id);
                }
            }
        }
    }
}
