package com.riprod.hexcode.core.glyphs.registry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import com.riprod.hexcode.core.glyphs.values.HexVal;


public class HexValueRegistry {
    private static final Map<String, HexVal> glyphs = new HashMap<>();
    private static boolean initialized = false;

    private HexValueRegistry() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
    }

    public static void register(String glyphId, @Nonnull HexVal glyph) {
        glyphs.put(glyphId, glyph);
    }

    @Nullable
    public static HexVal get(@Nonnull String glyphId) {
        return glyphs.get(glyphId);
    }

    @Nonnull
    public static Map<String, HexVal> getAll() {
        return new HashMap<>(glyphs);
    }
}
