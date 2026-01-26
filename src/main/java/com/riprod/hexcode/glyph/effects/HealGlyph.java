package com.riprod.hexcode.glyph.effects;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.asset.GlyphAssetDefinition;
import com.riprod.hexcode.executing.SpellContext;
import com.riprod.hexcode.glyph.GlyphVisual;

/**
 * Heal effect glyph - restores health to target.
 *
 * <p>Asset-driven properties:
 * <ul>
 *   <li>baseHealing - amount of health to restore (default: 15.0)</li>
 * </ul>
 */
public class HealGlyph extends EffectGlyph {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /**
     * Create a heal glyph from an asset definition.
     *
     * @param assetDefinition The asset definition containing glyph properties
     */
    public HealGlyph(GlyphAssetDefinition assetDefinition) {
        super(assetDefinition, GlyphVisual.effect(GlyphVisual.COLOR_HEAL, "Heal"));
    }

    @Override
    protected void applyEffect(SpellContext context, Ref<EntityStore> target, float power) {
        Store<EntityStore> store = context.getStore();

        // Get asset-driven properties
        float baseHealing = getProperty("baseHealing", 15.0f);

        // Calculate final healing with power
        float actualHealing = baseHealing * power;

        LOGGER.atInfo().log("Applying heal effect: %.1f healing", actualHealing);

        // Get entity stats
        EntityStatMap statMap = store.getComponent(target, EntityStatMap.getComponentType());
        if (statMap == null) {
            LOGGER.atWarning().log("Target has no EntityStatMap, cannot heal");
            return;
        }

        // Get health stat index and add healing
        int healthIndex = DefaultEntityStatTypes.getHealth();
        statMap.addStatValue(healthIndex, actualHealing);

        LOGGER.atInfo().log("Healed target for %.1f health", actualHealing);
    }

    @Override
    protected void applyEffectAtPosition(SpellContext context, Vector3d position, float power) {
        // Heal doesn't make sense at a position without a target
        LOGGER.atInfo().log("Heal effect at position (%.1f, %.1f, %.1f) - no target to heal",
                position.x, position.y, position.z);
    }
}
