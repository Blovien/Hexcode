package com.riprod.hexcode.builtin.glyphs.effect.ignite;

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
import com.riprod.hexcode.utils.SpellVarUtil;

public class IgniteGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "Glyph_Ignite";

    private static final double DEFAULT_DURATION = 5.0;

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexVar targets = glyph.resolveInput("target", hexContext);

        if (targets == null || targets.size() == 0) {
            Executor.continueExecution(glyph.getNext(), hexContext);
            return;
        }

        double duration = SpellVarUtil.resolveNumberOrDefault(
                glyph.resolveInput("duration", hexContext), DEFAULT_DURATION);

        EntityEffect burnEffect = EntityEffect.getAssetMap().getAsset("Burn");
        if (burnEffect == null) {
            LOGGER.atWarning().log("ignite glyph: Burn effect asset not found");
            Executor.continueExecution(glyph.getNext(), hexContext);
            return;
        }

        CommandBuffer<EntityStore> accessor = hexContext.getAccessor();

        if (targets instanceof EntityVar entityVar) {
            for (int i = 0; i < entityVar.size(); i++) {
                Ref<EntityStore> ref = entityVar.getRef(i, accessor);
                if (ref == null || !ref.isValid()) continue;

                try {
                    EffectControllerComponent controller = accessor.getComponent(
                            ref, EffectControllerComponent.getComponentType());
                    if (controller == null) continue;

                    controller.addEffect(ref, burnEffect, (float) duration,
                            OverlapBehavior.OVERWRITE, accessor);

                    TransformComponent tc = accessor.getComponent(ref,
                            TransformComponent.getComponentType());
                    if (tc != null) {
                        IgniteGlyphStyle.render(tc.getPosition(), accessor);
                    }
                } catch (Exception e) {
                    LOGGER.atWarning().log("ignite glyph: failed on entity %s: %s",
                            entityVar.getAt(i).getUuid(), e.getMessage());
                }
            }
        }

        Executor.continueExecution(glyph.getNext(), hexContext);
    }
}
