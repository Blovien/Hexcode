package com.riprod.hexcode.core.common.glyphs.registry;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class SlotDefinition {
    private String title;
    private int defaultSlot;
    private String description;

    public SlotDefinition() {
    }

    public SlotDefinition(String title, int defaultSlot) {
        this.title = title;
        this.defaultSlot = defaultSlot;
    }

    public SlotDefinition(String title, int defaultSlot, String description) {
        this.title = title;
        this.defaultSlot = defaultSlot;
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public int getDefaultSlot() {
        return defaultSlot;
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
            .append(new KeyedCodec<>("Description", Codec.STRING),
                    (s, v) -> s.description = v, s -> s.description)
            .add()
            .build();
}
