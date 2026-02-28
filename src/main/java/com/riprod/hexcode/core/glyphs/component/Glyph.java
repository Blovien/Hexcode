package com.riprod.hexcode.core.glyphs.component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.math.vector.Vector3f;
import com.riprod.hexcode.core.execution.component.HexContext;
import com.riprod.hexcode.core.glyphs.registry.GlyphAsset;
import com.riprod.hexcode.core.glyphs.utils.GlyphType;
import com.riprod.hexcode.core.glyphs.values.HexVal;
import com.riprod.hexcode.core.glyphs.variables.HexVar;
import com.riprod.hexcode.utils.SpellVarUtil;

public class Glyph {
    private String glyphId;
    private String id;
    private float volatility;
    private float efficiency;
    private List<HexVal> inputs;
    private List<HexVal> outputs;
    private int totalInputs = 0;
    private int totalOutputs = 0;
    private List<String> next;
    private List<String> previous;
    private Vector3f relPosition;
    private Vector3f relRotation;
    private GlyphType type;

    public Glyph() {
        this.glyphId = "";
        this.id = "";
        this.volatility = 0;
        this.efficiency = 0;
        this.inputs = new ArrayList<>();
        this.outputs = new ArrayList<>();
        this.totalInputs = 0;
        this.totalOutputs = 0;
        this.next = new ArrayList<>();
        this.previous = new ArrayList<>();
        this.relPosition = new Vector3f(0, 0, 0);
        this.relRotation = new Vector3f(0, 0, 0);
        this.type = GlyphType.Effect;
    }

    public Glyph(GlyphAsset glyphAsset, float volatility, float efficiency) {
        this.glyphId = glyphAsset.getId();
        this.id = UUID.randomUUID().toString();
        this.volatility = volatility;
        this.efficiency = efficiency;
        this.totalInputs = glyphAsset.getInputCount();
        this.totalOutputs = glyphAsset.getOutputCount();
        this.inputs = this.totalInputs > 0 ? new ArrayList<>(this.totalInputs) : new ArrayList<>();
        this.outputs = this.totalOutputs > 0 ? new ArrayList<>(this.totalOutputs) : new ArrayList<>();
        this.next = new ArrayList<>();
        this.previous = new ArrayList<>();
        this.relPosition = new Vector3f(0, 0, 0);
        this.relRotation = new Vector3f(0, 0, 0);
        this.type = glyphAsset.getGlyphType();
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

    public String getId() {
        return id;
    }

    public Vector3f getPosition() {
        return relPosition;
    }

    public void setPosition(Vector3f position) {
        this.relPosition = position;
    }

    public Vector3f getRotation() {
        return relRotation;
    }

    public void setRotation(Vector3f rotation) {
        this.relRotation = rotation;
    }

    /** Inputs and Outputs */

    public HexVar getInputOrDefault(int key, HexContext hexContext, HexVar defaultValue) {
        HexVar input = getInput(key, hexContext);
        if (input == null) {
            return defaultValue;
        }
        return input;
    }

    public HexVar getInput(int key, HexContext hexContext) {
        if (key < 0 || key >= inputs.size()) {
            return hexContext.getVariable(key + 1);
        }
        HexVal val = inputs.get(key);
        return val != null ? val.getValue(hexContext) : hexContext.getVariable(key);
    }

    public HexVal getInputVal(int key) {
        if (key < 0 || key >= inputs.size()) {
            return null;
        }
        return inputs.get(key);
    }

    public List<HexVal> getInputs() {
        return inputs;
    }

    public void setInputs(List<HexVal> inputs) {
        this.inputs = inputs;
    }

    public void addInput(HexVal input) {
        this.inputs.add(input);
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

    public List<String> getNext() {
        return next;
    }

    public void setNext(List<String> next) {
        this.next = next;
    }

    public void addNext(String nextId) {
        this.next.add(nextId);
    }

    public void removeNext(String nextId) {
        this.next.remove(nextId);
    }

    public List<String> getPrevious() {
        return previous;
    }

    public void setPrevious(List<String> previous) {
        this.previous = previous;
    }

    public void addPrevious(String previousId) {
        this.previous.add(previousId);
    }

    public void removePrevious(String previousId) {
        this.previous.remove(previousId);
    }

    public static final BuilderCodec<Glyph> CODEC = BuilderCodec
            .builder(Glyph.class, Glyph::new)
            .append(new KeyedCodec<>("GlyphId", Codec.STRING),
                    (n, v) -> n.glyphId = v, n -> n.glyphId)
            .add()
            .append(new KeyedCodec<>("Id", Codec.STRING),
                    (n, v) -> n.id = v, n -> n.id)
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
            .append(new KeyedCodec<>("TotalInputs", Codec.INTEGER),
                    (n, v) -> n.totalInputs = v, n -> n.totalInputs)
            .add()
            .append(new KeyedCodec<>("TotalOutputs", Codec.INTEGER),
                    (n, v) -> n.totalOutputs = v, n -> n.totalOutputs)
            .add()
            .append(new KeyedCodec<>("Next", Codec.STRING_ARRAY),
                    (n, v) -> n.next = v != null
                            ? new ArrayList<>(Arrays.asList(v))
                            : new ArrayList<>(),
                    n -> n.next.toArray(String[]::new))
            .add()
            .append(new KeyedCodec<>("Previous", Codec.STRING_ARRAY),
                    (n, v) -> n.previous = v != null
                            ? new ArrayList<>(Arrays.asList(v))
                            : new ArrayList<>(),
                    n -> n.previous.toArray(String[]::new))
            .add()
            .append(new KeyedCodec<>("RelativePosition", Codec.FLOAT_ARRAY),
                    (c, v) -> c.relPosition = new Vector3f(v[0], v[1], v[2]),
                    c -> new float[] { c.relPosition.x, c.relPosition.y, c.relPosition.z })
            .add()
            .append(new KeyedCodec<>("RelativeRotation", Codec.FLOAT_ARRAY),
                    (c, v) -> c.relRotation = new Vector3f(v[0], v[1], v[2]),
                    c -> new float[] { c.relRotation.x, c.relRotation.y, c.relRotation.z })
            .add()
            .append(new KeyedCodec<>("Type", new EnumCodec<>(GlyphType.class)),
                    (c, v) -> c.type = v,
                    c -> c.type)
            .add()
            .build();

    public Glyph clone() {
        Glyph clone = new Glyph();
        clone.glyphId = this.glyphId;
        clone.id = this.id;
        clone.volatility = this.volatility;
        clone.efficiency = this.efficiency;
        clone.inputs = new ArrayList<>(this.inputs);
        clone.outputs = new ArrayList<>(this.outputs);
        clone.totalInputs = this.totalInputs;
        clone.totalOutputs = this.totalOutputs;
        clone.next = new ArrayList<>(this.next);
        clone.previous = new ArrayList<>(this.previous);
        clone.relPosition = new Vector3f(this.relPosition.x, this.relPosition.y, this.relPosition.z);
        clone.relRotation = new Vector3f(this.relRotation.x, this.relRotation.y, this.relRotation.z);
        clone.type = this.type;
        return clone;
    }
}
