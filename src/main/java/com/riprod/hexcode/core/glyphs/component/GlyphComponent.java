package com.riprod.hexcode.core.glyphs.component;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.schema.SchemaContext;
import com.hypixel.hytale.codec.schema.config.Schema;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.glyphs.registry.GlyphAsset;
import com.riprod.hexcode.core.glyphs.utils.GlyphType;
import com.riprod.hexcode.core.glyphs.values.HexVal;
import com.riprod.hexcode.utils.SphericalPosition;

import org.bson.BsonValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class GlyphComponent implements Component<EntityStore> {

    private enum GlyphFlags {
        Hovering,
        Dragging,
        Hiding,
        Selected
    }

    public static final BuilderCodec<GlyphComponent> CODEC = BuilderCodec
            .builder(GlyphComponent.class, GlyphComponent::new)
            .append(new KeyedCodec<>("GlyphId", Codec.STRING),
                    (c, v) -> c.glyphId = v,
                    c -> c.glyphId)
            .add()
            .append(new KeyedCodec<>("Volatility", Codec.FLOAT),
                    (c, v) -> c.volatility = v,
                    c -> c.volatility)
            .add()
            .append(new KeyedCodec<>("UUID", Codec.UUID_STRING),
                    (c, v) -> c.id = v,
                    c -> c.id)
            .add()
            .append(new KeyedCodec<>("Efficiency", Codec.FLOAT),
                    (c, v) -> c.efficiency = v,
                    c -> c.efficiency)
            .add()
            .append(new KeyedCodec<>("RelativePosition", Codec.FLOAT_ARRAY),
                    (c, v) -> c.offset = new Vector3f(v[0], v[1], v[2]),
                    c -> new float[] { c.offset.x, c.offset.y, c.offset.z })
            .add()
            .append(new KeyedCodec<>("Children", new ArrayCodec<>(selfCodec(), GlyphComponent[]::new)),
                    (c, v) -> {
                        if (v != null) {
                            c.children = new ArrayList<>(Arrays.asList(v));
                        } else {
                            c.children = new ArrayList<>();
                        }
                    },
                    c -> c.children.toArray(GlyphComponent[]::new))
            .add()
            .append(new KeyedCodec<>("Inputs", new ArrayCodec<>(HexVal.CODEC, HexVal[]::new)),
                    (n, v) -> n.inputs = v != null ? new ArrayList<>(Arrays.asList(v)) : new ArrayList<>(),
                    n -> n.inputs.toArray(HexVal[]::new))
            .add()
            .append(new KeyedCodec<>("Outputs", new ArrayCodec<>(HexVal.CODEC, HexVal[]::new)),
                    (n, v) -> n.outputs = v != null ? new ArrayList<>(Arrays.asList(v)) : new ArrayList<>(),
                    n -> n.outputs.toArray(HexVal[]::new))
            .add()
            .append(new KeyedCodec<>("InputCount", Codec.INTEGER),
                    (c, v) -> c.totalInputs = v,
                    c -> c.totalInputs)
            .add()
            .append(new KeyedCodec<>("OutputCount", Codec.INTEGER),
                    (c, v) -> c.totalOutputs = v,
                    c -> c.totalOutputs)
            .add()
            .build();

    private static Codec<GlyphComponent> selfCodec() {
        return new Codec<GlyphComponent>() {
            @Override
            public GlyphComponent decode(BsonValue bsonValue, ExtraInfo extraInfo) {
                return CODEC.decode(bsonValue, extraInfo);
            }

            @Override
            public BsonValue encode(GlyphComponent value, ExtraInfo extraInfo) {
                return CODEC.encode(value, extraInfo);
            }

            @Override
            public Schema toSchema(SchemaContext context) {
                return CODEC.toSchema(context);
            }
        };
    }

    private static ComponentType<EntityStore, GlyphComponent> componentType;

    // persistent - from asset
    @Nonnull
    private String glyphId;
    private int totalInputs;
    private int totalOutputs;
    private GlyphType type;
    // persistent
    private UUID id;
    private float volatility;
    private float efficiency;
    private List<HexVal> inputs;
    private List<HexVal> outputs;
    private List<GlyphComponent> children = new ArrayList<>();

    // transient
    private Vector3f offset; // offset from parent
    private Vector3f rotation;
    private Ref<EntityStore> parentRef;
    private Ref<EntityStore> rootRef;
    private Ref<EntityStore> selfRef;
    private Set<GlyphFlags> flags;
    private float scale = 1f;

    // for the codec - do not use
    public GlyphComponent() {
    }

    public GlyphComponent(@Nonnull GlyphAsset glyphAsset, float volatility, float efficiency) {
        this.glyphId = glyphAsset.getId();
        this.volatility = volatility;
        this.efficiency = efficiency;
        this.id = UUID.randomUUID();

        this.totalInputs = glyphAsset.getInputCount();
        this.totalOutputs = glyphAsset.getOutputCount();
        this.type = glyphAsset.getGlyphType();
    }

    public static void setComponentType(ComponentType<EntityStore, GlyphComponent> type) {
        componentType = type;
    }

    public static ComponentType<EntityStore, GlyphComponent> getComponentType() {
        return componentType;
    }

    @Nonnull
    public UUID getId() {
        return id;
    }

    public void setId(@Nonnull UUID id) {
        this.id = id;
    }

    /** Getters and Setters */

    @Nonnull
    public String getGlyphId() {
        return glyphId;
    }

    @Nullable
    public Ref<EntityStore> getParentRef() {
        return parentRef;
    }

    public void setParentRef(@Nullable Ref<EntityStore> parentRef) {
        this.parentRef = parentRef;
    }

    @Nullable
    public Ref<EntityStore> getSelfRef() {
        return selfRef;
    }

    public void setSelfRef(@Nullable Ref<EntityStore> selfRef) {
        this.selfRef = selfRef;
    }

    @Nullable
    public Ref<EntityStore> getRootRef() {
        return rootRef;
    }

    public void setRootRef(@Nullable Ref<EntityStore> hexRootRef) {
        this.rootRef = hexRootRef;
    }

    public List<GlyphComponent> getChildren() {
        return children;
    }

    public GlyphType getType() {
        return type;
    }

    /** Positioning */

    public float getYaw() {
        return this.rotation.getYaw();
    }

    public void setYaw(float yaw) {
        this.rotation.setYaw(yaw);
    }

    public float getPitch() {
        return this.rotation.getPitch();
    }

    public void setPitch(float pitch) {
        this.rotation.setPitch(pitch);
    }

    public float getDistance() {
        return this.rotation.getRoll();
    }

    public void setDistance(float distance) {
        this.rotation.setRoll(distance);
    }

    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public Vector3f getOffset() {
        return offset;
    }

    public void setOffset(Vector3f offset) {
        this.offset = offset;
    }

    public void setOffset(float x, float y, float z) {
        this.offset = new Vector3f(x, y, z);
    }

    /** Flags */

    public void setDragState(Boolean isBeingDragged) {
        if (isBeingDragged) {
            this.flags.add(GlyphFlags.Dragging);
        } else {
            this.flags.remove(GlyphFlags.Dragging);
        }
    }

    public boolean isBeingDragged() {
        return this.flags.contains(GlyphFlags.Dragging);
    }

    /** Attributes */

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

    /** Casting Context Items */
    public List<HexVal> getInputs() {
        return inputs;
    }

    public void setInputs(List<HexVal> inputs) {
        this.inputs = inputs;
    }

    public List<HexVal> getOutputs() {
        return outputs;
    }

    public void setOutputs(List<HexVal> outputs) {
        this.outputs = outputs;
    }

    public int getTotalInputs() {
        return totalInputs;
    }

    public void setTotalInputs(int inputCount) {
        this.totalInputs = inputCount;
    }

    public int getTotalOutputs() {
        return totalOutputs;
    }

    public void setTotalOutputs(int outputCount) {
        this.totalOutputs = outputCount;
    }

    @Nonnull
    @Override
    public GlyphComponent clone() {
        GlyphComponent copy = new GlyphComponent();
        copy.glyphId = this.glyphId;
        copy.parentRef = this.parentRef;
        copy.id = this.id;
        copy.rotation = this.rotation;
        copy.scale = this.scale;
        copy.volatility = this.volatility;
        copy.efficiency = this.efficiency;
        copy.children = new ArrayList<>();
        copy.offset = this.offset != null ? new Vector3f(this.offset) : null;
        copy.outputs = new ArrayList<>(this.outputs);
        copy.inputs = new ArrayList<>(this.inputs);
        copy.totalInputs = this.totalInputs;
        copy.totalOutputs = this.totalOutputs;
        copy.type = this.type;
        copy.rootRef = this.rootRef;
        copy.selfRef = this.selfRef;
        copy.flags = this.flags;
        for (GlyphComponent child : this.children) {
            copy.children.add(child.clone());
        }
        return copy;
    }
}
