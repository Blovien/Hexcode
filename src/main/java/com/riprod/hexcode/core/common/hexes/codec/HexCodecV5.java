package com.riprod.hexcode.core.common.hexes.codec;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import com.hypixel.hytale.math.vector.Vector3f;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.utils.GlyphType;
import com.riprod.hexcode.core.common.hexes.component.Hex;

public class HexCodecV5 {

    static final int FORMAT_VERSION = 5;

    @Nullable
    public static byte[] encode(Hex hex) {
        return encode(hex, null);
    }

    @Nullable
    public static byte[] encode(Hex hex, @Nullable List<String> dictionary) {
        List<String> dict = dictionary != null ? dictionary : CodecUtil.buildDictionary();
        Map<String, Integer> dictMap = new HashMap<>();
        for (int i = 0; i < dict.size(); i++) dictMap.put(dict.get(i), i);

        List<Glyph> allGlyphs = hex.getGlyphs();
        if (allGlyphs.isEmpty()) return null;

        List<Glyph> effects = new ArrayList<>();
        List<Glyph> values = new ArrayList<>();
        for (Glyph g : allGlyphs) {
            if (g.getType() == GlyphType.Value) values.add(g);
            else effects.add(g);
        }

        List<Glyph> ordered = CodecUtil.topoSort(hex, effects);
        ordered.addAll(values);

        Map<String, Integer> gidx = new HashMap<>();
        for (int i = 0; i < ordered.size(); i++) gidx.put(ordered.get(i).getId(), i);

        int ne = effects.size();
        int fi = gidx.getOrDefault(hex.getFirstGlyphId(), 0);

        List<String> palette = CodecUtil.buildPalette(ordered);
        Map<String, Integer> palMap = new HashMap<>();
        for (int i = 0; i < palette.size(); i++) palMap.put(palette.get(i), i);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(baos)) {

            out.writeByte(FORMAT_VERSION);
            out.writeShort(ne);
            out.writeShort(values.size());
            out.writeShort(fi);

            // palette
            out.writeShort(palette.size());
            for (String assetId : palette) {
                int hint = dictMap.getOrDefault(assetId, 0xFFFF);
                out.writeShort(hint);
                out.writeShort(CodecUtil.assetHash(assetId));
                out.writeByte(CodecUtil.getAssetInputKeys(assetId).size());
            }

            // per-glyph
            for (int i = 0; i < ordered.size(); i++) {
                Glyph g = ordered.get(i);
                out.writeByte(palMap.get(g.getGlyphId()));

                out.writeFloat(g.getVolatility());
                out.writeFloat(g.getEfficiency());

                Vector3f pos = g.getPosition();
                out.writeFloat(pos.x);
                out.writeFloat(pos.y);
                out.writeFloat(pos.z);

                // next links (effects only)
                if (i < ne) {
                    List<String> nxt = g.getNext();
                    List<Integer> refs = new ArrayList<>();
                    for (String nid : nxt) {
                        Integer idx = gidx.get(nid);
                        if (idx != null) refs.add(idx);
                    }
                    out.writeByte(refs.size());
                    for (int ref : refs) out.writeShort(ref);
                }

                // input slots: connected asset keys then extras
                List<String> akeys = CodecUtil.getAssetInputKeys(g.getGlyphId());
                List<String[]> connectedAkeys = new ArrayList<>();
                for (String key : akeys) {
                    String target = g.getInputs().get(key);
                    if (target != null && gidx.containsKey(target)) {
                        connectedAkeys.add(new String[]{key, String.valueOf(gidx.get(target))});
                    }
                }

                List<String[]> extras = new ArrayList<>();
                for (Map.Entry<String, String> entry : g.getInputs().entrySet()) {
                    if (!akeys.contains(entry.getKey()) && gidx.containsKey(entry.getValue())) {
                        extras.add(new String[]{entry.getKey(), String.valueOf(gidx.get(entry.getValue()))});
                    }
                }

                out.writeByte(connectedAkeys.size());
                for (String[] pair : connectedAkeys) {
                    CodecUtil.writePrefixedByte(out, pair[0]);
                    out.writeShort(Integer.parseInt(pair[1]));
                }

                out.writeByte(extras.size());
                for (String[] pair : extras) {
                    CodecUtil.writePrefixedByte(out, pair[0]);
                    out.writeShort(Integer.parseInt(pair[1]));
                }

                // output slots: connected asset keys then extras
                List<String> outKeys = CodecUtil.getAssetOutputKeys(g.getGlyphId());
                List<String[]> connectedOutKeys = new ArrayList<>();
                for (String key : outKeys) {
                    String target = g.getOutputs().get(key);
                    if (target != null && gidx.containsKey(target)) {
                        connectedOutKeys.add(new String[]{key, String.valueOf(gidx.get(target))});
                    }
                }

                List<String[]> extraOuts = new ArrayList<>();
                for (Map.Entry<String, String> entry : g.getOutputs().entrySet()) {
                    if (!outKeys.contains(entry.getKey()) && gidx.containsKey(entry.getValue())) {
                        extraOuts.add(new String[]{entry.getKey(), String.valueOf(gidx.get(entry.getValue()))});
                    }
                }

                out.writeByte(connectedOutKeys.size());
                for (String[] pair : connectedOutKeys) {
                    CodecUtil.writePrefixedByte(out, pair[0]);
                    out.writeShort(Integer.parseInt(pair[1]));
                }

                out.writeByte(extraOuts.size());
                for (String[] pair : extraOuts) {
                    CodecUtil.writePrefixedByte(out, pair[0]);
                    out.writeShort(Integer.parseInt(pair[1]));
                }
            }

            out.flush();
            return baos.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    public static DecodeResult decode(byte[] data) {
        return decode(data, null);
    }

    public static DecodeResult decode(byte[] data, @Nullable List<String> dictionary) {
        List<String> dict = dictionary != null ? dictionary : CodecUtil.buildDictionary();
        Map<Integer, List<String>> hashLookup = CodecUtil.buildHashLookup(dict);
        List<DecodeIssue> issues = new ArrayList<>();

        try {
            return decodeInner(data, dict, hashLookup, issues);
        } catch (Exception e) {
            issues.add(new DecodeIssue("decode failed: " + e.getMessage(), DecodeIssue.Severity.ERROR));
            return new DecodeResult(null, issues);
        }
    }

    private static DecodeResult decodeInner(byte[] data, List<String> dict,
            Map<Integer, List<String>> hashLookup, List<DecodeIssue> issues) throws Exception {

        DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));

        int version = in.readUnsignedByte();
        if (version != FORMAT_VERSION) {
            issues.add(new DecodeIssue("unknown format version " + version, DecodeIssue.Severity.ERROR));
            return new DecodeResult(null, issues);
        }

        int ne = in.readUnsignedShort();
        int nv = in.readUnsignedShort();
        int fi = in.readUnsignedShort();
        int nt = ne + nv;

        // palette
        int palSize = in.readUnsignedShort();
        List<String> palette = new ArrayList<>();
        List<Integer> palKeyCounts = new ArrayList<>();
        for (int p = 0; p < palSize; p++) {
            int hint = in.readUnsignedShort();
            int hash = in.readUnsignedShort();
            int keyCount = in.readUnsignedByte();
            palette.add(CodecUtil.resolvePaletteEntry(hint, hash, dict, hashLookup, issues));
            palKeyCounts.add(keyCount);
        }

        // glyphs
        List<Glyph> glyphs = new ArrayList<>();
        Set<Integer> removedIndices = new HashSet<>();

        for (int i = 0; i < nt; i++) {
            Glyph g = new Glyph();
            g.setId(String.valueOf(i));

            int palIdx = in.readUnsignedByte();
            String assetId = palIdx < palette.size() ? palette.get(palIdx) : null;

            if (assetId == null) {
                removedIndices.add(i);
                CodecUtil.setGlyphFields(g, "", i >= ne ? GlyphType.Value : GlyphType.Effect);
            } else {
                CodecUtil.setGlyphFields(g, assetId, i >= ne ? GlyphType.Value : GlyphType.Effect);
            }

            g.setVolatility(in.readFloat());
            g.setEfficiency(in.readFloat());

            float px = in.readFloat();
            float py = in.readFloat();
            float pz = in.readFloat();
            g.setPosition(new Vector3f(px, py, pz));

            if (i < ne) {
                int nxtCount = in.readUnsignedByte();
                List<String> next = new ArrayList<>();
                for (int n = 0; n < nxtCount; n++) next.add(String.valueOf(in.readUnsignedShort()));
                g.setNext(next);
            }

            // connected asset keys
            int akeyCount = in.readUnsignedByte();
            for (int a = 0; a < akeyCount; a++) {
                String key = CodecUtil.readPrefixedByte(in);
                int ref = in.readUnsignedShort();
                if (assetId != null) g.getInputs().put(key, String.valueOf(ref));
            }

            // extra input keys
            int extraCount = in.readUnsignedByte();
            for (int e = 0; e < extraCount; e++) {
                String key = CodecUtil.readPrefixedByte(in);
                int ref = in.readUnsignedShort();
                if (assetId != null) g.getInputs().put(key, String.valueOf(ref));
            }

            // connected output keys
            int outKeyCount = in.readUnsignedByte();
            for (int o = 0; o < outKeyCount; o++) {
                String key = CodecUtil.readPrefixedByte(in);
                int ref = in.readUnsignedShort();
                if (assetId != null) g.getOutputs().put(key, String.valueOf(ref));
            }

            // extra output keys
            int extraOutCount = in.readUnsignedByte();
            for (int o = 0; o < extraOutCount; o++) {
                String key = CodecUtil.readPrefixedByte(in);
                int ref = in.readUnsignedShort();
                if (assetId != null) g.getOutputs().put(key, String.valueOf(ref));
            }

            glyphs.add(g);
        }

        if (!removedIndices.isEmpty()) {
            CodecUtil.scrubRemoved(glyphs, removedIndices, issues);
        }

        Hex hex = new Hex();
        for (Glyph g : glyphs) {
            if (!g.getGlyphId().isEmpty()) {
                hex.put(g.getId(), g);
            }
        }

        String fiStr = String.valueOf(fi);
        if (hex.get(fiStr) != null) {
            hex.setFirstGlyphId(fiStr);
        } else if (!hex.getGlyphs().isEmpty()) {
            String fallback = hex.getGlyphs().get(0).getId();
            hex.setFirstGlyphId(fallback);
            issues.add(new DecodeIssue(
                    "first glyph index " + fi + " was removed, reassigned to " + fallback,
                    DecodeIssue.Severity.WARNING));
        } else {
            issues.add(new DecodeIssue("all glyphs were removed", DecodeIssue.Severity.ERROR));
            return new DecodeResult(null, issues);
        }

        return new DecodeResult(hex, issues);
    }
}
