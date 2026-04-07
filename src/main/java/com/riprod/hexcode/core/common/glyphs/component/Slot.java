package com.riprod.hexcode.core.common.glyphs.component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class Slot {
    public List<String> links = new ArrayList<>();
    public String slotId;

    public static final BuilderCodec<Slot> CODEC = BuilderCodec.builder(Slot.class, Slot::new)
            .append(new KeyedCodec<>("Links", Codec.STRING_ARRAY),
                    (s, v) -> s.links = v != null ? new ArrayList<>(Arrays.asList(v)) : new ArrayList<>(),
                    s -> s.links.toArray(String[]::new))
            .add()
            .append(new KeyedCodec<>("SlotId", Codec.STRING),
                    (s, v) -> s.slotId = v,
                    s -> s.slotId)
            .add()
            .build();

    public Slot clone() {
        Slot clone = new Slot();
        clone.links = new ArrayList<>(this.links);
        clone.slotId = this.slotId;

        return clone;
    }

    @Override
    public String toString() {
        return slotId;
    }
}
