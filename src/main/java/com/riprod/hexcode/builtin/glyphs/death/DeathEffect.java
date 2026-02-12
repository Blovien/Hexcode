package com.riprod.hexcode.builtin.glyphs.death;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.builtin.glyphs.GlyphConstants;

public final class DeathEffect {
    private DeathEffect() {}

    public static void applyDamage(Ref<EntityStore> targetRef, Ref<EntityStore> casterRef, ComponentAccessor<EntityStore> accessor) {
        Damage damage = new Damage(new Damage.EntitySource(casterRef), DamageCause.PHYSICAL, GlyphConstants.DEATH_DAMAGE);
        DamageSystems.executeDamage(targetRef, accessor, damage);
    }
}
