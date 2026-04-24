package com.riprod.hexcode.builtin.glyphs.gust;

import java.util.concurrent.ThreadLocalRandom;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.ExplosionConfig;
import com.hypixel.hytale.server.core.entity.ExplosionUtils;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.combat.PointKnockback;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.state.execution.HexExecuter;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.utils.SpellVarUtil;

public class GustGlyph implements GlyphHandler {
    @Override
public String getId() { return ID; };

public static final String ID = "Gust";

    private static final double MIN_KNOCKBACK_OFFSET = 0.1;
    private static final float BASE_DAMAGE = 2.0f;
    private static final float DAMAGE_SCALE = 0.2f;

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexVar centerVar = glyph.readSlot(GustGlyphSlots.CENTER, hexContext);
        double radius = SpellVarUtil.resolveNumberOrDefault(glyph.readSlot(GustGlyphSlots.RADIUS, hexContext), 5.0);
        double mag = SpellVarUtil.resolveNumberOrDefault(glyph.readSlot(GustGlyphSlots.MAGNITUDE, hexContext), 10.0);

        if (centerVar == null) {
            HexExecuter.continueFromSlot(glyph, Glyph.NEXT_SLOT, hexContext);
            return;
        }

        Vector3d center = SpellVarUtil.resolvePosition(centerVar, hexContext.getAccessor());
        if (center == null) {
            center = SpellVarUtil.resolvePosition(
                    hexContext.getVariable("1"), hexContext.getAccessor());
        }
        if (center == null) {
            HexExecuter.continueFromSlot(glyph, Glyph.NEXT_SLOT, hexContext);
            return;
        }

        CommandBuffer<EntityStore> accessor = hexContext.getAccessor();
        float damage = BASE_DAMAGE + (float) mag * DAMAGE_SCALE;

        Vector3d explosionCenter = new Vector3d(center).add(0, MIN_KNOCKBACK_OFFSET, 0);

        ExplosionConfig config = new ExplosionConfig() {
            {
                damageEntities = true;
                damageBlocks = false;
                entityDamageRadius = (float) radius;
                entityDamage = damage;
                entityDamageFalloff = 1.0f;
                knockback = new PointKnockback() {
                    {
                        force = (float) mag;
                        velocityY = (float) (mag * 0.3);
                        duration = 0;
                    }
                };
            }
        };

        ExplosionUtils.performExplosion(
                new Damage.EnvironmentSource("hex_gust"),
                explosionCenter,
                config,
                null,
                accessor,
                hexContext.getChunkAccessor());

        GustGlyphStyle.render(center, radius, hexContext.getColors(), accessor);

        HexExecuter.continueFromSlot(glyph, Glyph.NEXT_SLOT, hexContext);
    }
}
