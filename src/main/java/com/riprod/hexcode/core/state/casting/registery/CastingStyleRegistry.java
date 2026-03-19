package com.riprod.hexcode.core.state.casting.registery;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.riprod.hexcode.builtin.styles.ArcStyle;
import com.riprod.hexcode.builtin.styles.RingStyle;
import com.riprod.hexcode.builtin.styles.SphereStyle;
import com.riprod.hexcode.core.state.casting.component.CastingStyle;

import java.util.HashMap;
import java.util.Map;

public class CastingStyleRegistry {

    private static final Map<String, CastingStyle> styles = new HashMap<>();

    private CastingStyleRegistry() {
    }

    public static void register(@Nonnull CastingStyle style) {
        styles.put(style.getStyleId(), style);
    }

    @Nullable
    public static CastingStyle get(@Nonnull String styleId) {
        return styles.get(styleId);
    }

    @Nonnull
    public static CastingStyle getOrDefault(@Nonnull String styleId) {
        CastingStyle style = styles.get(styleId);
        if (style == null) {
            style = styles.get(RingStyle.ID);
        }
        return style != null ? style : new RingStyle();
    }
}
