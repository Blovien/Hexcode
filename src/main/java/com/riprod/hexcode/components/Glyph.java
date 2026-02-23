package com.riprod.hexcode.components;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.riprod.hexcode.core.glyphs.values.HexVal;
import com.riprod.hexcode.core.glyphs.variables.HexVar;

public class Glyph {
    private String glyphId;
    private float accuracy;
    private float speed;
    private List<HexVal> inputs = new ArrayList<>();
    private List<Integer> outputs = new ArrayList<>();
    private List<UUID> next = new ArrayList<>();

    public Glyph() {
    }

    @Nullable
    public String getGlyphId() {
        return glyphId;
    }

    public void setGlyphId(String glyphId) {
        this.glyphId = glyphId;
    }

    public float getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(float accuracy) {
        this.accuracy = accuracy;
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    @Nullable
    public HexVar getInput(int key, ExecutionContext executionContext, HexContext hexContext) {
        if (key < 0 || key >= inputs.size()) {
            return null;
        }
        HexVal val = inputs.get(key);
        return val != null ? val.getValue(executionContext, hexContext) : null;
    }

    public void setInputs(List<HexVal> inputs) {
        this.inputs = inputs;
    }

    public void setOutputs(List<Integer> outputs) {
        this.outputs = outputs;
    }

    public void setInput(int key, HexVal value) {
        if (key < 0 || key >= inputs.size()) {
            return;
        }
        inputs.set(key, value);
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
    public Integer getOutput(int key) {
        if (key < 0 || key >= outputs.size()) {
            return null;
        }
        return outputs.get(key);
    }

    @Nullable
    public Integer getOutputOrNumber(int key) {
        if (key < 0 || key >= outputs.size()) {
            return key;
        }
        return outputs.get(key);
    }

    public void setOutput(int key, int value) {
        if (key < 0 || key >= outputs.size()) {
            return;
        }
        outputs.set(key, value);
    }

    public void addOutput(int value) {
        outputs.add(value);
    }

    public List<Integer> getOutputs() {
        return new ArrayList<>(outputs.stream().map(Integer::intValue).toList());
    }

    public List<UUID> getNext() {
        return next;
    }

    public void setNext(List<UUID> next) {
        this.next = next;
    }

    private static final ArrayCodec<UUID> UUID_ARRAY_CODEC = new ArrayCodec<>(Codec.UUID_STRING, UUID[]::new);

    public static final BuilderCodec<Glyph> CODEC = BuilderCodec
            .builder(Glyph.class, Glyph::new)
            .append(new KeyedCodec<>("GlyphId", Codec.STRING),
                    (n, v) -> n.glyphId = v, n -> n.glyphId)
            .add()
            .append(new KeyedCodec<>("Accuracy", Codec.FLOAT),
                    (n, v) -> n.accuracy = v, n -> n.accuracy)
            .add()
            .append(new KeyedCodec<>("Speed", Codec.FLOAT),
                    (n, v) -> n.speed = v, n -> n.speed)
            .add()
            .append(new KeyedCodec<>("Inputs", new ArrayCodec<>(HexVal.CODEC, HexVal[]::new)),
                    (n, v) -> n.inputs = v != null ? new ArrayList<>(Arrays.asList(v)) : new ArrayList<>(),
                    n -> n.inputs.toArray(HexVal[]::new))
            .add()
            .append(new KeyedCodec<>("Outputs", Codec.INT_ARRAY),
                    (n, v) -> n.outputs = v != null
                            ? new ArrayList<>(Arrays.asList(Arrays.stream(v).boxed().toArray(Integer[]::new)))
                            : new ArrayList<>(),
                    n -> n.outputs.stream().mapToInt(Number::intValue).toArray())
            .add()
            .append(new KeyedCodec<>("Next", UUID_ARRAY_CODEC),
                    (n, v) -> n.next = v != null
                            ? new ArrayList<>(Arrays.asList(v))
                            : new ArrayList<>(),
                    n -> n.next.toArray(UUID[]::new))
            .add()
            .build();
}
