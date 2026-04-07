package com.riprod.hexcode.core.common.glyphs.component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.VersionedExtraInfo;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.codecs.map.EnumMapCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.math.vector.Vector3f;
import com.riprod.hexcode.core.common.glyphs.registry.GlyphAsset;
import com.riprod.hexcode.core.common.glyphs.registry.HexValueRegistry;
import com.riprod.hexcode.core.common.glyphs.registry.SlotDefinition;
import com.riprod.hexcode.core.common.glyphs.utils.GlyphType;
import com.riprod.hexcode.core.common.glyphs.utils.SlotType;
import com.riprod.hexcode.core.common.glyphs.values.HexValInterface;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.common.glyphs.variables.NumberVar;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.state.HexState;
import com.riprod.hexcode.utils.SpellVarUtil;

public class Glyph {
    private static final int MAX_RESOLVE_DEPTH = 8;
    private static final ThreadLocal<Integer> resolveDepth = ThreadLocal.withInitial(() -> 0);

    private String glyphId;
    private String id;
    private float volatility;
    private float efficiency;
    private Map<SlotType, Slot[]> slots;
    private Vector3f relPosition;
    private Vector3f relRotation;
    private GlyphType type;

    public Glyph() {
        this.glyphId = "";
        this.id = "";
        this.volatility = 0;
        this.efficiency = 0;
        this.slots = new HashMap<>();
        this.relPosition = new Vector3f(0, 0, 0);
        this.relRotation = new Vector3f(0, 0, 0);
        this.type = GlyphType.Effect;
    }

    public Glyph(GlyphAsset glyphAsset, float volatility, float efficiency) {
        this.glyphId = glyphAsset.getId();
        this.id = UUID.randomUUID().toString();
        this.volatility = volatility;
        this.efficiency = efficiency;
        this.slots = new HashMap<>();
        this.relPosition = new Vector3f(0, 0, 0);
        this.relRotation = new Vector3f(0, 0, 0);
        this.type = glyphAsset.getGlyphType();
    }

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

    public void setId(String id) {
        this.id = id;
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

    public GlyphType getType() {
        return type;
    }

    @Nullable
    public HexVar resolveInput(String key, HexContext hexContext) {
        int depth = resolveDepth.get();
        if (depth >= MAX_RESOLVE_DEPTH) {
            return null;
        }

        String valueGlyphId = inputs.get(key);

        if (valueGlyphId == null) {
            GlyphAsset asset = GlyphAsset.getAssetMap().getAsset(glyphId);
            if (asset == null)
                return null;
            SlotDefinition def = asset.getInputDef(key);
            if (def == null)
                return null;
            if (def.getDefaultValue() != null)
                return new NumberVar(def.getDefaultValue());
            if (def.getDefaultSlot() != null)
                return hexContext.getVariable(def.getDefaultSlot());
            return null;
        }

        Glyph valueGlyph = hexContext.gethex().get(valueGlyphId);
        if (valueGlyph == null)
            return null;

        HexValInterface resolver = HexValueRegistry.get(valueGlyph.getGlyphId());
        if (resolver == null)
            return null;

        resolveDepth.set(depth + 1);
        try {
            return resolver.getValue(valueGlyph, hexContext);
        } finally {
            resolveDepth.set(depth);
        }
    }

    @Nullable
    public HexVar resolveInputOrDefault(String key, HexContext hexContext, HexVar defaultValue) {
        HexVar result = resolveInput(key, hexContext);
        return result != null ? result : defaultValue;
    }

    @Nullable
    public Integer resolveOutput(String key, HexContext hexContext) {
        String valueGlyphId = outputs.get(key);

        if (valueGlyphId == null) {
            GlyphAsset asset = GlyphAsset.getAssetMap().getAsset(glyphId);
            if (asset == null)
                return null;
            SlotDefinition def = asset.getOutputDef(key);
            if (def == null)
                return null;
            return def.getDefaultSlot();
        }

        Glyph valueGlyph = hexContext.gethex().get(valueGlyphId);
        if (valueGlyph == null)
            return null;

        HexValInterface resolver = HexValueRegistry.get(valueGlyph.getGlyphId());
        if (resolver == null)
            return null;

        int depth = resolveDepth.get();
        if (depth >= MAX_RESOLVE_DEPTH)
            return null;

        resolveDepth.set(depth + 1);
        try {
            HexVar resolved = resolver.getValue(valueGlyph, hexContext);
            Double num = SpellVarUtil.resolveNumber(resolved);
            return num != null ? num.intValue() : null;
        } finally {
            resolveDepth.set(depth);
        }
    }

    public Map<String, String> getInputs() {
        return inputs;
    }

    public void setInput(String key, String valueGlyphId) {
        this.inputs.put(key, valueGlyphId);
    }

    public void removeInput(String key) {
        this.inputs.remove(key);
    }

    public Map<String, String> getOutputs() {
        return outputs;
    }

    public void setOutput(String key, String valueGlyphId) {
        this.outputs.put(key, valueGlyphId);
    }

    public void removeOutput(String key) {
        this.outputs.remove(key);
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

    public static final BuilderCodec<Glyph> CODEC = buildCodec();

    @SuppressWarnings("unchecked")
    private static BuilderCodec<Glyph> buildCodec() {
        Codec<Map<String, String>> stringMapCodec = (Codec<Map<String, String>>) (Codec<?>) new MapCodec<>(Codec.STRING,
                HashMap::new, false);
        return BuilderCodec
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
                .<Map<SlotType, Slot[]>>append(
                        new KeyedCodec<>("Slots",
                                new EnumMapCodec<>(
                                        SlotType.class,
                                        new ArrayCodec<>(Slot.CODEC, Slot[]::new),
                                        () -> new EnumMap<>(SlotType.class),
                                        false)), // mutable
                        (g, m) -> g.slots = m != null ? m : new EnumMap<>(SlotType.class),
                        g -> g.slots)
                .add()
                /** Deprecated fields */
                .<Map<String, String>>append(new KeyedCodec<>("Inputs", stringMapCodec),
                        (n, v) -> n._legacyInputs = v != null ? new HashMap<>(v) : new HashMap<>(),
                        n -> n._legacyInputs)
                .setVersionRange(1, 1)
                .add()
                .<Map<String, String>>append(new KeyedCodec<>("Outputs", stringMapCodec),
                        (n, v) -> n._legacyOutputs = v != null ? new HashMap<>(v) : new HashMap<>(),
                        n -> n._legacyOutputs)
                .setVersionRange(1, 1)
                .add()
                .append(new KeyedCodec<>("Next", Codec.STRING_ARRAY),
                        (n, v) -> n._legacyNext = v != null
                                ? new ArrayList<>(Arrays.asList(v))
                                : new ArrayList<>(),
                        n -> n._legacyNext.toArray(String[]::new))
                .setVersionRange(1, 1)
                .add()
                .append(new KeyedCodec<>("Previous", Codec.STRING_ARRAY),
                        (n, v) -> n._legacyPrevious = v != null
                                ? new ArrayList<>(Arrays.asList(v))
                                : new ArrayList<>(),
                        n -> n._legacyPrevious.toArray(String[]::new))
                .setVersionRange(1, 1)
                .add()
                .codecVersion(2) // current codec version
                .versioned()
                .build();
    }

    /** @deprecated */
    private transient Map<String, String> _legacyInputs;
    /** @deprecated */
    private transient Map<String, String> _legacyOutputs;
    /** @deprecated */
    private transient List<String> _legacyNext;
    /** @deprecated */
    private transient List<String> _legacyPrevious;

    public Glyph clone() {
        Glyph clone = new Glyph();
        clone.glyphId = this.glyphId;
        clone.id = this.id;
        clone.volatility = this.volatility;
        clone.efficiency = this.efficiency;
        clone.relPosition = new Vector3f(this.relPosition.x, this.relPosition.y, this.relPosition.z);
        clone.relRotation = new Vector3f(this.relRotation.x, this.relRotation.y, this.relRotation.z);
        clone.type = this.type;
        return clone;
    }

    @Override
    public String toString() {
        return glyphId;
    }
}
