package com.riprod.hexcode.core.glyphs.utils;

public enum GlyphType {
    /** Wraps a number */
    VARIABLE,
    /** Just a number */
    PRIMITIVE,
    /** Takes a number or variable, modifies context */
    GLYPH,
    /** Can output to multiple different glyphs via links.  */
    NODE,
}
