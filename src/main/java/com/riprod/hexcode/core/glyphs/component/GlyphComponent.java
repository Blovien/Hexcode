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
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.utils.SphericalPosition;

import org.bson.BsonValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class GlyphComponent implements Component<EntityStore> {

    public static final BuilderCodec<GlyphComponent> CODEC = BuilderCodec
            .builder(GlyphComponent.class, GlyphComponent::new)
            .append(new KeyedCodec<>("GlyphId", Codec.STRING),
                    (c, v) -> c.glyphId = v,
                    c -> c.glyphId)
            .add()
            .append(new KeyedCodec<>("Accuracy", Codec.FLOAT),
                    (c, v) -> c.accuracy = v,
                    c -> c.accuracy)
            .add()
            .append(new KeyedCodec<>("UUID", Codec.UUID_STRING),
                    (c, v) -> c.id = v,
                    c -> c.id)
            .add()
            .append(new KeyedCodec<>("Speed", Codec.FLOAT),
                    (c, v) -> c.speed = v,
                    c -> c.speed)
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

    private List<GlyphComponent> children = new ArrayList<>();

    // persistent
    @Nonnull
    private String glyphId;;
    @Nonnull
    private UUID id;
    private float accuracy = 1f;
    private float speed = 1f;
    private float idealSpeed = 1f;

    // non-persistent
    @Nullable
    private Ref<EntityStore> ownerRef;
    private Ref<EntityStore> rootRef;
    private Ref<EntityStore> selfRef;
    private float yaw = 0f;
    private float pitch = 0f;
    private double distance = 2d;
    private Boolean isBeingDragged = false;
    /** Used for positioning child glyphs relative to their parent */
    private Vector3f offset = new Vector3f(0, 0, 0);
    private float scale = 1f;

    /** Execution Context items */
    private List<Integer> numbers = new ArrayList<>();
    private List<Integer> variables = new ArrayList<>();

    // for the codec - do not use
    public GlyphComponent() {
    }

    public GlyphComponent(@Nonnull String glyphId) {
        this.glyphId = glyphId;
        this.id = UUID.randomUUID();
    }

    public GlyphComponent(@Nonnull String glyphId, float accuracy, float speed) {
        this.glyphId = glyphId;
        this.accuracy = accuracy;
        this.speed = speed / Math.max(idealSpeed, 1); // normalize speed to idealSpeed
        this.id = UUID.randomUUID();
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

    /** Casting Context Items */

    public boolean isChild() {
        return rootRef != null;
    }

    @Nonnull
    public String getGlyphId() {
        return glyphId;
    }

    public void setGlyphId(@Nonnull String glyphId) {
        this.glyphId = glyphId;
    }

    @Nullable
    public Ref<EntityStore> getOwnerRef() {
        return ownerRef;
    }

    public void setOwnerRef(@Nullable Ref<EntityStore> ownerRef) {
        this.ownerRef = ownerRef;
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

    public void setChildren(List<GlyphComponent> children) {
        this.children = children;
    }

    public void addChild(GlyphComponent child) {
        this.children.add(child);
    }

    public void removeChild(GlyphComponent child) {
        this.children.remove(child);
    }

    public void removeChild(UUID id) {
        this.children.removeIf(child -> child.getId().equals(id));
    }

    public List<GlyphComponent> getAllDescendants() {
        List<GlyphComponent> descendants = new ArrayList<>();
        for (GlyphComponent child : children) {
            descendants.add(child);
            descendants.addAll(child.getAllDescendants());
        }
        return descendants;
    }

    public boolean isHex() {
        return !children.isEmpty();
    }

    public boolean isLeaf() {
        return children.isEmpty();
    }

    /** Positioning */

    public float getYaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public SphericalPosition getSphericalPosition() {
        return new SphericalPosition(yaw, pitch, distance);
    }

    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public void setDragState(Boolean isBeingDragged) {
        this.isBeingDragged = isBeingDragged;
    }

    public boolean isBeingDragged() {
        return isBeingDragged;
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

    /** Attributes */

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

    /** Casting Context Items */
    public List<Integer> getNumbers() {
        return numbers;
    }

    public void setNumbers(List<Integer> numbers) {
        this.numbers = numbers;
    }

    public List<Integer> getVariables() {
        return variables;
    }

    public void setVariables(List<Integer> variables) {
        this.variables = variables;
    }

    @Nonnull
    @Override
    public GlyphComponent clone() {
        GlyphComponent copy = new GlyphComponent();
        copy.glyphId = this.glyphId;
        copy.ownerRef = this.ownerRef;
        copy.id = this.id;
        copy.yaw = this.yaw;
        copy.pitch = this.pitch;
        copy.distance = this.distance;
        copy.scale = this.scale;
        copy.accuracy = this.accuracy;
        copy.speed = this.speed;
        copy.children = new ArrayList<>();
        copy.idealSpeed = this.idealSpeed;
        for (GlyphComponent child : this.children) {
            copy.children.add(child.clone());
        }
        return copy;
    }
}
