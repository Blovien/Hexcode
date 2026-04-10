package com.riprod.hexcode.core.common.glyphs.registry;

import javax.annotation.Nullable;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.DebugShape;

public class SlotAsset {
    private String label;
    private String description;
    private Vector3f color = new Vector3f(1f, 1f, 1f);
    @Nullable
    private Vector3f offset;
    private DebugShape shape = DebugShape.Cube;
    @Nullable
    private String defaultDisplay;
    @Nullable
    private Double defaultValue;
    private boolean unique = false;

    public SlotAsset() {
    }

    public static SlotAsset of(String label, String description, Vector3f color,
            @Nullable Vector3f offset, DebugShape shape) {
        SlotAsset asset = new SlotAsset();
        asset.label = label;
        asset.description = description;
        asset.color = color;
        asset.offset = offset;
        asset.shape = shape;
        return asset;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public Vector3f getColor() {
        return color;
    }

    @Nullable
    public Vector3f getOffset() {
        return offset;
    }

    public DebugShape getShape() {
        return shape;
    }

    @Nullable
    public String getDefaultDisplay() {
        return defaultDisplay;
    }

    @Nullable
    public Double getDefaultValue() {
        return defaultValue;
    }

    public boolean isUnique() {
        return unique;
    }

    public static final BuilderCodec<SlotAsset> CODEC = BuilderCodec
            .builder(SlotAsset.class, SlotAsset::new)
            .append(new KeyedCodec<>("Label", Codec.STRING),
                    (s, v) -> s.label = v, s -> s.label)
            .add()
            .append(new KeyedCodec<>("Description", Codec.STRING),
                    (s, v) -> s.description = v, s -> s.description)
            .add()
            .append(new KeyedCodec<>("Color", Codec.FLOAT_ARRAY),
                    (s, v) -> {
                        if (v != null && v.length >= 3) s.color = new Vector3f(v[0], v[1], v[2]);
                    },
                    s -> new float[] { s.color.x, s.color.y, s.color.z })
            .add()
            .append(new KeyedCodec<>("Shape", new EnumCodec<>(DebugShape.class)),
                    (s, v) -> s.shape = v, s -> s.shape)
            .add()
            .append(new KeyedCodec<>("DefaultDisplay", Codec.STRING),
                    (s, v) -> s.defaultDisplay = v, s -> s.defaultDisplay)
            .add()
            .append(new KeyedCodec<>("DefaultValue", Codec.DOUBLE),
                    (s, v) -> s.defaultValue = v, s -> s.defaultValue)
            .add()
            .append(new KeyedCodec<>("Unique", Codec.BOOLEAN),
                    (s, v) -> s.unique = v != null && v, s -> s.unique)
            .add()
            .build();
}
