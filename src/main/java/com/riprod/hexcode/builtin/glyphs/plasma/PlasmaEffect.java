package com.riprod.hexcode.builtin.glyphs.plasma;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.OverlapBehavior;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.builtin.glyphs.GlyphConstants;

public final class PlasmaEffect {
    private PlasmaEffect() {}

    public static void apply(Ref<EntityStore> targetRef, Ref<EntityStore> casterRef, ComponentAccessor<EntityStore> accessor) {
        Damage damage = new Damage(new Damage.EntitySource(casterRef), DamageCause.PHYSICAL, GlyphConstants.PLASMA_DIRECT_DAMAGE);
        DamageSystems.executeDamage(targetRef, accessor, damage);

        EffectControllerComponent controller = accessor.getComponent(targetRef, EffectControllerComponent.getComponentType());
        if (controller == null) return;

        EntityEffect burnEffect = EntityEffect.getAssetMap().getAsset(GlyphConstants.PLASMA_EFFECT_ID);
        if (burnEffect == null) return;

        int effectIndex = EntityEffect.getAssetMap().getIndex(GlyphConstants.PLASMA_EFFECT_ID);
        controller.addEffect(targetRef, effectIndex, burnEffect, GlyphConstants.PLASMA_BURN_DURATION, OverlapBehavior.EXTEND, accessor);
    }
}
