package com.riprod.hexcode.core.common.hexes.codec;

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

public class HexCodecV4 {

    static final int FORMAT_VERSION = 4;

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
        int nt = ordered.size();
        int refBits = CodecUtil.nbits(nt - 1);
        int effBits = ne > 0 ? CodecUtil.nbits(ne - 1) : 1;
        int fi = gidx.getOrDefault(hex.getFirstGlyphId(), 0);

        List<String> palette = CodecUtil.buildPalette(ordered);
        Map<String, Integer> palMap = new HashMap<>();
        for (int i = 0; i < palette.size(); i++) palMap.put(palette.get(i), i);
        int palBits = palette.isEmpty() ? 1 : CodecUtil.nbits(palette.size() - 1);
        int hintBits = dict.isEmpty() ? 1 : CodecUtil.nbits(dict.size() - 1);

        // default efficiency
        Map<Integer, Integer> effCounts = new HashMap<>();
        for (Glyph g : ordered) {
            int e = Math.round(Math.max(0, Math.min(1, g.getEfficiency())) * 100);
            effCounts.merge(e, 1, Integer::sum);
        }
        int defEff = effCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(80);

        // adaptive volatility range
        int volMin = 100, volMax = 0;
        for (Glyph g : ordered) {
            int v = Math.round(Math.max(0, Math.min(1, g.getVolatility())) * 100);
            volMin = Math.min(volMin, v);
            volMax = Math.max(volMax, v);
        }
        int volRange = volMax - volMin;
        int vbCandidate = volRange > 0 ? CodecUtil.nbits(volRange) : 0;
        boolean useAdaptive = vbCandidate < 7 && nt * (7 - vbCandidate) > 10;
        int volBits = useAdaptive ? vbCandidate : 7;

        BitWriter bw = new BitWriter();

        // header
        bw.write(FORMAT_VERSION, 4);
        bw.writeVarInt(ne);
        bw.writeVarInt(values.size());
        bw.write(fi, effBits);
        bw.write(defEff, 7);
        bw.write(volBits, 3);
        if (volBits < 7) bw.write(volMin, 7);

        // palette
        bw.writeVarInt(palette.size());
        bw.write(hintBits, 4);
        for (String assetId : palette) {
            int hint = dictMap.getOrDefault(assetId, 0);
            bw.write(hint, hintBits);
            bw.write(CodecUtil.assetHash(assetId), CodecUtil.HASH_BITS);
            bw.write(CodecUtil.getAssetInputKeys(assetId).size(), 3);
        }

        // per-glyph
        for (int i = 0; i < ordered.size(); i++) {
            Glyph g = ordered.get(i);
            bw.write(palMap.get(g.getGlyphId()), palBits);

            int vol = Math.round(Math.max(0, Math.min(1, g.getVolatility())) * 100);
            int eff = Math.round(Math.max(0, Math.min(1, g.getEfficiency())) * 100);
            if (volBits < 7) {
                if (volBits > 0) bw.write(vol - volMin, volBits);
            } else {
                bw.write(vol, 7);
            }
            bw.write(eff == defEff ? 1 : 0, 1);
            if (eff != defEff) bw.write(eff, 7);

            // position
            Vector3f pos = g.getPosition();
            boolean zeroPos = Math.abs(pos.x) < 0.005f && Math.abs(pos.y) < 0.005f && Math.abs(pos.z) < 0.005f;
            bw.write(zeroPos ? 1 : 0, 1);
            if (!zeroPos) {
                bw.write(CodecUtil.zigzag(Math.round(pos.x * 100)), CodecUtil.POS_BITS);
                bw.write(CodecUtil.zigzag(Math.round(pos.y * 100)), CodecUtil.POS_BITS);
                bw.write(CodecUtil.zigzag(Math.round(pos.z * 100)), CodecUtil.POS_BITS);
            }

            // next links (effects only)
            if (i < ne) {
                List<String> nxt = g.getNext();
                boolean isEnd = nxt.isEmpty();
                boolean isImplicit = nxt.size() == 1
                        && gidx.containsKey(nxt.get(0))
                        && gidx.get(nxt.get(0)) == i + 1;

                if (isEnd) {
                    bw.write(0, 2);
                } else if (isImplicit) {
                    bw.write(1, 2);
                } else if (nxt.size() == 1) {
                    bw.write(2, 2);
                    bw.write(gidx.getOrDefault(nxt.get(0), 0), effBits);
                } else {
                    bw.write(3, 2);
                    bw.writeVarInt(nxt.size());
                    for (String nid : nxt) bw.write(gidx.getOrDefault(nid, 0), effBits);
                }
            }

            // input slots
            List<String> akeys = CodecUtil.getAssetInputKeys(g.getGlyphId());
            for (String key : akeys) {
                String target = g.getInputs().get(key);
                if (target != null && gidx.containsKey(target)) {
                    bw.write(1, 1);
                    bw.write(gidx.get(target), refBits);
                } else {
                    bw.write(0, 1);
                }
            }

            List<String> extra = new ArrayList<>();
            for (String key : g.getInputs().keySet()) {
                if (!akeys.contains(key) && gidx.containsKey(g.getInputs().get(key))) {
                    extra.add(key);
                }
            }
            if (!extra.isEmpty()) {
                bw.write(1, 1);
                bw.writeVarInt(extra.size());
                for (String key : extra) {
                    CodecUtil.writePrefixedString(bw, key);
                    bw.write(gidx.get(g.getInputs().get(key)), refBits);
                }
            } else {
                bw.write(0, 1);
            }

            // output slots
            List<String> okeys = CodecUtil.getAssetOutputKeys(g.getGlyphId());
            for (String key : okeys) {
                String target = g.getOutputs().get(key);
                if (target != null && gidx.containsKey(target)) {
                    bw.write(1, 1);
                    bw.write(gidx.get(target), refBits);
                } else {
                    bw.write(0, 1);
                }
            }

            List<String> extraOut = new ArrayList<>();
            for (String key : g.getOutputs().keySet()) {
                if (!okeys.contains(key) && gidx.containsKey(g.getOutputs().get(key))) {
                    extraOut.add(key);
                }
            }
            if (!extraOut.isEmpty()) {
                bw.write(1, 1);
                bw.writeVarInt(extraOut.size());
                for (String key : extraOut) {
                    CodecUtil.writePrefixedString(bw, key);
                    bw.write(gidx.get(g.getOutputs().get(key)), refBits);
                }
            } else {
                bw.write(0, 1);
            }
        }

        return bw.flush();
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
            Map<Integer, List<String>> hashLookup, List<DecodeIssue> issues) {

        BitReader br = new BitReader(data);

        int version = br.read(4);
        if (version != FORMAT_VERSION) {
            issues.add(new DecodeIssue("unknown format version " + version, DecodeIssue.Severity.ERROR));
            return new DecodeResult(null, issues);
        }

        int ne = br.readVarInt();
        int nv = br.readVarInt();
        int nt = ne + nv;
        int effBits = ne > 0 ? CodecUtil.nbits(ne - 1) : 1;
        int fi = br.read(effBits);
        int defEff = br.read(7);
        int refBits = CodecUtil.nbits(nt - 1);
        int volBits = br.read(3);
        int volMin = volBits < 7 ? br.read(7) : 0;

        // palette
        int palSize = br.readVarInt();
        int hintBits = br.read(4);
        List<String> palette = new ArrayList<>();
        List<Integer> palKeyCounts = new ArrayList<>();
        for (int p = 0; p < palSize; p++) {
            int hintIdx = br.read(hintBits);
            int storedHash = br.read(CodecUtil.HASH_BITS);
            int keyCount = br.read(3);
            palette.add(CodecUtil.resolvePaletteEntry(hintIdx, storedHash, dict, hashLookup, issues));
            palKeyCounts.add(keyCount);
        }
        int palBits = palSize > 0 ? CodecUtil.nbits(palSize - 1) : 1;

        // glyphs
        List<Glyph> glyphs = new ArrayList<>();
        Set<Integer> removedIndices = new HashSet<>();

        for (int i = 0; i < nt; i++) {
            Glyph g = new Glyph();
            g.setId(String.valueOf(i));

            int palIdx = br.read(palBits);
            String assetId = palIdx < palette.size() ? palette.get(palIdx) : null;
            int storedKeyCount = palIdx < palKeyCounts.size() ? palKeyCounts.get(palIdx) : 0;

            if (assetId == null) {
                removedIndices.add(i);
                CodecUtil.setGlyphFields(g, "", i >= ne ? GlyphType.Value : GlyphType.Effect);
            } else {
                CodecUtil.setGlyphFields(g, assetId, i >= ne ? GlyphType.Value : GlyphType.Effect);
            }

            // volatility
            if (volBits < 7) {
                g.setVolatility((volMin + (volBits > 0 ? br.read(volBits) : 0)) / 100f);
            } else {
                g.setVolatility(br.read(7) / 100f);
            }

            // efficiency
            if (br.read(1) == 1) {
                g.setEfficiency(defEff / 100f);
            } else {
                g.setEfficiency(br.read(7) / 100f);
            }

            // position
            if (br.read(1) == 1) {
                g.setPosition(new Vector3f(0, 0, 0));
            } else {
                float px = CodecUtil.unzigzag(br.read(CodecUtil.POS_BITS)) / 100f;
                float py = CodecUtil.unzigzag(br.read(CodecUtil.POS_BITS)) / 100f;
                float pz = CodecUtil.unzigzag(br.read(CodecUtil.POS_BITS)) / 100f;
                g.setPosition(new Vector3f(px, py, pz));
            }

            // next links
            if (i < ne) {
                int nextType = br.read(2);
                if (nextType == 0) {
                    g.setNext(new ArrayList<>());
                } else if (nextType == 1) {
                    g.setNext(new ArrayList<>(List.of(String.valueOf(i + 1))));
                } else if (nextType == 2) {
                    g.setNext(new ArrayList<>(List.of(String.valueOf(br.read(effBits)))));
                } else {
                    int count = br.readVarInt();
                    List<String> next = new ArrayList<>();
                    for (int n = 0; n < count; n++) next.add(String.valueOf(br.read(effBits)));
                    g.setNext(next);
                }
            }

            // input slots
            List<String> akeys = assetId != null ? CodecUtil.getAssetInputKeys(assetId) : List.of();
            if (assetId != null && akeys.size() == storedKeyCount) {
                for (String key : akeys) {
                    if (br.read(1) == 1) {
                        g.getInputs().put(key, String.valueOf(br.read(refBits)));
                    }
                }
            } else {
                for (int s = 0; s < storedKeyCount; s++) {
                    if (br.read(1) == 1) br.read(refBits);
                }
            }

            // extra input slots
            if (br.read(1) == 1) {
                int extraCount = br.readVarInt();
                for (int e = 0; e < extraCount; e++) {
                    String key = CodecUtil.readPrefixedString(br);
                    int ref = br.read(refBits);
                    if (assetId != null) {
                        g.getInputs().put(key, String.valueOf(ref));
                    }
                }
            }

            // output slots
            List<String> okeys = assetId != null ? CodecUtil.getAssetOutputKeys(assetId) : List.of();
            for (String key : okeys) {
                if (br.read(1) == 1) {
                    g.getOutputs().put(key, String.valueOf(br.read(refBits)));
                }
            }

            // extra output slots
            if (br.read(1) == 1) {
                int extraOutCount = br.readVarInt();
                for (int e = 0; e < extraOutCount; e++) {
                    String key = CodecUtil.readPrefixedString(br);
                    int ref = br.read(refBits);
                    if (assetId != null) {
                        g.getOutputs().put(key, String.valueOf(ref));
                    }
                }
            }

            glyphs.add(g);
        }

        // scrub removed
        if (!removedIndices.isEmpty()) {
            CodecUtil.scrubRemoved(glyphs, removedIndices, issues);
        }

        // build hex
        Hex hex = new Hex();
        for (Glyph g : glyphs) {
            if (!g.getGlyphId().isEmpty()) {
                hex.put(g.getId(), g);
            }
        }

        // resolve first glyph
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
