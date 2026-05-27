package com.riprod.hexcode.utils;

import com.riprod.hexcode.core.common.glyphs.variables.HexVar;

public class HexCompareUtil {

    private HexCompareUtil() {
    }

    public static boolean isEqual(HexVar a, HexVar b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equalTo(b);
    }

    public static boolean isGreater(HexVar a, HexVar b) {
        if (a == null && b == null) return false;
        if (a == null || b == null) return a == null ? false : true;
        return a.compareTo(b) > 0;
    }

    public static boolean isLess(HexVar a, HexVar b) {
        if (a == null && b == null) return false;
        if (a == null || b == null) return b == null ? false : true;
        return a.compareTo(b) < 0;
    }
}
