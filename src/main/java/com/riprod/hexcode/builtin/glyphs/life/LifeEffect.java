package com.riprod.hexcode.builtin.glyphs.life;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.builtin.glyphs.GlyphConstants;

public final class LifeEffect {
    private LifeEffect() {}

    public static void heal(Ref<EntityStore> entityRef, ComponentAccessor<EntityStore> accessor) {
        EntityStatMap stats = accessor.getComponent(entityRef, EntityStatMap.getComponentType());
        if (stats == null) return;

        stats.addStatValue(DefaultEntityStatTypes.getHealth(), GlyphConstants.LIFE_HEAL_AMOUNT);
    }
}
