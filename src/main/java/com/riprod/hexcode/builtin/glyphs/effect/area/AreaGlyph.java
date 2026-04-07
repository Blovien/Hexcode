package com.riprod.hexcode.builtin.glyphs.effect.area;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.riprod.hexcode.builtin.glyphs.effect.area.style.AreaStyle;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.registry.GlyphAsset;
import com.riprod.hexcode.core.common.glyphs.variables.BlockVar;
import com.riprod.hexcode.core.common.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.common.glyphs.variables.NumberVar;
import com.hypixel.hytale.server.core.entity.reference.PersistentRef;
import com.riprod.hexcode.core.state.execution.Executor;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.core.state.execution.component.VolatilityTracker;
import com.riprod.hexcode.utils.SpellVarUtil;

public class AreaGlyph implements GlyphHandler {
    public static final String ID = "Glyph_Area";

    private static final double DEFAULT_RADIUS = 5.0;
    private static final float VOLATILITY_HARSHNESS = 0.6f;

    @Override
    public boolean resolveVolatility(Glyph glyph, HexContext hexContext) {
        VolatilityTracker tracker = hexContext.getVolatilityTracker();
        if (tracker == null) return true;
        float chance = tracker.computeSuccessChance(glyph) * VOLATILITY_HARSHNESS;
        chance = Math.max(0f, Math.min(1f, chance));
        float roll = ThreadLocalRandom.current().nextFloat();
        tracker.incrementGlyphType(glyph.getGlyphId());
        return roll < chance;
    }

    @Override
    public boolean resolveMana(Glyph glyph, HexContext hexContext) {
        GlyphAsset asset = GlyphAsset.getAssetMap().getAsset(glyph.getGlyphId());
        if (asset == null) return true;

        double radius = SpellVarUtil.resolveNumberOrDefault(
                glyph.resolveSlot("radius", hexContext), DEFAULT_RADIUS);
        float radiusFactor = (float) ((radius * radius) / (DEFAULT_RADIUS * DEFAULT_RADIUS));

        float baseCost = asset.getManaConsumption()
                * ((1 - glyph.getEfficiency()) * 0.25f + 0.75f);

        VolatilityTracker tracker = hexContext.getVolatilityTracker();
        float castMultiplier = (tracker != null) ? tracker.getManaCostMultiplier() : 1.0f;
        float finalCost = baseCost * castMultiplier * radiusFactor;

        return hexContext.getRoot().tryConsumeMana(finalCost, hexContext.getAccessor());
    }

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexVar centerVar = glyph.resolveSlot("center", hexContext);
        double radius = SpellVarUtil.resolveNumberOrDefault(
                glyph.resolveSlot("radius", hexContext), DEFAULT_RADIUS);

        CommandBuffer<EntityStore> accessor = hexContext.getAccessor();
        Vector3d center = SpellVarUtil.resolvePosition(centerVar, accessor);

        if (center == null) {
            Executor.continueFromSlot(glyph, Glyph.NEXT_SLOT, hexContext);
            return;
        }

        Integer outputSlot = glyph.getSlotIndex("result", hexContext);
        Integer numTargetsSlot = glyph.getSlotIndex("num_targets", hexContext);

        if (centerVar instanceof BlockVar) {
            List<Vector3i> blocks = gatherBlocks(center, radius, accessor);
            AreaStyle.render(center, radius, hexContext.getColors(), accessor);

            if (blocks.isEmpty()) {
                Executor.continueFromSlot(glyph, Glyph.NEXT_SLOT, hexContext);
                return;
            }

            for (Vector3i pos : blocks) {
                HexContext copy = hexContext.copy();
                if (outputSlot != null) {
                    copy.setVariable(outputSlot, new BlockVar(pos));
                }
                if (numTargetsSlot != null) {
                    copy.setVariable(numTargetsSlot, new NumberVar(blocks.size()));
                }
                Executor.continueFromSlot(glyph, Glyph.NEXT_SLOT, copy);
            }
        } else {
            List<PersistentRef> entities = gatherEntities(center, radius, hexContext);
            AreaStyle.render(center, radius, hexContext.getColors(), accessor);

            if (entities.isEmpty()) {
                Executor.continueFromSlot(glyph, Glyph.NEXT_SLOT, hexContext);
                return;
            }

            for (PersistentRef ref : entities) {
                HexContext copy = hexContext.copy();
                if (outputSlot != null) {
                    copy.setVariable(outputSlot, new EntityVar(ref));
                }
                if (numTargetsSlot != null) {
                    copy.setVariable(numTargetsSlot, new NumberVar(entities.size()));
                }
                Executor.continueFromSlot(glyph, Glyph.NEXT_SLOT, copy);
            }
        }
    }

    private List<PersistentRef> gatherEntities(Vector3d center, double radius, HexContext hexContext) {
        CommandBuffer<EntityStore> accessor = hexContext.getAccessor();
        Ref<EntityStore> casterRef = hexContext.getCasterRef();
        List<PersistentRef> gathered = new ArrayList<>();

        List<Ref<EntityStore>> nearby = TargetUtil.getAllEntitiesInSphere(center, radius, accessor);
        for (Ref<EntityStore> ref : nearby) {
            if (ref == null || !ref.isValid()) continue;
            if (ref.equals(casterRef)) continue;

            UUIDComponent uuid = accessor.getComponent(ref, UUIDComponent.getComponentType());
            if (uuid == null) continue;

            gathered.add(EntityVar.createRef(uuid.getUuid(), ref));
        }

        return gathered;
    }

    private List<Vector3i> gatherBlocks(Vector3d center, double radius,
            CommandBuffer<EntityStore> accessor) {
        World world = accessor.getExternalData().getWorld();
        List<Vector3i> gathered = new ArrayList<>();
        int r = (int) Math.ceil(radius);
        double radiusSq = radius * radius;

        int cx = (int) Math.floor(center.x);
        int cy = (int) Math.floor(center.y);
        int cz = (int) Math.floor(center.z);

        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dy * dy + dz * dz > radiusSq) continue;

                    int bx = cx + dx;
                    int by = cy + dy;
                    int bz = cz + dz;

                    int blockId = world.getBlock(bx, by, bz);
                    if (blockId == BlockType.EMPTY_ID) continue;

                    gathered.add(new Vector3i(bx, by, bz));
                }
            }
        }

        return gathered;
    }
}
