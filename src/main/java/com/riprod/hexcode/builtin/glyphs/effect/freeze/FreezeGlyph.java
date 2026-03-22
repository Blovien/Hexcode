package com.riprod.hexcode.builtin.glyphs.effect.freeze;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.OverlapBehavior;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.state.execution.Executor;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.utils.SpellVarUtil;

public class FreezeGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "Glyph_Freeze";

    private static final double DEFAULT_DURATION = 3.0;
    private static final int ICE_RADIUS = 3;

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexVar targets = glyph.resolveInput("target", hexContext);

        if (targets == null || targets.size() == 0) {
            Executor.continueExecution(glyph.getNext(), hexContext);
            return;
        }

        double duration = SpellVarUtil.resolveNumberOrDefault(
                glyph.resolveInput("duration", hexContext), DEFAULT_DURATION);

        EntityEffect freezeEffect = EntityEffect.getAssetMap().getAsset("Freeze");
        if (freezeEffect == null) {
            LOGGER.atWarning().log("freeze glyph: Freeze effect asset not found");
            Executor.continueExecution(glyph.getNext(), hexContext);
            return;
        }

        CommandBuffer<EntityStore> accessor = hexContext.getAccessor();
        World world = accessor.getExternalData().getWorld();

        if (targets instanceof EntityVar entityVar) {
            for (int i = 0; i < entityVar.size(); i++) {
                Ref<EntityStore> ref = entityVar.getRef(i, accessor);
                if (ref == null || !ref.isValid()) continue;

                try {
                    EffectControllerComponent controller = accessor.getComponent(
                            ref, EffectControllerComponent.getComponentType());
                    if (controller == null) continue;

                    controller.addEffect(ref, freezeEffect, (float) duration,
                            OverlapBehavior.OVERWRITE, accessor);

                    TransformComponent tc = accessor.getComponent(ref,
                            TransformComponent.getComponentType());
                    if (tc != null) {
                        Vector3d pos = tc.getPosition();
                        placeIceSurface(world, pos);
                        FreezeGlyphStyle.render(pos, accessor);
                    }
                } catch (Exception e) {
                    LOGGER.atWarning().log("freeze glyph: failed on entity %s: %s",
                            entityVar.getAt(i).getUuid(), e.getMessage());
                }
            }
        }

        Executor.continueExecution(glyph.getNext(), hexContext);
    }

    private static void placeIceSurface(World world, Vector3d pos) {
        int footX = (int) Math.floor(pos.x);
        int footY = (int) Math.floor(pos.y) - 1;
        int footZ = (int) Math.floor(pos.z);

        for (int dx = -ICE_RADIUS; dx <= ICE_RADIUS; dx++) {
            for (int dz = -ICE_RADIUS; dz <= ICE_RADIUS; dz++) {
                if (dx * dx + dz * dz > ICE_RADIUS * ICE_RADIUS) continue;
                int bx = footX + dx;
                int bz = footZ + dz;
                int blockId = world.getBlock(bx, footY, bz);
                if (blockId != BlockType.EMPTY_ID) {
                    world.setBlock(bx, footY, bz, "Rock_Ice");
                }
            }
        }
    }
}
