package com.riprod.hexcode.builtin.glyphs.seek;

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
import com.riprod.hexcode.components.ExecutionContext;
import com.riprod.hexcode.components.Glyph;
import com.riprod.hexcode.components.HexContext;
import com.riprod.hexcode.core.execution.Executor;
import com.riprod.hexcode.core.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.glyphs.utils.SpellVarUtil;
import com.riprod.hexcode.core.glyphs.variables.BlockVar;
import com.riprod.hexcode.core.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.glyphs.variables.SpellVar;

public class SeekGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "Glyph_Seek";
    private static final int DEFAULT_BEAM_LENGTH = 32;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void execute(Glyph glyph, HexContext hexContext, ExecutionContext executionContext) {
        int posSlot = glyph.getNumber(1);
        int rotSlot = glyph.getNumber(2);

        List<SpellVar> posVars = executionContext.getVariable(posSlot);
        List<SpellVar> rotVars = executionContext.getVariable(rotSlot);
        if (rotVars.isEmpty()) {
            rotVars = posVars;
        }

        Vector3d origin = SpellVarUtil.resolveEyePosition(posVars, hexContext.accessor);
        if (origin == null) {
            LOGGER.atWarning().log("seek glyph: could not resolve origin position");
            Executor.continueExecution(hexContext, executionContext);
            return;
        }

        Vector3d direction = SpellVarUtil.resolveDirection(rotVars, origin, hexContext.accessor);
        if (direction == null) {
            LOGGER.atWarning().log("seek glyph: could not resolve direction");
            Executor.continueExecution(hexContext, executionContext);
            return;
        }

        int beamLength = glyph.getNumbers().containsKey(1) ? glyph.getNumber(1) : DEFAULT_BEAM_LENGTH;

        Vector3f rotation = Vector3f.lookAt(direction);
        Transform transform = new Transform(origin, rotation);

        Vector3d blockHitLocation = TargetUtil.getTargetLocation(transform, blockId -> blockId != 0,
                beamLength, hexContext.accessor);

        Ref<EntityStore> entityHit = null;
        double blockHitDistSq = Double.MAX_VALUE;
        double entityHitDistSq = Double.MAX_VALUE;

        if (blockHitLocation != null) {
            blockHitDistSq = new Vector3d(origin).subtract(blockHitLocation).squaredLength();
        }

        if (!posVars.isEmpty() && posVars.get(0) instanceof EntityVar entityVar
                && entityVar.ref != null && entityVar.ref.isValid()) {
            entityHit = TargetUtil.getTargetEntity(entityVar.ref, (float) beamLength, hexContext.accessor);
            if (entityHit != null) {
                Vector3d entityPos = hexContext.accessor.getComponent(entityHit,
                        TransformComponent.getComponentType()).getPosition();
                entityHitDistSq = new Vector3d(origin).subtract(entityPos).squaredLength();
            }
        }

        int outputSlot = glyph.getVariable(1);
        Vector3d endPoint;
        SeekGlyphStyle.HitType hitType;

        if (entityHit != null && entityHitDistSq < blockHitDistSq) {
            UUIDComponent uuidComp = hexContext.accessor.getComponent(entityHit, UUIDComponent.getComponentType());
            EntityVar resultVar = new EntityVar(uuidComp.getUuid(), entityHit);
            executionContext.setVariable(outputSlot, List.of(resultVar));
            endPoint = hexContext.accessor.getComponent(entityHit,
                    TransformComponent.getComponentType()).getPosition();
            hitType = SeekGlyphStyle.HitType.ENTITY;
        } else if (blockHitLocation != null) {
            BlockVar resultVar = new BlockVar(blockHitLocation.toVector3i());
            executionContext.setVariable(outputSlot, List.of(resultVar));
            endPoint = blockHitLocation;
            hitType = SeekGlyphStyle.HitType.BLOCK;
        } else {
            endPoint = new Vector3d(origin).add(new Vector3d(direction).scale(beamLength));
            hitType = SeekGlyphStyle.HitType.MISS;
        }

        SeekGlyphStyle.render(origin, endPoint, hitType, hexContext.accessor);

        Executor.continueExecution(hexContext, executionContext);
    }
}
