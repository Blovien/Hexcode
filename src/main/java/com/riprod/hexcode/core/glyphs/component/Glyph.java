package com.riprod.hexcode.core.glyphs.component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.riprod.hexcode.core.execution.component.HexContext;
import com.riprod.hexcode.core.glyphs.component.GlyphComponent;
import com.riprod.hexcode.core.glyphs.values.HexVal;
import com.riprod.hexcode.core.glyphs.variables.HexVar;
import com.riprod.hexcode.utils.SpellVarUtil;

public class Glyph {
    private String glyphId;
    private UUID id;
    private float volatility;
    private float efficiency;
    private List<HexVal> inputs;
    private List<HexVal> outputs;
    private List<UUID> next;

    public Glyph() {
    }

    public Glyph(GlyphComponent glyphComponent) {
        this.glyphId = glyphComponent.getGlyphId();
        this.id = glyphComponent.getId();
        this.volatility = glyphComponent.getVolatility();
        this.efficiency = glyphComponent.getEfficiency();
        this.inputs = glyphComponent.getInputs();
        this.outputs = glyphComponent.getOutputs();
        this.next = new ArrayList<>();
    }

    /** Getters and setters */
    public String getGlyphId() {
        return glyphId;
    }

    public float getVolatility() {
        return volatility;
    }

    public void setVolatility(float accuracy) {
        this.volatility = accuracy;
    }

    public float getEfficiency() {
        return efficiency;
    }

    public void setEfficiency(float speed) {
        this.efficiency = speed;
    }

    public UUID getId() {
        return id;
    }

    /** Inputs and Outputs */

    @Nullable
    public HexVar getInput(int key, HexContext hexContext) {
        if (key < 0 || key >= inputs.size()) {
            return null;
        }
        HexVal val = inputs.get(key);
        return val != null ? val.getValue(hexContext) : null;
    }

    public List<HexVal> getInputs() {
        return inputs;
    }

    /**
     * Gets the variable slot for a given key. Use these to access the Variables
     * inside the ExecutionContext. Defaults to the key itself if not set.
     * 
     * @param key
     * @return
     */
    @Nullable
    public Integer getOutput(int key, HexContext hexContext) {
        if (key < 0 || key >= outputs.size()) {
            return null;
        }
        Double result = SpellVarUtil.resolveNumber(outputs.get(key).getValue(hexContext));
        return result != null ? result.intValue() : null;
    }

    @Nullable
    public Integer getOutputOrNumber(int key, HexContext hexContext) {
        if (key < 0 || key >= outputs.size()) {
            return key + 1;
        }
        return getOutput(key, hexContext);
    }

    public List<HexVal> getOutputs() {
        return outputs;
    }

    public List<UUID> getNext() {
        return next;
    }

    public void setNext(List<UUID> next) {
        this.next = next;
    }

    public void addNext(UUID nextId) {
        this.next.add(nextId);
    }

    private static final ArrayCodec<UUID> UUID_ARRAY_CODEC = new ArrayCodec<>(Codec.UUID_STRING, UUID[]::new);

    public static final BuilderCodec<Glyph> CODEC = BuilderCodec
            .builder(Glyph.class, Glyph::new)
            .append(new KeyedCodec<>("GlyphId", Codec.STRING),
                    (n, v) -> n.glyphId = v, n -> n.glyphId)
            .add()
            .append(new KeyedCodec<>("Accuracy", Codec.FLOAT),
                    (n, v) -> n.volatility = v, n -> n.volatility)
            .add()
            .append(new KeyedCodec<>("Speed", Codec.FLOAT),
                    (n, v) -> n.efficiency = v, n -> n.efficiency)
            .add()
            .append(new KeyedCodec<>("Inputs", new ArrayCodec<>(HexVal.CODEC, HexVal[]::new)),
                    (n, v) -> n.inputs = v != null ? new ArrayList<>(Arrays.asList(v)) : new ArrayList<>(),
                    n -> n.inputs.toArray(HexVal[]::new))
            .add()
            .append(new KeyedCodec<>("Outputs", new ArrayCodec<>(HexVal.CODEC, HexVal[]::new)),
                    (n, v) -> n.outputs = v != null ? new ArrayList<>(Arrays.asList(v)) : new ArrayList<>(),
                    n -> n.outputs.toArray(HexVal[]::new))
            .add()
            .append(new KeyedCodec<>("Next", UUID_ARRAY_CODEC),
                    (n, v) -> n.next = v != null
                            ? new ArrayList<>(Arrays.asList(v))
                            : new ArrayList<>(),
                    n -> n.next.toArray(UUID[]::new))
            .add()
            .build();
}
