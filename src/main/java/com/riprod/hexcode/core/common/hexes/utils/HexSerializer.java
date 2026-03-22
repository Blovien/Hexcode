package com.riprod.hexcode.core.common.hexes.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import javax.annotation.Nullable;

import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.registry.GlyphAsset;
import com.riprod.hexcode.core.common.glyphs.utils.GlyphType;
import com.riprod.hexcode.core.common.hexes.component.Hex;

public class HexSerializer {

    private static final String PREFIX = "hexcode:";
    private static final int FORMAT_VERSION = 2;

    private static final int SLOT_EMPTY = 0x00;
    private static final int INLINE_NUMBER_BASE = 0x01;
    private static final int INLINE_VARIABLE = 0x11;
    private static final int INLINE_POSITION = 0x12;
    private static final int INLINE_ROTATION = 0x13;
    private static final int REF_FLAG = 0x80;

    private static final String STR_PREFIX_GLYPH = "Glyph_";
    private static final String STR_PREFIX_NUMBER = "Number_";
    private static final int PFX_NONE = 0;
    private static final int PFX_GLYPH = 1;
    private static final int PFX_NUMBER = 2;

    public static String serialize(Hex hex) {
        Hex clone = hex.clone();
        HexUtils.validate(clone);
        HexUtils.compress(clone);

        byte[] raw = encode(clone);
        if (raw == null) return null;

        byte[] compressed = deflate(raw);
        byte[] payload = (compressed != null && compressed.length < raw.length) ? compressed : raw;

        String encoded = Base64.getEncoder().withoutPadding().encodeToString(payload);
        String checksum = computeChecksum(encoded);
        return PREFIX + checksum + ":" + encoded;
    }

    @Nullable
    public static Hex deserialize(String data) {
        if (data == null || !data.startsWith(PREFIX)) return null;

        String remainder = data.substring(PREFIX.length());
        int sep = remainder.indexOf(':');
        if (sep < 0) return null;

        String checksum = remainder.substring(0, sep);
        String encoded = remainder.substring(sep + 1);
        if (!checksum.equals(computeChecksum(encoded))) return null;

        byte[] payload;
        try {
            payload = Base64.getDecoder().decode(encoded);
        } catch (IllegalArgumentException e) {
            return null;
        }

        byte[] raw = inflate(payload);
        if (raw == null) raw = payload;

        try {
            return decode(raw);
        } catch (Exception e) {
            return null;
        }
    }

    // --- encode ---

    @Nullable
    private static byte[] encode(Hex hex) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(baos)) {

            List<Glyph> allGlyphs = hex.getGlyphs();
            if (allGlyphs.isEmpty()) return null;

            List<Glyph> effects = new ArrayList<>();
            Map<String, Glyph> values = new LinkedHashMap<>();
            Set<String> inlinable = new HashSet<>();

            for (Glyph g : allGlyphs) {
                if (g.getType() == GlyphType.Value) {
                    values.put(g.getId(), g);
                } else {
                    effects.add(g);
                }
            }
            for (Glyph v : values.values()) {
                if (getInlineCode(v.getGlyphId()) >= 0) inlinable.add(v.getId());
            }

            List<Glyph> ordered = topoSort(hex, effects);
            Map<String, Integer> eIdx = new HashMap<>();
            for (int i = 0; i < ordered.size(); i++) eIdx.put(ordered.get(i).getId(), i);

            List<Glyph> extValues = new ArrayList<>();
            for (Glyph v : values.values()) {
                if (!inlinable.contains(v.getId())) extValues.add(v);
            }
            Map<String, Integer> vIdx = new HashMap<>();
            for (int i = 0; i < extValues.size(); i++) {
                vIdx.put(extValues.get(i).getId(), ordered.size() + i);
            }

            LinkedHashSet<String> assetIds = new LinkedHashSet<>();
            for (Glyph g : ordered) assetIds.add(g.getGlyphId());
            for (Glyph g : extValues) assetIds.add(g.getGlyphId());
            List<String> palette = new ArrayList<>(assetIds);
            Map<String, Integer> palIdx = new HashMap<>();
            for (int i = 0; i < palette.size(); i++) palIdx.put(palette.get(i), i);

            String firstId = hex.getFirstGlyphId();
            int firstGlyphIdx = (firstId != null && eIdx.containsKey(firstId)) ? eIdx.get(firstId) : 0;

            out.writeByte(FORMAT_VERSION);
            out.writeByte(ordered.size());
            out.writeByte(extValues.size());
            out.writeByte(firstGlyphIdx);

            out.writeByte(palette.size());
            for (String id : palette) writePrefixed(out, id);

            for (int i = 0; i < ordered.size(); i++) {
                Glyph g = ordered.get(i);
                out.writeByte(palIdx.get(g.getGlyphId()));

                int vol = Math.round(clamp01(g.getVolatility()) * 15f);
                int eff = Math.round(clamp01(g.getEfficiency()) * 15f);
                out.writeByte((vol << 4) | eff);

                writeNextLinks(out, g, i, eIdx);
                writeSlots(out, g.getInputs(), g.getGlyphId(), true, eIdx, vIdx, inlinable, values);
                writeSlots(out, g.getOutputs(), g.getGlyphId(), false, eIdx, vIdx, inlinable, values);
            }

            for (Glyph v : extValues) {
                out.writeByte(palIdx.get(v.getGlyphId()));
                writeSlots(out, v.getInputs(), v.getGlyphId(), true, eIdx, vIdx, inlinable, values);
            }

            out.flush();
            return baos.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    private static void writeNextLinks(DataOutputStream out, Glyph g, int myIdx,
            Map<String, Integer> eIdx) throws Exception {
        List<String> next = g.getNext();
        boolean isEnd = next.isEmpty();
        boolean isImplicit = next.size() == 1
                && eIdx.containsKey(next.get(0))
                && eIdx.get(next.get(0)) == myIdx + 1;

        if (isEnd) {
            out.writeByte(0);
        } else if (isImplicit) {
            out.writeByte(1);
        } else {
            out.writeByte(next.size() + 1);
            for (String nid : next) {
                Integer idx = eIdx.get(nid);
                out.writeByte(idx != null ? idx : 0);
            }
        }
    }

    private static void writeSlots(DataOutputStream out, Map<String, String> slots,
            String glyphAssetId, boolean isInput,
            Map<String, Integer> eIdx, Map<String, Integer> vIdx,
            Set<String> inlinable, Map<String, Glyph> values) throws Exception {

        GlyphAsset asset = GlyphAsset.getAssetMap().getAsset(glyphAssetId);
        List<String> assetKeys = asset != null
                ? new ArrayList<>(isInput ? asset.getInputKeys() : asset.getOutputKeys())
                : new ArrayList<>();

        for (String key : assetKeys) {
            String targetId = slots.get(key);
            writeSlotValue(out, targetId, eIdx, vIdx, inlinable, values);
        }

        List<String> extraKeys = new ArrayList<>();
        for (String key : slots.keySet()) {
            if (!assetKeys.contains(key)) extraKeys.add(key);
        }

        out.writeByte(extraKeys.size());
        for (String key : extraKeys) {
            writePrefixed(out, key);
            writeSlotValue(out, slots.get(key), eIdx, vIdx, inlinable, values);
        }
    }

    private static void writeSlotValue(DataOutputStream out, @Nullable String targetId,
            Map<String, Integer> eIdx, Map<String, Integer> vIdx,
            Set<String> inlinable, Map<String, Glyph> values) throws Exception {

        if (targetId == null) {
            out.writeByte(SLOT_EMPTY);
            return;
        }

        if (inlinable.contains(targetId)) {
            Glyph v = values.get(targetId);
            out.writeByte(getInlineCode(v.getGlyphId()));
            return;
        }

        Integer idx = eIdx.get(targetId);
        if (idx == null) idx = vIdx.get(targetId);
        out.writeByte(idx != null ? (REF_FLAG | idx) : SLOT_EMPTY);
    }

    // --- decode ---

    @Nullable
    private static Hex decode(byte[] data) throws Exception {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
            int version = in.readUnsignedByte();
            if (version != FORMAT_VERSION) return null;

            int effectCount = in.readUnsignedByte();
            int extValueCount = in.readUnsignedByte();
            int firstIdx = in.readUnsignedByte();

            int paletteSize = in.readUnsignedByte();
            List<String> palette = new ArrayList<>();
            for (int i = 0; i < paletteSize; i++) palette.add(readPrefixed(in));

            int totalNodes = effectCount + extValueCount;
            List<Glyph> glyphs = new ArrayList<>();
            Map<String, Glyph> inlinedCache = new HashMap<>();

            for (int i = 0; i < effectCount; i++) {
                Glyph g = new Glyph();
                g.setId(String.valueOf(i));
                String assetId = palette.get(in.readUnsignedByte());
                setGlyphFields(g, assetId, GlyphType.Effect);

                int volEff = in.readUnsignedByte();
                g.setVolatility((volEff >> 4) / 15f);
                g.setEfficiency((volEff & 0x0F) / 15f);

                int nextFlag = in.readUnsignedByte();
                if (nextFlag == 0) {
                    g.setNext(new ArrayList<>());
                } else if (nextFlag == 1) {
                    g.setNext(new ArrayList<>(List.of(String.valueOf(i + 1))));
                } else {
                    int count = nextFlag - 1;
                    List<String> next = new ArrayList<>();
                    for (int n = 0; n < count; n++) next.add(String.valueOf(in.readUnsignedByte()));
                    g.setNext(next);
                }

                readSlots(in, g.getInputs(), assetId, true, glyphs, inlinedCache, totalNodes);
                readSlots(in, g.getOutputs(), assetId, false, glyphs, inlinedCache, totalNodes);
                glyphs.add(g);
            }

            for (int i = 0; i < extValueCount; i++) {
                Glyph g = new Glyph();
                g.setId(String.valueOf(effectCount + i));
                String assetId = palette.get(in.readUnsignedByte());
                setGlyphFields(g, assetId, GlyphType.Value);
                readSlots(in, g.getInputs(), assetId, true, glyphs, inlinedCache, totalNodes);
                glyphs.add(g);
            }

            glyphs.addAll(inlinedCache.values());

            Hex hex = new Hex();
            for (Glyph g : glyphs) hex.put(g.getId(), g);
            hex.setFirstGlyphId(String.valueOf(firstIdx));

            HexUtils.validate(hex);
            return hex;
        }
    }

    private static void readSlots(DataInputStream in, Map<String, String> slots,
            String glyphAssetId, boolean isInput,
            List<Glyph> glyphs, Map<String, Glyph> inlinedCache, int totalNodes) throws Exception {

        GlyphAsset asset = GlyphAsset.getAssetMap().getAsset(glyphAssetId);
        List<String> assetKeys = asset != null
                ? new ArrayList<>(isInput ? asset.getInputKeys() : asset.getOutputKeys())
                : new ArrayList<>();

        for (String key : assetKeys) {
            String ref = readSlotValue(in, glyphs, inlinedCache, totalNodes);
            if (ref != null) slots.put(key, ref);
        }

        int extraCount = in.readUnsignedByte();
        for (int i = 0; i < extraCount; i++) {
            String key = readPrefixed(in);
            String ref = readSlotValue(in, glyphs, inlinedCache, totalNodes);
            if (ref != null) slots.put(key, ref);
        }
    }

    @Nullable
    private static String readSlotValue(DataInputStream in,
            List<Glyph> glyphs, Map<String, Glyph> inlinedCache, int totalNodes) throws Exception {

        int b = in.readUnsignedByte();
        if (b == SLOT_EMPTY) return null;

        if ((b & REF_FLAG) != 0) {
            return String.valueOf(b & 0x7F);
        }

        String assetId = inlineCodeToAsset(b);
        if (assetId == null) return null;

        if (inlinedCache.containsKey(assetId)) {
            return inlinedCache.get(assetId).getId();
        }

        int newIdx = totalNodes + inlinedCache.size();
        Glyph v = new Glyph();
        v.setId(String.valueOf(newIdx));
        setGlyphFields(v, assetId, GlyphType.Value);
        inlinedCache.put(assetId, v);
        return v.getId();
    }

    // --- inline value encoding ---

    private static int getInlineCode(String glyphId) {
        if (glyphId.startsWith(STR_PREFIX_NUMBER)) {
            try {
                int num = Integer.parseInt(glyphId.substring(STR_PREFIX_NUMBER.length()));
                if (num >= 1 && num <= 16) return INLINE_NUMBER_BASE + num - 1;
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return switch (glyphId) {
            case "Glyph_Variable" -> INLINE_VARIABLE;
            case "Glyph_Position" -> INLINE_POSITION;
            case "Glyph_Rotation" -> INLINE_ROTATION;
            default -> -1;
        };
    }

    @Nullable
    private static String inlineCodeToAsset(int code) {
        if (code >= INLINE_NUMBER_BASE && code <= INLINE_NUMBER_BASE + 15) {
            return STR_PREFIX_NUMBER + (code - INLINE_NUMBER_BASE + 1);
        }
        return switch (code) {
            case INLINE_VARIABLE -> "Glyph_Variable";
            case INLINE_POSITION -> "Glyph_Position";
            case INLINE_ROTATION -> "Glyph_Rotation";
            default -> null;
        };
    }

    // --- glyph field helpers ---

    private static void setGlyphFields(Glyph g, String assetId, GlyphType type) {
        try {
            java.lang.reflect.Field f = Glyph.class.getDeclaredField("glyphId");
            f.setAccessible(true);
            f.set(g, assetId);
            java.lang.reflect.Field t = Glyph.class.getDeclaredField("type");
            t.setAccessible(true);
            t.set(g, type);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // --- topological sort ---

    private static List<Glyph> topoSort(Hex hex, List<Glyph> effects) {
        Map<String, Glyph> byId = new HashMap<>();
        for (Glyph g : effects) byId.put(g.getId(), g);

        List<Glyph> sorted = new ArrayList<>();
        Set<String> visited = new HashSet<>();

        String firstId = hex.getFirstGlyphId();
        if (firstId != null && byId.containsKey(firstId)) {
            topoVisit(firstId, byId, visited, sorted);
        }
        for (Glyph g : effects) {
            if (!visited.contains(g.getId())) topoVisit(g.getId(), byId, visited, sorted);
        }
        return sorted;
    }

    private static void topoVisit(String id, Map<String, Glyph> byId,
            Set<String> visited, List<Glyph> sorted) {
        if (visited.contains(id)) return;
        visited.add(id);
        Glyph g = byId.get(id);
        if (g == null) return;
        sorted.add(g);
        for (String nid : g.getNext()) {
            if (byId.containsKey(nid)) topoVisit(nid, byId, visited, sorted);
        }
    }

    // --- prefix-compressed strings ---

    private static void writePrefixed(DataOutputStream out, String str) throws Exception {
        int pfx;
        String suffix;
        if (str.startsWith(STR_PREFIX_GLYPH)) {
            pfx = PFX_GLYPH;
            suffix = str.substring(STR_PREFIX_GLYPH.length());
        } else if (str.startsWith(STR_PREFIX_NUMBER)) {
            pfx = PFX_NUMBER;
            suffix = str.substring(STR_PREFIX_NUMBER.length());
        } else {
            pfx = PFX_NONE;
            suffix = str;
        }
        byte[] bytes = suffix.getBytes(StandardCharsets.UTF_8);
        out.writeByte((pfx << 6) | (bytes.length & 0x3F));
        out.write(bytes);
    }

    private static String readPrefixed(DataInputStream in) throws Exception {
        int header = in.readUnsignedByte();
        int pfx = (header >> 6) & 0x03;
        int len = header & 0x3F;
        byte[] bytes = new byte[len];
        in.readFully(bytes);
        String suffix = new String(bytes, StandardCharsets.UTF_8);
        return switch (pfx) {
            case PFX_GLYPH -> STR_PREFIX_GLYPH + suffix;
            case PFX_NUMBER -> STR_PREFIX_NUMBER + suffix;
            default -> suffix;
        };
    }

    // --- compression ---

    @Nullable
    private static byte[] deflate(byte[] data) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DeflaterOutputStream dos = new DeflaterOutputStream(baos,
                     new Deflater(Deflater.BEST_COMPRESSION))) {
            dos.write(data);
            dos.finish();
            return baos.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    private static byte[] inflate(byte[] data) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
             InflaterInputStream iis = new InflaterInputStream(bais)) {
            return iis.readAllBytes();
        } catch (Exception e) {
            return null;
        }
    }

    private static String computeChecksum(String data) {
        CRC32 crc = new CRC32();
        crc.update(data.getBytes());
        return String.format("%08x", crc.getValue()).substring(0, 4);
    }

    private static float clamp01(float v) {
        return Math.min(1f, Math.max(0f, v));
    }
}
