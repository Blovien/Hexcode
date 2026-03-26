package com.riprod.hexcode.core.common.glyphs.registry;

import javax.annotation.Nullable;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class SlotDefinition {
    private String title;
    private Integer defaultSlot;
    private Double defaultValue;
    private String description;

    public SlotDefinition() {
    }

    public SlotDefinition(String title, Integer defaultSlot) {
        this.title = title;
        this.defaultSlot = defaultSlot;
    }

    public SlotDefinition(String title, Integer defaultSlot, String description) {
        this.title = title;
        this.defaultSlot = defaultSlot;
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    @Nullable
    public Integer getDefaultSlot() {
        return defaultSlot;
    }

    @Nullable
    public Double getDefaultValue() {
        return defaultValue;
    }

    public String getDescription() {
        return description;
    }

    public static final BuilderCodec<SlotDefinition> CODEC = BuilderCodec
            .builder(SlotDefinition.class, SlotDefinition::new)
            .append(new KeyedCodec<>("Title", Codec.STRING),
                    (s, v) -> s.title = v, s -> s.title)
            .add()
            .append(new KeyedCodec<>("DefaultSlot", Codec.INTEGER),
                    (s, v) -> s.defaultSlot = v, s -> s.defaultSlot)
            .add()
            .append(new KeyedCodec<>("DefaultValue", Codec.DOUBLE),
                    (s, v) -> s.defaultValue = v, s -> s.defaultValue)
            .add()
            .append(new KeyedCodec<>("Description", Codec.STRING),
                    (s, v) -> s.description = v, s -> s.description)
            .add()
            .build();
}
