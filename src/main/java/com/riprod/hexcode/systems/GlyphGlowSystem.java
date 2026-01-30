package com.riprod.hexcode.systems;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;

import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.entity.HexNodeComponent;

/**
 * System that applies glow effects to glyph entities when they spawn.
 * Uses RefSystem callbacks to catch entity add events.
 */
public class GlyphGlowSystem extends RefSystem<EntityStore> {

    private static ComponentType<EntityStore, HexNodeComponent> hexNodeType;
    private static ComponentType<EntityStore, EffectControllerComponent> effectControllerType;

    public static void setHexNodeType(ComponentType<EntityStore, HexNodeComponent> type) {
        hexNodeType = type;
    }

    public static void setEffectControllerType(ComponentType<EntityStore, EffectControllerComponent> type) {
        effectControllerType = type;
    }

    @Override
    public Query<EntityStore> getQuery() {
        // Only trigger for entities with HexNodeComponent
        return hexNodeType;
    }

    @Override
    public void onEntityAdded(Ref<EntityStore> ref, AddReason reason,
            Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {
        // Get the HexNodeComponent to find the glyph's visual properties
        HexNodeComponent hexNode = store.getComponent(ref, hexNodeType);
        if (hexNode == null) {
            return;
        }

        // Get or add EffectControllerComponent
        EffectControllerComponent effectController = store.getComponent(ref, effectControllerType);
        if (effectController == null) {
            // Add the component via buffer (deferred)
            effectController = new EffectControllerComponent();
            buffer.addComponent(ref, effectControllerType, effectController);
            // Can't apply effect this tick - will need second pass or different approach
            return;
        }

        // Determine which glow effect to apply based on glyph color
        String effectId = getGlowEffectId(hexNode);
        EntityEffect effect = EntityEffect.getAssetMap().getAsset(effectId);

        if (effect != null) {
            int effectIndex = EntityEffect.getAssetMap().getIndex(effectId);
            effectController.addInfiniteEffect(ref, effectIndex, effect, store);
        }
    }

    @Override
    public void onEntityRemove(Ref<EntityStore> ref, RemoveReason reason,
            Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {
        // Effects auto-clear when entity is removed, no action needed
        return;
    }

    /**
     * Determine the glow effect ID based on the glyph's visual color.
     */
    private String getGlowEffectId(HexNodeComponent hexNode) {
        // Later, look up the Glyph visual 
        return "GlyphGlow";
    }
}