package com.riprod.hexcode.core.hexstaff;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;

public class HexStaffItem extends Item {

    public static final String DEFAULT_STYLE_ID = "ring";

    public static final BuilderCodec<HexStaffItem> CODEC = BuilderCodec
            .builder(HexStaffItem.class, HexStaffItem::new)
            .<String>appendInherited(
                    new KeyedCodec<>("StyleId", Codec.STRING),
                    (item, v) -> item.styleId = v,
                    item -> item.styleId,
                    (item, parent) -> item.styleId = parent.styleId)
            .add()
            .build();

    protected String styleId = DEFAULT_STYLE_ID;

    public HexStaffItem() {
    }

    public String getStyleId() {
        return styleId;
    }
}