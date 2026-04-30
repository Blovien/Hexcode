package com.riprod.hexcode.builtin.glyphs.combust;

import java.util.List;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockGathering;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.OverlapBehavior;
import com.hypixel.hytale.server.core.entity.ExplosionConfig;
import com.hypixel.hytale.server.core.entity.ExplosionUtils;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.combat.PointKnockback;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.api.event.GlyphFizzleEvent;
import com.riprod.hexcode.builtin.glyphs.combust.style.CombustStyle;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.state.execution.HexExecuter;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.utils.HexVarUtil;
import com.hypixel.hytale.server.core.util.TargetUtil;

public class CombustGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    @Override
public String getId() { return ID; };

public static final String ID = "Combust";

    private static final double DEFAULT_RADIUS = 3.0;
    private static final double DEFAULT_MAGNITUDE = 10.0;
    private static final double LAVA_THRESHOLD = 15.0;
    private static final float BURN_DURATION = 5.0f;
    private static final float KNOCKBACK_Y_SCALE = 0.3f;
    private static final double CENTER_Y_OFFSET = 0.1;
    private static final String BURN_EFFECT_ID = "Burn";
    private static final String LAVA_BLOCK_ID = "Lava_Source";

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexVar centerVar = glyph.readSlot(CombustGlyphSlots.CENTER, hexContext);
        double radius = HexVarUtil.numberOrDefault(
                glyph.readSlot(CombustGlyphSlots.RADIUS, hexContext), DEFAULT_RADIUS);
        double magnitude = HexVarUtil.numberOrDefault(
                glyph.readSlot(CombustGlyphSlots.MAGNITUDE, hexContext), DEFAULT_MAGNITUDE);

        Vector3d center = resolveCenter(centerVar, hexContext);
        if (center == null) {
            HexExecuter.fail(glyph, hexContext, GlyphFizzleEvent.Reason.HANDLER_FAILED,
                    "Center position is required");
            return;
        }

        CommandBuffer<EntityStore> accessor = hexContext.getAccessor();
        World world = accessor.getExternalData().getWorld();

        EntityEffect burnEffect = EntityEffect.getAssetMap().getAsset(BURN_EFFECT_ID);

        center.add(0, CENTER_Y_OFFSET, 0);

        performExplosion(center, radius, magnitude, accessor, hexContext);
        destroySoftBlocks(world, center, radius);

        if (burnEffect != null) {
            applyBurn(accessor, center, radius, burnEffect);
        }

        if (magnitude >= LAVA_THRESHOLD) {
            placeLava(world, center);
            CombustStyle.renderLava(accessor, center, hexContext.getColors());
        }

        CombustStyle.renderExplosion(accessor, center, radius, hexContext.getColors());

        HexExecuter.continueFromSlot(glyph, Glyph.NEXT_SLOT, hexContext);
    }

    private Vector3d resolveCenter(HexVar centerVar, HexContext hexContext) {
        if (centerVar != null) {
            Vector3d pos = HexVarUtil.position(centerVar, hexContext.getAccessor());
            if (pos != null) return pos;
        }

        return HexVarUtil.position(
                hexContext.getVariable(Glyph.DEFAULT_SLOT), hexContext.getAccessor());
    }

    private void performExplosion(Vector3d center, double radius, double magnitude,
            CommandBuffer<EntityStore> accessor, HexContext hexContext) {
        ExplosionConfig config = new ExplosionConfig() {
            {
                damageEntities = true;
                damageBlocks = false;
                entityDamageRadius = (float) radius;
                entityDamage = (float) magnitude;
                entityDamageFalloff = 1.0f;
                knockback = new PointKnockback() {
                    {
                        force = (float) magnitude;
                        velocityY = (float) (magnitude * KNOCKBACK_Y_SCALE);
                        duration = 0;
                    }
                };
            }
        };

        ExplosionUtils.performExplosion(
                new Damage.EnvironmentSource("hex_combust"),
                center, config, null, accessor, hexContext.getChunkAccessor());
    }

    private void destroySoftBlocks(World world, Vector3d center, double radius) {
        int cx = (int) Math.floor(center.x);
        int cy = (int) Math.floor(center.y);
        int cz = (int) Math.floor(center.z);
        int r = (int) Math.ceil(radius);
        double radiusSq = radius * radius;

        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dy * dy + dz * dz > radiusSq) continue;

                    int bx = cx + dx;
                    int by = cy + dy;
                    int bz = cz + dz;

                    int blockId = world.getBlock(bx, by, bz);
                    if (blockId == BlockType.EMPTY_ID) continue;

                    BlockType blockType = BlockType.getAssetMap().getAsset(blockId);
                    if (blockType == null) continue;

                    BlockGathering gathering = blockType.getGathering();
                    if (gathering != null && gathering.isSoft()) {
                        world.setBlock(bx, by, bz, "Empty");
                    }
                }
            }
        }
    }

    private void applyBurn(CommandBuffer<EntityStore> accessor, Vector3d center,
            double radius, EntityEffect burnEffect) {
        List<Ref<EntityStore>> entities = TargetUtil.getAllEntitiesInSphere(center, radius, accessor);

        for (Ref<EntityStore> ref : entities) {
            if (ref == null || !ref.isValid()) continue;

            EffectControllerComponent controller = accessor.getComponent(
                    ref, EffectControllerComponent.getComponentType());
            if (controller == null) continue;

            controller.addEffect(ref, burnEffect, BURN_DURATION, OverlapBehavior.OVERWRITE, accessor);
        }
    }

    private void placeLava(World world, Vector3d center) {
        int lx = (int) Math.floor(center.x);
        int ly = (int) Math.floor(center.y);
        int lz = (int) Math.floor(center.z);
        world.setBlock(lx, ly, lz, LAVA_BLOCK_ID);
    }
}
