package com.riprod.hexcode.builtin.glyphs;

public class GlyphRegister {

    private static boolean initialized = false;

    private GlyphRegister() {
    }

    public static void registerAll() {
        if (initialized) {
            return;
        }
        // todo: register builtin glyph implementations
        // glyphs are defined as assets in Server/Glyphs/
        // this would register any java-side handlers for specific glyphs
        initialized = true;
    }
}
