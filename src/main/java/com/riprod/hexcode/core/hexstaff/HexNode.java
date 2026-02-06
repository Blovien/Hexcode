package com.riprod.hexcode.core.hexstaff;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class HexNode {

    public static final BuilderCodec<HexNode> CODEC = BuilderCodec
            .builder(HexNode.class, HexNode::new)
            .append(new KeyedCodec<>("GlyphId", Codec.STRING),
                    (c, v) -> c.glyphId = v,
                    c -> c.glyphId)
            .add()
            .append(new KeyedCodec<>("Accuracy", Codec.FLOAT),
                    (c, v) -> c.accuracy = v,
                    c -> c.accuracy)
            .add()
            .append(new KeyedCodec<>("Speed", Codec.FLOAT),
                    (c, v) -> c.speed = v,
                    c -> c.speed)
            .add()
            .build();

    @Nonnull
    private String glyphId = "";
    @Nonnull
    private List<HexNode> children = new ArrayList<>();
    private float accuracy = 1.0f;
    private float speed = 1.0f;

    public HexNode() {
    }

    public HexNode(@Nonnull String glyphId, float accuracy, float speed) {
        this.glyphId = glyphId;
        this.accuracy = accuracy;
        this.speed = speed;
    }

    @Nonnull
    public String getGlyphId() {
        return glyphId;
    }

    public void setGlyphId(@Nonnull String glyphId) {
        this.glyphId = glyphId;
    }

    @Nonnull
    public List<HexNode> getChildren() {
        return children;
    }

    public void addChild(@Nonnull HexNode child) {
        // todo: add child to nested glyph structure
        this.children.add(child);
    }

    public float getAccuracy() {
        return accuracy;
    }

    public float getSpeed() {
        return speed;
    }

    public boolean hasChildren() {
        return !children.isEmpty();
    }

    @Nullable
    public HexNode clone() {
        HexNode copy = new HexNode();
        copy.glyphId = this.glyphId;
        copy.accuracy = this.accuracy;
        copy.speed = this.speed;
        for (HexNode child : this.children) {
            HexNode childCopy = child.clone();
            if (childCopy != null) {
                copy.children.add(childCopy);
            }
        }
        return copy;
    }
}
