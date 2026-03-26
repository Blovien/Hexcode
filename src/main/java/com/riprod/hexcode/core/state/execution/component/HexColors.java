package com.riprod.hexcode.core.state.execution.component;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.Color;
import com.hypixel.hytale.server.core.codec.ProtocolCodecs;

public class HexColors {

    private Color primaryColor;
    private Color secondaryColor;

    public Color getPrimaryColor() {
        return primaryColor;
    }

    public void setPrimaryColor(Color primaryColor) {
        this.primaryColor = primaryColor;
    }

    public Color getSecondaryColor() {
        return secondaryColor;
    }

    public void setSecondaryColor(Color secondaryColor) {
        this.secondaryColor = secondaryColor;
    }

    public static Vector3f toVector3f(Color color) {
        return new Vector3f((color.red & 0xFF) / 255f, (color.green & 0xFF) / 255f, (color.blue & 0xFF) / 255f);
    }

    public static final BuilderCodec<HexColors> CODEC = BuilderCodec
            .builder(HexColors.class, HexColors::new)
            .append(new KeyedCodec<>("PrimaryColor", ProtocolCodecs.COLOR),
                    (c, v) -> c.primaryColor = v,
                    c -> c.primaryColor)
            .add()
            .append(new KeyedCodec<>("SecondaryColor", ProtocolCodecs.COLOR),
                    (c, v) -> c.secondaryColor = v,
                    c -> c.secondaryColor)
            .add()
            .build();

    public HexColors clone() {
        HexColors copy = new HexColors();
        if (this.primaryColor != null) copy.primaryColor = this.primaryColor.clone();
        if (this.secondaryColor != null) copy.secondaryColor = this.secondaryColor.clone();
        return copy;
    }
}
