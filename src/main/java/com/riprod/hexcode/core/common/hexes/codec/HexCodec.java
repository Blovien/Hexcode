package com.riprod.hexcode.core.common.hexes.codec;

import javax.annotation.Nullable;

import com.riprod.hexcode.core.common.hexes.component.Hex;
import com.riprod.hexcode.core.common.hexes.utils.HexUtils;

public class HexCodec {

    public static final String PREFIX = HexCodecV14.FRAME_PREFIX;

    @Nullable
    public static String serialize(Hex hex) {
        Hex clone = hex.clone();
        HexUtils.validate(clone);
        HexUtils.compress(clone);
        return HexCodecV14.serialize(clone);
    }

    public static DecodeResult deserialize(String data) {
        if (data == null) return DecodeResult.error("null input");
        if (!data.startsWith(PREFIX)) {
            return DecodeResult.error("unsupported format (expected " + PREFIX + ")");
        }
        DecodeResult result = HexCodecV14.deserialize(data);
        if (result.getHex() != null) {
            HexUtils.repair(result.getHex());
        }
        return result;
    }
}
