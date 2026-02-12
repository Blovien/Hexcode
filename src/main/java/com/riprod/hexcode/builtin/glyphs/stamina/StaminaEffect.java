package com.riprod.hexcode.builtin.glyphs.stamina;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.builtin.glyphs.GlyphConstants;

public final class StaminaEffect {
    private StaminaEffect() {}

    public static void convert(Ref<EntityStore> entityRef, ComponentAccessor<EntityStore> accessor) {
        EntityStatMap stats = accessor.getComponent(entityRef, EntityStatMap.getComponentType());
        if (stats == null) return;

        stats.subtractStatValue(DefaultEntityStatTypes.getMana(), GlyphConstants.STAMINA_MANA_COST);
        stats.addStatValue(DefaultEntityStatTypes.getStamina(), GlyphConstants.STAMINA_GAIN);
    }
}
