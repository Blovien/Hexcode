package com.riprod.hexcode.core.glyphs.registry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import com.riprod.hexcode.core.glyphs.component.GlyphHandler;


public class GlyphRegistry {
    private static final Map<String, GlyphHandler> glyphs = new HashMap<>();
    private static boolean initialized = false;

    private GlyphRegistry() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
    }

    public static void register(@Nonnull GlyphHandler glyph) {
        glyphs.put(glyph.getId(), glyph);
    }

    @Nullable
    public static GlyphHandler get(@Nonnull String glyphId) {
        return glyphs.get(glyphId);
    }

    @Nonnull
    public static Map<String, GlyphHandler> getAll() {
        return new HashMap<>(glyphs);
    }
}
