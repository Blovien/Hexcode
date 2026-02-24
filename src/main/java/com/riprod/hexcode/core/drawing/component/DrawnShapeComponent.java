package com.riprod.hexcode.core.drawing.component;

import java.util.List;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.Color;
import com.riprod.hexcode.core.drawing.registry.ShapeAsset;

public class DrawnShapeComponent {

    public static final BuilderCodec<DrawnShapeComponent> CODEC = BuilderCodec
                .builder(DrawnShapeComponent.class, DrawnShapeComponent::new)
                .append(new KeyedCodec<>("ShapeId", Codec.STRING),
                        (a, v) -> a.shapeId = v, a -> a.shapeId)
                .add()
                .append(new KeyedCodec<>("Size", Codec.FLOAT),
                        (a, v) -> a.relativeSize = v, a -> a.relativeSize)
                .add()
                .build();

    private String shapeId;
    
    // scoring
    private float volatility;
    private long efficiency;
    
    // shape calc
    private float size;
    private float relativeSize;
    private List<Vector3d> points;
    private Color color;
    private transient ShapeAsset shapeAsset;

    public DrawnShapeComponent(String shapeId, float size, float relativeSize, float volatility) {
        this.shapeId = shapeId;
        this.size = size;
        this.relativeSize = relativeSize;
        this.volatility = volatility;
    }

    public DrawnShapeComponent(String glyphId, float accuracy, ShapeAsset asset) {
        this(glyphId, 1.0f, 1.0f, accuracy);
        this.shapeAsset = asset;
    }

    private DrawnShapeComponent() {
    }

    public List<Vector3d> getPoints() {
        return points;
    }

    public Color getColor() {
        return color;
    }

    public void setPoints(List<Vector3d> points) {
        this.points = points;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public String getShapeId() {
        return shapeId;
    }

    public float getSize() {
        return size;
    }

    public float getVolatility() {
        return volatility;
    }

    public float getRelativeSize() {
        return relativeSize;
    }

    public void setShapeId(String glyphId) {
        this.shapeId = glyphId;
    }

    public void setSize(float size) {
        this.size = size;
    }

    public void setVolatility(float accuracy) {
        this.volatility = accuracy;
    }

    public void setRelativeSize(float relativeSize) {
        this.relativeSize = relativeSize;
    }

    public long getEfficiency() {
        return efficiency;
    }

    public void setSpeed(long speed) {
        this.efficiency = shapeAsset.getExpectedSpeed() / speed;
    }
}
