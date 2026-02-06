package com.riprod.hexcode.core.hexstaff;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class HexStaffComponent implements Component<EntityStore> {

    public static final BuilderCodec<HexStaffComponent> CODEC = BuilderCodec
            .builder(HexStaffComponent.class, HexStaffComponent::new)
            .append(new KeyedCodec<>("StyleId", Codec.STRING),
                    (c, v) -> c.styleId = v,
                    c -> c.styleId)
            .add()
            .append(new KeyedCodec<>("ActiveSpellData", Codec.STRING),
                    (c, v) -> c.activeSpellData = v,
                    c -> c.activeSpellData)
            .add()
            .build();

    private static ComponentType<EntityStore, HexStaffComponent> componentType;

    @Nonnull
    private String styleId = "ring";
    @Nonnull
    private String activeSpellData = "";
    @Nullable
    private transient HexNode activeSpell;

    public HexStaffComponent() {
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
    public HexNode getActiveSpell() {
        return activeSpell;
    }

    public void setActiveSpell(@Nullable HexNode activeSpell) {
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
