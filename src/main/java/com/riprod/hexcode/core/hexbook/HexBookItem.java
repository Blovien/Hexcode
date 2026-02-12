
package com.riprod.hexcode.core.hexbook;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;

public class HexBookItem extends Item {

    public static final int DEFAULT_MAX_CAPACITY = 10;

    public static final BuilderCodec<HexBookItem> CODEC = BuilderCodec
            .builder(HexBookItem.class, HexBookItem::new)
            .<Integer>appendInherited(
                    new KeyedCodec<>("MaxCapacity", Codec.INTEGER),
                    (item, v) -> item.maxCapacity = v,
                    item -> item.maxCapacity,
                    (item, parent) -> item.maxCapacity = parent.maxCapacity)
            .add()
            .build();

    protected int maxCapacity = DEFAULT_MAX_CAPACITY;

    public HexBookItem() {
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }
}