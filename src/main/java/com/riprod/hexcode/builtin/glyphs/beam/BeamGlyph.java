package com.riprod.hexcode.builtin.glyphs.beam;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.riprod.hexcode.api.event.GlyphFizzleEvent;
import com.riprod.hexcode.builtin.glyphs.beam.style.BeamStyle;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.variables.BlockVar;
import com.riprod.hexcode.core.common.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.common.glyphs.variables.NumberVar;
import com.riprod.hexcode.core.state.execution.HexExecuter;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.utils.HexDirectionUtil;
import com.riprod.hexcode.utils.HexVarUtil;

public class BeamGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    @Override
public String getId() { return ID; };

public static final String ID = "Beam";
    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexVar posVar = glyph.readSlot(BeamGlyphSlots.SOURCE, hexContext);
        HexVar rotVar = glyph.readSlot(BeamGlyphSlots.ROTATION, hexContext);
        if (rotVar == null) rotVar = posVar;

        if (posVar == null) {
            HexExecuter.fail(glyph, hexContext, GlyphFizzleEvent.Reason.HANDLER_FAILED,
                    "Unable to find source position");
            return;
        }

        Vector3d origin = HexDirectionUtil.resolveEyePosition(posVar, hexContext.getAccessor());
        if (origin == null) {
            HexExecuter.fail(glyph, hexContext, GlyphFizzleEvent.Reason.HANDLER_FAILED,
                    "Source entity is invalid");
            return;
        }

        Vector3d direction = HexDirectionUtil.resolveDirection(rotVar, origin, hexContext.getAccessor());
        if (direction == null) {
            HexExecuter.fail(glyph, hexContext, GlyphFizzleEvent.Reason.HANDLER_FAILED,
                    "Rotation variable is not valid");
            return;
        }

        int beamLength = (int) HexVarUtil.number(
                glyph.readSlot(BeamGlyphSlots.RANGE, hexContext, new NumberVar(32.0))).doubleValue();

        Vector3f rotation = Vector3f.lookAt(direction);
        Transform transform = new Transform(new Vector3d(origin), rotation);

        Vector3d blockHitLocation = TargetUtil.getTargetLocation(transform, blockId -> blockId != 0,
                beamLength, hexContext.getAccessor());

        Ref<EntityStore> entityHit = null;
        double blockHitDist = Double.MAX_VALUE;
        double entityHitDist = Double.MAX_VALUE;

        if (blockHitLocation != null) {
            blockHitDist = new Vector3d(origin).subtract(blockHitLocation).length();
        }

        EntityVar sourceEntityVar = HexVarUtil.resolveEntityVar(posVar, hexContext);
        if (sourceEntityVar != null) {
            Ref<EntityStore> sourceRef = sourceEntityVar.getRef(hexContext.getAccessor());
            if (sourceRef != null && sourceRef.isValid()
                    && hexContext.getAccessor().getComponent(sourceRef,
                            com.hypixel.hytale.server.core.modules.entity.component.HeadRotation.getComponentType()) != null) {
                entityHit = TargetUtil.getTargetEntity(sourceRef, (float) beamLength, hexContext.getAccessor());
                if (entityHit != null) {
                    Vector3d entityPos = hexContext.getAccessor().getComponent(entityHit,
                            TransformComponent.getComponentType()).getPosition();
                    entityHitDist = new Vector3d(origin).subtract(entityPos).length();
                }
            }
        }

        Vector3d beamOrigin = new Vector3d(origin).add(new Vector3d(direction).scale(1.5));

        Vector3d endPoint;
        BeamStyle.HitType hitType;

        if (entityHit != null && entityHitDist < blockHitDist) {
            UUIDComponent uuidComp = hexContext.getAccessor().getComponent(entityHit, UUIDComponent.getComponentType());
            if (uuidComp == null) {
                endPoint = new Vector3d(origin).add(new Vector3d(direction).scale(beamLength));
                hitType = BeamStyle.HitType.MISS;
            } else {
                EntityVar resultVar = new EntityVar(EntityVar.createRef(uuidComp.getUuid(), entityHit));
                glyph.writeOutput(resultVar, hexContext);
                endPoint = hexContext.getAccessor().getComponent(entityHit,
                        TransformComponent.getComponentType()).getPosition();
                hitType = BeamStyle.HitType.ENTITY;
            }
        } else if (blockHitLocation != null) {
            BlockVar resultVar = new BlockVar(blockHitLocation.toVector3i());
            glyph.writeOutput(resultVar, hexContext);
            endPoint = blockHitLocation;
            hitType = BeamStyle.HitType.BLOCK;
        } else {
            endPoint = new Vector3d(origin).add(new Vector3d(direction).scale(beamLength));
            hitType = BeamStyle.HitType.MISS;
            BeamStyle.render(beamOrigin, endPoint, hitType, hexContext, hexContext.getAccessor());
            BlockVar resultVar = new BlockVar(endPoint.toVector3i());
            glyph.writeOutput(resultVar, hexContext);
        }

        BeamStyle.render(beamOrigin, endPoint, hitType, hexContext, hexContext.getAccessor());

        HexExecuter.continueFromSlot(glyph, Glyph.NEXT_SLOT, hexContext);
    }
}
