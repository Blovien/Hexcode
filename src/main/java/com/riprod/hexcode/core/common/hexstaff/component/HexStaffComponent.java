package com.riprod.hexcode.core.common.hexstaff.component;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class HexStaffComponent implements Component<EntityStore> {

    public static final BuilderCodec<HexStaffComponent> CODEC = BuilderCodec
            .builder(HexStaffComponent.class, HexStaffComponent::new)
            .append(new KeyedCodec<>("StyleId", Codec.STRING),
                    (c, v) -> c.styleId = v,
                    c -> c.styleId)
            .add()
            .append(new KeyedCodec<>("CastDecayRate", Codec.FLOAT),
                    (c, v) -> c.castDecayRate = v,
                    c -> c.castDecayRate)
            .add()
            .append(new KeyedCodec<>("VolatilityBonus", Codec.FLOAT),
                    (c, v) -> c.volatilityBonus = v,
                    c -> c.volatilityBonus)
            .add()
            .build();

    private static ComponentType<EntityStore, HexStaffComponent> componentType;

    @Nonnull
    private String styleId = "ring";
    private float castDecayRate = 0.05f;
    private float volatilityBonus = 0f;

    public HexStaffComponent() {
    }

    public HexStaffComponent(@Nonnull HexStaffAsset staffAsset) {
        this.styleId = staffAsset.getCastStyleId();
        this.castDecayRate = staffAsset.getCastDecayRate();
        this.volatilityBonus = staffAsset.getVolatilityBonus();
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

    public float getCastDecayRate() {
        return castDecayRate;
    }

    public float getVolatilityBonus() {
        return volatilityBonus;
    }

    @Nonnull
    @Override
    public HexStaffComponent clone() {
        HexStaffComponent copy = new HexStaffComponent();
        copy.styleId = this.styleId;
        copy.castDecayRate = this.castDecayRate;
        copy.volatilityBonus = this.volatilityBonus;
        return copy;
    }
}
