package com.riprod.hexcode.core.drawing.component;

import java.util.List;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.Color;

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
    private float size;
    private float relativeSize;
    private float accuracy;
    private long speed;
    private List<Vector3d> points;
    private Color color;

    public DrawnShapeComponent(String shapeId, float size, float relativeSize, float accuracy) {
        this.shapeId = shapeId;
        this.size = size;
        this.relativeSize = relativeSize;
        this.accuracy = accuracy;
    }

    public DrawnShapeComponent(String glyphId, float accuracy) {
        this(glyphId, 1.0f, 1.0f, accuracy);
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

    public float getAccuracy() {
        return accuracy;
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

    public void setAccuracy(float accuracy) {
        this.accuracy = accuracy;
    }

    public void setRelativeSize(float relativeSize) {
        this.relativeSize = relativeSize;
    }

    public long getSpeed() {
        return speed;
    }

    public void setSpeed(long speed) {
        this.speed = speed;
    }
}
