package com.riprod.hexcode.builtin;

import com.riprod.hexcode.builtin.glyphs.BlinkGlyph;
import com.riprod.hexcode.builtin.glyphs.FireGlyph;
import com.riprod.hexcode.builtin.glyphs.IceGlyph;
import com.riprod.hexcode.builtin.glyphs.PlasmaGlyph;
import com.riprod.hexcode.builtin.styles.ArcStyle;
import com.riprod.hexcode.builtin.styles.RingStyle;
import com.riprod.hexcode.builtin.styles.SphereStyle;
import com.riprod.hexcode.core.casting.registery.CastingStyleRegistry;
import com.riprod.hexcode.core.glyphs.registry.GlyphRegistry;

public class BuiltinPlugin {

    private static boolean initialized = false;

    private BuiltinPlugin() {
    }

    public static void startup() {
        if (initialized) {
            return;
        }
        RegisterGlyphs();
        RegisterStyles();

        initialized = true;
    }

    private static void RegisterGlyphs() {
        GlyphRegistry.register(new BlinkGlyph());
        GlyphRegistry.register(new IceGlyph());
        GlyphRegistry.register(new PlasmaGlyph());
        GlyphRegistry.register(new FireGlyph());
    }

    private static void RegisterStyles() {
        CastingStyleRegistry.register(new ArcStyle());
        CastingStyleRegistry.register(new RingStyle());
        CastingStyleRegistry.register(new SphereStyle());

    }
}
