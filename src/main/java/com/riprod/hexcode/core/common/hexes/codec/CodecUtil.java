package com.riprod.hexcode.core.common.hexes.codec;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import com.hypixel.hytale.math.vector.Vector3f;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.registry.GlyphAsset;
import com.riprod.hexcode.core.common.glyphs.utils.GlyphType;
import com.riprod.hexcode.core.common.hexes.component.Hex;

public class CodecUtil {

    static final int HASH_BITS = 11;
    static final int POS_BITS = 12;
    static final int V4_MAX_INPUT_KEYS = 7;
    static final float V4_MAX_POS = 20.47f;

    private static final int PFX_NONE = 0;
    private static final int PFX_GLYPH = 1;
    private static final int PFX_NUMBER = 2;

    private static List<String> cachedDictionary;

    // --- fnv-1a hash ---

    static int assetHash(String s) {
        int h = 0x811c9dc5;
        for (byte b : s.getBytes(StandardCharsets.UTF_8)) {
            h ^= (b & 0xFF);
            h *= 0x01000193;
        }
        return h & ((1 << HASH_BITS) - 1);
    }

    // --- dictionary ---

    public static List<String> buildDictionary() {
        if (cachedDictionary != null) return cachedDictionary;
        List<String> dict = new ArrayList<>(GlyphAsset.getAssetMap().getAssetMap().keySet());
        Collections.sort(dict);
        cachedDictionary = Collections.unmodifiableList(dict);
        return cachedDictionary;
    }

    public static void invalidateCache() {
        cachedDictionary = null;
    }

    static Map<Integer, List<String>> buildHashLookup(List<String> dictionary) {
        Map<Integer, List<String>> lookup = new HashMap<>();
        for (String s : dictionary) {
            lookup.computeIfAbsent(assetHash(s), k -> new ArrayList<>()).add(s);
        }
        return lookup;
    }

    // --- palette resolution ---

    @Nullable
    static String resolvePaletteEntry(int hintIdx, int storedHash,
            List<String> dictionary, Map<Integer, List<String>> hashLookup,
            List<DecodeIssue> issues) {

        if (hintIdx < dictionary.size()) {
            String candidate = dictionary.get(hintIdx);
            if (assetHash(candidate) == storedHash) {
                return candidate;
            }
        }

        List<String> candidates = hashLookup.getOrDefault(storedHash, List.of());

        if (candidates.size() == 1) {
            String resolved = candidates.get(0);
            issues.add(new DecodeIssue(
                    "dictionary shifted: hint index " + hintIdx + " resolved to '" + resolved + "' via hash",
                    DecodeIssue.Severity.INFO));
            return resolved;
        }

        if (candidates.size() > 1) {
            String resolved = candidates.get(0);
            int bestDist = Integer.MAX_VALUE;
            for (String c : candidates) {
                int idx = dictionary.indexOf(c);
                int dist = idx >= 0 ? Math.abs(idx - hintIdx) : Integer.MAX_VALUE;
                if (dist < bestDist) {
                    bestDist = dist;
                    resolved = c;
                }
            }
            issues.add(new DecodeIssue(
                    "hash collision for hint " + hintIdx + ", resolved to '" + resolved + "' via hint proximity",
                    DecodeIssue.Severity.WARNING));
            return resolved;
        }

        issues.add(new DecodeIssue(
                "glyph (hint=" + hintIdx + ", hash=0x" + Integer.toHexString(storedHash)
                        + ") not found in current dictionary, removed from hex",
                DecodeIssue.Severity.ERROR));
        return null;
    }

    // --- scrubbing ---

    static void scrubRemoved(List<Glyph> glyphs, Set<Integer> removedIndices,
            List<DecodeIssue> issues) {
        Set<String> removedIds = new HashSet<>();
        for (int idx : removedIndices) removedIds.add(String.valueOf(idx));

        for (Glyph g : glyphs) {
            if (removedIds.contains(g.getId())) continue;

            int beforeNext = g.getNext().size();
            g.getNext().removeIf(removedIds::contains);
            if (g.getNext().size() < beforeNext) {
                issues.add(new DecodeIssue(
                        "removed " + (beforeNext - g.getNext().size())
                                + " next link(s) from '" + g.getGlyphId() + "' (glyph " + g.getId()
                                + ") pointing to removed glyph(s)",
                        DecodeIssue.Severity.WARNING));
            }

            List<String> inputsToRemove = new ArrayList<>();
            for (Map.Entry<String, String> entry : g.getInputs().entrySet()) {
                if (removedIds.contains(entry.getValue())) {
                    inputsToRemove.add(entry.getKey());
                }
            }
            for (String key : inputsToRemove) {
                g.getInputs().remove(key);
                issues.add(new DecodeIssue(
                        "removed input '" + key + "' from '" + g.getGlyphId() + "' (glyph " + g.getId()
                                + ") pointing to removed glyph",
                        DecodeIssue.Severity.WARNING));
            }
        }
    }

    // --- v4 limit check ---

    @Nullable
    static String exceedsV4Limits(Hex hex) {
        for (Glyph g : hex.getGlyphs()) {
            GlyphAsset asset = GlyphAsset.getAssetMap().getAsset(g.getGlyphId());
            if (asset != null && asset.getInputKeys().size() > V4_MAX_INPUT_KEYS) {
                return "glyph '" + g.getGlyphId() + "' has " + asset.getInputKeys().size()
                        + " asset-defined inputs (max " + V4_MAX_INPUT_KEYS + ")";
            }
            if (g.getVolatility() < 0 || g.getVolatility() > 1.0f) {
                return "glyph '" + g.getGlyphId() + "' volatility " + g.getVolatility() + " out of range [0, 1]";
            }
            if (g.getEfficiency() < 0 || g.getEfficiency() > 1.0f) {
                return "glyph '" + g.getGlyphId() + "' efficiency " + g.getEfficiency() + " out of range [0, 1]";
            }
            Vector3f pos = g.getPosition();
            if (Math.abs(pos.x) > V4_MAX_POS || Math.abs(pos.y) > V4_MAX_POS || Math.abs(pos.z) > V4_MAX_POS) {
                return "glyph '" + g.getGlyphId() + "' position exceeds ±" + V4_MAX_POS;
            }
        }
        return null;
    }

    // --- topological sort ---

    static List<Glyph> topoSort(Hex hex, List<Glyph> effects) {
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

    // --- glyph field setter (reflection) ---

    static void setGlyphFields(Glyph g, String assetId, GlyphType fallback) {
        GlyphType type = fallback;
        GlyphAsset asset = GlyphAsset.getAssetMap().getAsset(assetId);
        if (asset != null) type = asset.getGlyphType();

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

    // --- prefixed string i/o ---

    static void writePrefixedString(BitWriter bw, String s) {
        int pfx;
        String suffix;
        if (s.startsWith("Glyph_")) {
            pfx = PFX_GLYPH;
            suffix = s.substring(6);
        } else if (s.startsWith("Number_")) {
            pfx = PFX_NUMBER;
            suffix = s.substring(7);
        } else {
            pfx = PFX_NONE;
            suffix = s;
        }
        byte[] bytes = suffix.getBytes(StandardCharsets.UTF_8);
        bw.write(pfx, 2);
        bw.writeVarInt(bytes.length);
        for (byte b : bytes) bw.write(b & 0xFF, 8);
    }

    static String readPrefixedString(BitReader br) {
        int pfx = br.read(2);
        int len = br.readVarInt();
        byte[] bytes = new byte[len];
        for (int i = 0; i < len; i++) bytes[i] = (byte) br.read(8);
        String suffix = new String(bytes, StandardCharsets.UTF_8);
        return switch (pfx) {
            case PFX_GLYPH -> "Glyph_" + suffix;
            case PFX_NUMBER -> "Number_" + suffix;
            default -> suffix;
        };
    }

    // --- byte-level prefixed string i/o (for v5) ---

    static void writePrefixedByte(java.io.DataOutputStream out, String s) throws java.io.IOException {
        int pfx;
        String suffix;
        if (s.startsWith("Glyph_")) {
            pfx = PFX_GLYPH;
            suffix = s.substring(6);
        } else if (s.startsWith("Number_")) {
            pfx = PFX_NUMBER;
            suffix = s.substring(7);
        } else {
            pfx = PFX_NONE;
            suffix = s;
        }
        byte[] bytes = suffix.getBytes(StandardCharsets.UTF_8);
        out.writeByte((pfx << 6) | (bytes.length & 0x3F));
        out.write(bytes);
    }

    static String readPrefixedByte(java.io.DataInputStream in) throws java.io.IOException {
        int header = in.readUnsignedByte();
        int pfx = (header >> 6) & 0x03;
        int len = header & 0x3F;
        byte[] bytes = new byte[len];
        in.readFully(bytes);
        String suffix = new String(bytes, StandardCharsets.UTF_8);
        return switch (pfx) {
            case PFX_GLYPH -> "Glyph_" + suffix;
            case PFX_NUMBER -> "Number_" + suffix;
            default -> suffix;
        };
    }

    // --- math ---

    static int nbits(int maxVal) {
        if (maxVal <= 0) return 1;
        return Math.max(1, 32 - Integer.numberOfLeadingZeros(maxVal));
    }

    static int zigzag(int n) {
        return (n << 1) ^ (n >> 31);
    }

    static int unzigzag(int n) {
        return (n >>> 1) ^ -(n & 1);
    }

    // --- asset input keys ---

    static List<String> getAssetInputKeys(String glyphId) {
        GlyphAsset asset = GlyphAsset.getAssetMap().getAsset(glyphId);
        if (asset == null) return List.of();
        return new ArrayList<>(asset.getInputKeys());
    }

    static List<String> getAssetOutputKeys(String glyphId) {
        GlyphAsset asset = GlyphAsset.getAssetMap().getAsset(glyphId);
        if (asset == null) return List.of();
        return new ArrayList<>(asset.getOutputKeys());
    }

    // --- palette builder ---

    static List<String> buildPalette(List<Glyph> ordered) {
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (Glyph g : ordered) seen.add(g.getGlyphId());
        return new ArrayList<>(seen);
    }
}
