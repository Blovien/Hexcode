package com.riprod.hexcode.builtin.glyphs.cold;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.OverlapBehavior;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.builtin.glyphs.GlyphConstants;

public final class ColdEffect {
    private ColdEffect() {}

    public static void applyFreeze(Ref<EntityStore> entityRef, ComponentAccessor<EntityStore> accessor) {
        EffectControllerComponent controller = accessor.getComponent(entityRef, EffectControllerComponent.getComponentType());
        if (controller == null) return;

        EntityEffect freezeEffect = EntityEffect.getAssetMap().getAsset(GlyphConstants.COLD_EFFECT_ID);
        if (freezeEffect == null) return;

        int effectIndex = EntityEffect.getAssetMap().getIndex(GlyphConstants.COLD_EFFECT_ID);
        controller.addEffect(entityRef, effectIndex, freezeEffect, GlyphConstants.COLD_FREEZE_DURATION, OverlapBehavior.EXTEND, accessor);
    }
}
