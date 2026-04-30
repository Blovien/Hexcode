package com.riprod.hexcode.builtin.glyphs.area;

import java.util.ArrayList;
import java.util.List;


import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.riprod.hexcode.api.event.GlyphFizzleEvent;
import com.riprod.hexcode.builtin.glyphs.area.style.AreaStyle;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.registry.GlyphAsset;
import com.riprod.hexcode.core.common.glyphs.variables.BlockVar;
import com.riprod.hexcode.core.common.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.hypixel.hytale.server.core.entity.reference.PersistentRef;
import com.riprod.hexcode.core.state.execution.HexExecuter;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.core.state.execution.component.VolatilityTracker;
import com.riprod.hexcode.utils.HexDirectionUtil;
import com.riprod.hexcode.utils.HexVarUtil;

public class AreaGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    @Override
public String getId() { return ID; };

public static final String ID = "Area";

    private static final double DEFAULT_RADIUS = 5.0;
    private static final float VOLATILITY_COST_MULTIPLIER = 1.67f;

    @Override
    public boolean consumeVolatility(Glyph glyph, HexContext hexContext) {
        VolatilityTracker tracker = hexContext.getVolatilityTracker();
        if (tracker == null) return true;

        double radius = HexVarUtil.numberOrDefault(
                glyph.readSlot(AreaGlyphSlots.RADIUS, hexContext), DEFAULT_RADIUS);
        GlyphAsset asset = GlyphAsset.getAssetMap().getAsset(glyph.getGlyphId());
        float areaScale = computeAreaScale(radius, asset);

        int repeatCount = tracker.getGlyphUsage(glyph.getId());
        float cost = VolatilityTracker.computeGlyphCost(glyph, repeatCount)
                * VOLATILITY_COST_MULTIPLIER * areaScale;
        if (cost <= 0) return true;
        return tracker.consumeVolatility(cost);
    }

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexVar centerVar = glyph.readSlot(AreaGlyphSlots.CENTER, hexContext);
        double radius = HexVarUtil.numberOrDefault(
                glyph.readSlot(AreaGlyphSlots.RADIUS, hexContext), DEFAULT_RADIUS);

        CommandBuffer<EntityStore> accessor = hexContext.getAccessor();
        Vector3d center = HexVarUtil.position(centerVar, accessor);

        if (center == null) {
            HexExecuter.fail(glyph, hexContext, GlyphFizzleEvent.Reason.HANDLER_FAILED,
                    "Center Variable is not a valid position");
            return;
        }

        if (centerVar instanceof BlockVar) {
            List<Vector3i> blocks = gatherBlocks(center, radius, accessor);
            AreaStyle.render(center, radius, hexContext, accessor);

            if (blocks.isEmpty()) {
                HexExecuter.continueFromSlot(glyph, Glyph.NEXT_SLOT, hexContext);
                return;
            }

            for (Vector3i pos : blocks) {
                HexContext copy = hexContext.branch();
                glyph.writeOutput(new BlockVar(pos), copy);
                HexExecuter.continueFromSlot(glyph, Glyph.NEXT_SLOT, copy);
            }
        } else {
            List<PersistentRef> entities = gatherEntities(center, radius, hexContext);
            AreaStyle.render(center, radius, hexContext, accessor);

            if (entities.isEmpty()) {
                HexExecuter.continueFromSlot(glyph, Glyph.NEXT_SLOT, hexContext);
                return;
            }

            for (PersistentRef ref : entities) {
                HexContext copy = hexContext.branch();
                glyph.writeOutput(new EntityVar(ref), copy);
                HexExecuter.continueFromSlot(glyph, Glyph.NEXT_SLOT, copy);
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
