package com.riprod.hexcode.core.hexstaff.component;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.execution.component.HexGraph;
import com.riprod.hexcode.core.hexstaff.registry.HexStaffAsset;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class HexStaffComponent implements Component<EntityStore> {

    public static final BuilderCodec<HexStaffComponent> CODEC = BuilderCodec
            .builder(HexStaffComponent.class, HexStaffComponent::new)
            .append(new KeyedCodec<>("StyleId", Codec.STRING),
                    (c, v) -> c.styleId = v,
                    c -> c.styleId)
            .add()
            .append(new KeyedCodec<>("SpellGraph", HexGraph.CODEC),
                    (c, v) -> c.activeSpell = v,
                    c -> c.activeSpell)
            .add()
            .build();

    private static ComponentType<EntityStore, HexStaffComponent> componentType;

    @Nonnull
    private String styleId = "ring";
    @Nullable
    private HexGraph activeSpell;

    public HexStaffComponent() {
    }

    public HexStaffComponent(@Nonnull HexStaffAsset staffAsset) {
        this.styleId = staffAsset.getCastStyleId();
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
    public HexGraph getActiveSpell() {
        return activeSpell;
    }

    public void setActiveSpell(@Nullable HexGraph activeSpell) {
        this.activeSpell = activeSpell;
    }

    public boolean hasActiveSpell() {
        return activeSpell != null;
    }

    @Nonnull
    @Override
    public HexStaffComponent clone() {
        HexStaffComponent copy = new HexStaffComponent();
        copy.styleId = this.styleId;
        copy.activeSpell = this.activeSpell != null ? this.activeSpell.clone() : null;
        return copy;
    }
}
