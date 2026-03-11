package com.riprod.hexcode.core.common.glyphs.registry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.riprod.hexcode.core.common.glyphs.values.HexValInterface;

import java.util.HashMap;
import java.util.Map;


public class HexValueRegistry {
    private static final Map<String, HexValInterface> glyphs = new HashMap<>();
    private static boolean initialized = false;

    private HexValueRegistry() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
    }

    public static void register(String glyphId, @Nonnull HexValInterface glyph) {
        glyphs.put(glyphId, glyph);
    }

    @Nullable
    public static HexValInterface get(@Nonnull String glyphId) {
        return glyphs.get(glyphId);
    }

    @Nonnull
    public static Map<String, HexValInterface> getAll() {
        return new HashMap<>(glyphs);
    }
}
