package com.riprod.hexcode.core.common.imbuement;

import java.util.Map;

import com.hypixel.hytale.codec.Codec;
import com.riprod.hexcode.core.common.imbuement.codec.ImbuementMapCodec;
import com.riprod.hexcode.core.common.imbuement.component.ImbuementData;

public final class ImbuementMetadata {

    public static final String KEY = "Imbuement";
    public static final Codec<Map<String, ImbuementData>> CODEC = ImbuementMapCodec.INSTANCE;
    public static final String DEFAULT_SLOT = ImbuementMapCodec.LEGACY_DEFAULT_KEY;

    private ImbuementMetadata() {
    }
}
