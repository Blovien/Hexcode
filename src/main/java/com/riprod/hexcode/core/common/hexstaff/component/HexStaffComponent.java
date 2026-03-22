package com.riprod.hexcode.core.common.hexstaff.component;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.hexes.component.Hex;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class HexStaffComponent implements Component<EntityStore> {

    public static final BuilderCodec<HexStaffComponent> CODEC = BuilderCodec
            .builder(HexStaffComponent.class, HexStaffComponent::new)
            .append(new KeyedCodec<>("StyleId", Codec.STRING),
                    (c, v) -> c.styleId = v,
                    c -> c.styleId)
            .add()
            .append(new KeyedCodec<>("Hex", Hex.CODEC),
                    (c, v) -> c.hex = v,
                    c -> c.hex)
            .add()
            .append(new KeyedCodec<>("CastCount", Codec.INTEGER),
                    (c, v) -> c.castCount = v,
                    c -> c.castCount)
            .add()
            .append(new KeyedCodec<>("StaffModifier", Codec.FLOAT),
                    (c, v) -> c.staffModifier = v,
                    c -> c.staffModifier)
            .add()
            .build();

    private static ComponentType<EntityStore, HexStaffComponent> componentType;

    @Nonnull
    private String styleId = "ring";
    @Nullable
    private Hex hex;
    private int castCount = 0;
    private float staffModifier = 1.0f;

    public HexStaffComponent() {
    }

    public HexStaffComponent(@Nonnull HexStaffAsset staffAsset) {
        this.styleId = staffAsset.getCastStyleId();
        this.staffModifier = staffAsset.getStaffModifier();
    }

    public static void setComponentType(ComponentType<EntityStore, HexStaffComponent> type) {
        componentType = type;
    }

    public static ComponentType<EntityStore, HexStaffComponent> getComponentType() {
        return componentType;
    }

    @Nonnull
    public String getStyleId() {
        return styleId;
    }

    public void setStyleId(@Nonnull String styleId) {
        this.styleId = styleId;
    }

    @Nullable
    public Hex getActiveHex() {
        return hex;
    }

    public void setActiveHex(@Nullable Hex hex) {
        this.hex = hex;
    }

    public boolean hasActiveHex() {
        return hex != null;
    }

    public int getCastCount() {
        return castCount;
    }

    public void incrementCastCount() {
        this.castCount++;
    }

    public float getStaffModifier() {
        return staffModifier;
    }

    @Nonnull
    @Override
    public HexStaffComponent clone() {
        HexStaffComponent copy = new HexStaffComponent();
        copy.styleId = this.styleId;
        copy.hex = this.hex != null ? this.hex.clone() : null;
        copy.castCount = this.castCount;
        copy.staffModifier = this.staffModifier;
        return copy;
    }
}
