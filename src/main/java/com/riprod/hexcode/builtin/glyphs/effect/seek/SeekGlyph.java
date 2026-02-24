package com.riprod.hexcode.builtin.glyphs.effect.seek;

import java.util.ArrayList;
import java.util.List;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.riprod.hexcode.core.glyphs.component.Glyph;
import com.riprod.hexcode.core.execution.Executor;
import com.riprod.hexcode.core.execution.component.HexContext;
import com.riprod.hexcode.core.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.glyphs.variables.BlockVar;
import com.riprod.hexcode.core.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.glyphs.variables.HexVar;
import com.riprod.hexcode.utils.SpellVarUtil;

public class SeekGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "Glyph_Seek";
    private static final int DEFAULT_BEAM_LENGTH = 32;

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexVar posVar = glyph.getInput(0, hexContext);
        HexVar rotVar = glyph.getInput(1, hexContext);
        if (rotVar == null || rotVar.size() == 0) {
            rotVar = posVar;
        }

        Vector3d origin = SpellVarUtil.resolveEyePosition(posVar, hexContext.getAccessor());
        if (origin == null) {
            LOGGER.atWarning().log("seek glyph: could not resolve origin position");
            Executor.continueExecution(glyph.getNext(), hexContext);
            return;
        }

        Vector3d direction = SpellVarUtil.resolveDirection(rotVar, origin, hexContext.getAccessor());
        if (direction == null) {
            LOGGER.atWarning().log("seek glyph: could not resolve direction");
            Executor.continueExecution(glyph.getNext(), hexContext);
            return;
        }

        int beamLength = (int) SpellVarUtil.resolveNumberOrDefault(
                glyph.getInput(2, hexContext), (double) DEFAULT_BEAM_LENGTH).doubleValue();

        Vector3f rotation = Vector3f.lookAt(direction);
        Transform transform = new Transform(origin, rotation);

        Vector3d blockHitLocation = TargetUtil.getTargetLocation(transform, blockId -> blockId != 0,
                beamLength, hexContext.getAccessor());

        Ref<EntityStore> entityHit = null;
        double blockHitDistSq = Double.MAX_VALUE;
        double entityHitDistSq = Double.MAX_VALUE;

        if (blockHitLocation != null) {
            blockHitDistSq = new Vector3d(origin).subtract(blockHitLocation).squaredLength();
        }

        if (posVar instanceof EntityVar entityVar && entityVar.size() > 0) {
            Ref<EntityStore> sourceRef = entityVar.getRef(0, hexContext.getAccessor());
            if (sourceRef != null && sourceRef.isValid()) {
                entityHit = TargetUtil.getTargetEntity(sourceRef, (float) beamLength, hexContext.getAccessor());
                if (entityHit != null) {
                    Vector3d entityPos = hexContext.getAccessor().getComponent(entityHit,
                            TransformComponent.getComponentType()).getPosition();
                    entityHitDistSq = new Vector3d(origin).subtract(entityPos).squaredLength();
                }
            }
        }

        int outputSlot = glyph.getOutput(0, hexContext);
        Vector3d endPoint;
        SeekGlyphStyle.HitType hitType;

        if (entityHit != null && entityHitDistSq < blockHitDistSq) {
            UUIDComponent uuidComp = hexContext.getAccessor().getComponent(entityHit, UUIDComponent.getComponentType());
            EntityVar resultVar = new EntityVar(EntityVar.createRef(uuidComp.getUuid(), entityHit));
            hexContext.setVariable(outputSlot, resultVar);
            endPoint = hexContext.getAccessor().getComponent(entityHit,
                    TransformComponent.getComponentType()).getPosition();
            hitType = SeekGlyphStyle.HitType.ENTITY;
        } else if (blockHitLocation != null) {
            BlockVar resultVar = new BlockVar(new ArrayList<>(List.of(blockHitLocation.toVector3i())));
            hexContext.setVariable(outputSlot, resultVar);
            endPoint = blockHitLocation;
            hitType = SeekGlyphStyle.HitType.BLOCK;
        } else {
            endPoint = new Vector3d(origin).add(new Vector3d(direction).scale(beamLength));
            hitType = SeekGlyphStyle.HitType.MISS;
        }

        SeekGlyphStyle.render(origin, endPoint, hitType, hexContext.getAccessor());

        Executor.continueExecution(glyph.getNext(), hexContext);
    }
}
