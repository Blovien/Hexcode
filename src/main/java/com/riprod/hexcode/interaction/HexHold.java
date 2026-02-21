package com.riprod.hexcode.interaction;

import java.util.Arrays;

import javax.annotation.Nonnull;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.ChargingInteraction;

import it.unimi.dsi.fastutil.floats.Float2ObjectOpenHashMap;

public class HexHold extends ChargingInteraction {

    @Nonnull
    public static final BuilderCodec<HexHold> CODEC = BuilderCodec
            .builder(HexHold.class, HexHold::new, ChargingInteraction.ABSTRACT_CODEC)
            .<String>appendInherited(
                    new KeyedCodec<>("Next", Interaction.CHILD_ASSET_CODEC),
                    (i, s) -> {
                        i.next = new Float2ObjectOpenHashMap<>();
                        i.next.put(0.0f, s);
                    },
                    i -> i.next != null ? i.next.get(0.0f) : null,
                    (i, p) -> i.next = p.next)
            .add()
            .afterDecode(i -> {
                i.allowIndefiniteHold = true;
                if (i.next != null) {
                    i.sortedKeys = i.next.keySet().toFloatArray();
                    Arrays.sort(i.sortedKeys);
                    i.highestChargeValue = i.sortedKeys[i.sortedKeys.length - 1];
                }
            })
            .build();

    public HexHold() {
    }

}
