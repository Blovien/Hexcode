package com.riprod.hexcode.builtin.glyphs.fire;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.OverlapBehavior;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.builtin.glyphs.GlyphConstants;

public final class FireEffect {
    private FireEffect() {}

    public static void applyBurn(Ref<EntityStore> entityRef, ComponentAccessor<EntityStore> accessor) {
        EffectControllerComponent controller = accessor.getComponent(entityRef, EffectControllerComponent.getComponentType());
        if (controller == null) return;

        EntityEffect burnEffect = EntityEffect.getAssetMap().getAsset(GlyphConstants.FIRE_EFFECT_ID);
        if (burnEffect == null) return;

        int effectIndex = EntityEffect.getAssetMap().getIndex(GlyphConstants.FIRE_EFFECT_ID);
        controller.addEffect(entityRef, effectIndex, burnEffect, GlyphConstants.FIRE_BURN_DURATION, OverlapBehavior.EXTEND, accessor);
    }
}
