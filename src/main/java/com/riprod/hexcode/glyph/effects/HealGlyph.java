package com.riprod.hexcode.glyph.effects;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.execution.ExecutionContext;
import com.riprod.hexcode.execution.TargetSet;
import com.riprod.hexcode.glyph.GlyphVisual;

import java.util.Set;

/**
 * Heal effect glyph - restores health to target.
 */
public class HealGlyph extends EffectGlyph {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static final String ID = "hexcode:heal";
    public static final int BASE_COST = 20;
    public static final float BASE_HEALING = 15.0f;

    public HealGlyph() {
        super(
            ID,
            "Heal",
            BASE_COST,
            GlyphVisual.effect(GlyphVisual.COLOR_HEAL, "heal"),
            Set.of("hexcode:power")
        );
    }

    @Override
    public void applyEffect(ExecutionContext ctx, TargetSet targets) {
        float healing = getModifiedAmount(ctx, BASE_HEALING);
        Store<EntityStore> store = ctx.getStore();

        LOGGER.atInfo().log("Applying heal effect: %.1f healing to %d targets",
                healing, targets.getEntityCount());

        // Apply to each target entity
        for (Ref<EntityStore> targetRef : targets.getEntities()) {
            EntityStatMap statMap = store.getComponent(targetRef, EntityStatMap.getComponentType());
            if (statMap == null) {
                LOGGER.atWarning().log("Target has no EntityStatMap, cannot heal");
                continue;
            }

            // Get health stat index and add healing
            int healthIndex = DefaultEntityStatTypes.getHealth();
            statMap.addStatValue(healthIndex, healing);

            LOGGER.atInfo().log("Healed target for %.1f health", healing);
        }
    }
}
