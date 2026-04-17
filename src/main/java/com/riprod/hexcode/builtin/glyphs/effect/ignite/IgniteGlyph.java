package com.riprod.hexcode.builtin.glyphs.effect.ignite;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.OverlapBehavior;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.state.execution.Executor;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.builtin.glyphs.effect.ignite.style.IgniteStyle;
import com.riprod.hexcode.utils.SpellVarUtil;

public class IgniteGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "Glyph_Ignite";

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexVar targets = glyph.readSlot("target", hexContext);

        if (targets == null) {
            Executor.continueFromSlot(glyph, Glyph.NEXT_SLOT, hexContext);
            return;
        }

        double duration = SpellVarUtil.resolveNumberOrDefault(
                glyph.readSlot("duration", hexContext), 5.0);

        EntityEffect burnEffect = EntityEffect.getAssetMap().getAsset("Burn");
        if (burnEffect == null) {
            LOGGER.atWarning().log("ignite: burn effect asset not found");
            Executor.continueFromSlot(glyph, Glyph.NEXT_SLOT, hexContext);
            return;
        }

        CommandBuffer<EntityStore> accessor = hexContext.getAccessor();

        EntityVar entityVar = SpellVarUtil.resolveEntityVar(targets, hexContext);
        if (entityVar != null) {
            Ref<EntityStore> ref = entityVar.getRef(accessor);
            if (ref != null && ref.isValid()) {
                try {
                    EffectControllerComponent controller = accessor.getComponent(
                            ref, EffectControllerComponent.getComponentType());
                    if (controller != null) {
                        controller.addEffect(ref, burnEffect, (float) duration,
                                OverlapBehavior.OVERWRITE, accessor);

                        TransformComponent tc = accessor.getComponent(ref,
                                TransformComponent.getComponentType());
                        if (tc != null) {
                            IgniteStyle.render(tc.getPosition(), hexContext.getColors(), accessor);
                        }
                    }
                } catch (Exception e) {
                    LOGGER.atWarning().log("ignite: failed on entity: %s", e.getMessage());
                }
            }
        }

        Executor.continueFromSlot(glyph, Glyph.NEXT_SLOT, hexContext);
    }
}
