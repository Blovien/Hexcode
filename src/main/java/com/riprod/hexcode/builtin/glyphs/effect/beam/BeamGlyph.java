package com.riprod.hexcode.builtin.glyphs.effect.beam;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.riprod.hexcode.builtin.glyphs.effect.beam.style.BeamStyle;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.variables.BlockVar;
import com.riprod.hexcode.core.common.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.state.execution.Executor;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.utils.SpellVarUtil;

public class BeamGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "Glyph_Beam";
    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexVar posVar = glyph.readSlot("entity", hexContext);
        HexVar rotVar = glyph.readSlot("rotation", hexContext);
        if (rotVar == null) rotVar = posVar;

        if (posVar == null) {
            LOGGER.atWarning().log("beam glyph: no source provided");
            Executor.continueFromSlot(glyph, Glyph.NEXT_SLOT, hexContext);
            return;
        }

        Vector3d origin = SpellVarUtil.resolveEyePosition(posVar, hexContext.getAccessor());
        if (origin == null) {
            LOGGER.atWarning().log("beam glyph: could not resolve origin position");
            Executor.continueFromSlot(glyph, Glyph.NEXT_SLOT, hexContext);
            return;
        }

        Vector3d direction = SpellVarUtil.resolveDirection(rotVar, origin, hexContext.getAccessor());
        if (direction == null) {
            LOGGER.atWarning().log("beam glyph: could not resolve direction");
            Executor.continueFromSlot(glyph, Glyph.NEXT_SLOT, hexContext);
            return;
        }

        int beamLength = (int) SpellVarUtil.resolveNumberOrDefault(
                glyph.readSlot("distance", hexContext),
                32.0).doubleValue();

        Vector3f rotation = Vector3f.lookAt(direction);
        Transform transform = new Transform(new Vector3d(origin), rotation);

        Vector3d blockHitLocation = TargetUtil.getTargetLocation(transform, blockId -> blockId != 0,
                beamLength, hexContext.getAccessor());

        Ref<EntityStore> entityHit = null;
        double blockHitDistSq = Double.MAX_VALUE;
        double entityHitDistSq = Double.MAX_VALUE;

        if (blockHitLocation != null) {
            blockHitDistSq = new Vector3d(origin).subtract(blockHitLocation).squaredLength();
        }

        EntityVar sourceEntityVar = SpellVarUtil.resolveEntityVar(posVar, hexContext);
        if (sourceEntityVar != null) {
            Ref<EntityStore> sourceRef = sourceEntityVar.getRef(hexContext.getAccessor());
            if (sourceRef != null && sourceRef.isValid()) {
                entityHit = TargetUtil.getTargetEntity(sourceRef, (float) beamLength, hexContext.getAccessor());
                if (entityHit != null) {
                    Vector3d entityPos = hexContext.getAccessor().getComponent(entityHit,
                            TransformComponent.getComponentType()).getPosition();
                    entityHitDistSq = new Vector3d(origin).subtract(entityPos).squaredLength();
                }
            }
        }

        Vector3d beamOrigin = new Vector3d(origin).add(new Vector3d(direction).scale(1.5));

        Vector3d endPoint;
        BeamStyle.HitType hitType;

        if (entityHit != null && entityHitDistSq < blockHitDistSq) {
            UUIDComponent uuidComp = hexContext.getAccessor().getComponent(entityHit, UUIDComponent.getComponentType());
            if (uuidComp == null) {
                endPoint = new Vector3d(origin).add(new Vector3d(direction).scale(beamLength));
                hitType = BeamStyle.HitType.MISS;
            } else {
                EntityVar resultVar = new EntityVar(EntityVar.createRef(uuidComp.getUuid(), entityHit));
                glyph.writeSlot("result", resultVar, hexContext);
                endPoint = hexContext.getAccessor().getComponent(entityHit,
                        TransformComponent.getComponentType()).getPosition();
                hitType = BeamStyle.HitType.ENTITY;
            }
        } else if (blockHitLocation != null) {
            BlockVar resultVar = new BlockVar(blockHitLocation.toVector3i());
            glyph.writeSlot("result", resultVar, hexContext);
            endPoint = blockHitLocation;
            hitType = BeamStyle.HitType.BLOCK;
        } else {
            endPoint = new Vector3d(origin).add(new Vector3d(direction).scale(beamLength));
            hitType = BeamStyle.HitType.MISS;
            BeamStyle.render(beamOrigin, endPoint, hitType, hexContext.getColors(), hexContext.getAccessor());
            Executor.fail(hexContext);
            return;
        }

        BeamStyle.render(beamOrigin, endPoint, hitType, hexContext.getColors(), hexContext.getAccessor());

        Executor.continueFromSlot(glyph, Glyph.NEXT_SLOT, hexContext);
    }
}
