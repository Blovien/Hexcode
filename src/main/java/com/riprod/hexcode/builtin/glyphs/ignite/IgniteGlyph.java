package com.riprod.hexcode.builtin.glyphs.ignite;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.OverlapBehavior;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.api.event.GlyphFizzleEvent;
import com.riprod.hexcode.builtin.glyphs.ignite.style.IgniteStyle;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.state.execution.HexExecuter;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.utils.HexDirectionUtil;
import com.riprod.hexcode.utils.HexVarUtil;

public class IgniteGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    @Override
public String getId() { return ID; };

public static final String ID = "Ignite";

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexVar targets = glyph.readSlot(IgniteGlyphSlots.TARGET, hexContext);

        if (targets == null) {
            HexExecuter.fail(glyph, hexContext, GlyphFizzleEvent.Reason.HANDLER_FAILED,
                    "Target required");
            return;
        }

        double duration = HexVarUtil.numberOrDefault(
                glyph.readSlot(IgniteGlyphSlots.DURATION, hexContext), 5.0);

        EntityEffect burnEffect = EntityEffect.getAssetMap().getAsset("Burn");
        if (burnEffect == null) {
            HexExecuter.fail(glyph, hexContext, GlyphFizzleEvent.Reason.HANDLER_FAILED,
                    "Missing asset Burn");
            return;
        }

        CommandBuffer<EntityStore> accessor = hexContext.getAccessor();

        EntityVar entityVar = HexVarUtil.resolveEntityVar(targets, hexContext);
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
                            IgniteStyle.render(tc.getPosition(), hexContext, accessor);
                        }
                    }
                } catch (Exception e) {
                    LOGGER.atWarning().log("ignite: failed on entity: %s", e.getMessage());
                }
            }
        }

        HexExecuter.continueFromSlot(glyph, Glyph.NEXT_SLOT, hexContext);
    }
}
