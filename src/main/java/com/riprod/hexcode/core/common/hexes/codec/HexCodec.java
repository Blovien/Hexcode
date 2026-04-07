package com.riprod.hexcode.core.common.hexes.codec;

import java.util.List;

import javax.annotation.Nullable;

import com.riprod.hexcode.core.common.hexes.component.Hex;

// stub: full hex serialization is deferred (see plan deferred items).
// new shape will use Hex.CODEC directly via the standard BSON path.
public class HexCodec {

    @Nullable
    public static String serialize(Hex hex) {
        return null;
    }

    public static DecodeResult deserialize(String data) {
        return new DecodeResult(null, List.of(
                new DecodeIssue("hex serialization not yet implemented for new shape",
                        DecodeIssue.Severity.ERROR)));
    }
}
