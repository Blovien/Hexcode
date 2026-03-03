package com.riprod.hexcode.utils;

import java.util.List;
import java.util.Vector;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.riprod.hexcode.core.common.glyphs.variables.BlockVar;
import com.riprod.hexcode.core.common.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.common.glyphs.variables.NumberVar;
import com.riprod.hexcode.core.common.glyphs.variables.PositionVar;
import com.riprod.hexcode.core.common.glyphs.variables.RotationVar;

public class SpellVarUtil {

    private SpellVarUtil() {
    }

    @Nullable
    public static Vector3d resolvePosition(@Nonnull HexVar var,
            @Nonnull ComponentAccessor<EntityStore> accessor) {
        return resolvePositionAt(var, 0, accessor);
    }

    @Nullable
    public static Vector3d resolvePositionAt(@Nonnull HexVar var, int index,
            @Nonnull ComponentAccessor<EntityStore> accessor) {
        if (index < 0 || index >= var.size()) return null;

        if (var instanceof EntityVar entityVar) {
            Ref<EntityStore> entityRef = entityVar.getRef(index, accessor);
            if (entityRef != null && entityRef.isValid()) {
                return accessor.getComponent(entityRef, TransformComponent.getComponentType()).getPosition();
            }
        }
        if (var instanceof BlockVar blockVar && blockVar.getAt(index) != null) {
            return new Vector3d(blockVar.getAt(index).x + 0.5, blockVar.getAt(index).y + 0.5,
                    blockVar.getAt(index).z + 0.5);
        }
        if (var instanceof PositionVar posVar && posVar.getAt(index) != null) {
            return posVar.getAt(index);
        }
        return null;
    }

    @Nullable
    public static Vector3d resolveEyePosition(@Nonnull HexVar var,
            @Nonnull ComponentAccessor<EntityStore> accessor) {

        if (var instanceof EntityVar entityVar && entityVar.getAt(0) != null && entityVar.getAt(0).isValid()) {
            Ref<EntityStore> entityRef = entityVar.getAt(0).getEntity(accessor);
            if (entityRef != null && entityRef.isValid()) {
                return TargetUtil.getLook(entityRef, accessor).getPosition();
            }
        }
        return resolvePosition(var, accessor);
    }

    @Nullable
    public static Vector3d resolveDirection(@Nonnull HexVar var,
            @Nullable Vector3d sourcePosition,
            @Nonnull ComponentAccessor<EntityStore> accessor) {

        if (var instanceof EntityVar entityVar && entityVar.getAt(0) != null && entityVar.getAt(0).isValid()) {
            Ref<EntityStore> entityRef = entityVar.getAt(0).getEntity(accessor);
            if (entityRef == null || !entityRef.isValid()) {
                return null;
            }

            try {
                HeadRotation headRot = accessor.getComponent(entityRef, HeadRotation.getComponentType());
                if (headRot != null) {
                    return headRot.getDirection();
                }
            } catch (Exception e) {
                // no head rotation component, fall back to transform
            }
            Vector3f bodyRot = accessor.getComponent(entityRef, TransformComponent.getComponentType())
                    .getRotation();
            return new Vector3d(bodyRot.getYaw(), bodyRot.getPitch());
        }
        if (var instanceof RotationVar rotVar && rotVar.getValues().get(0) != null) {
            return new Vector3d(rotVar.getValues().get(0).getYaw(), rotVar.getValues().get(0).getPitch());
        }
        if (sourcePosition != null) {
            if (var instanceof BlockVar blockVar && blockVar.getValues().get(0) != null) {
                Vector3d blockCenter = new Vector3d(blockVar.getValues().get(0).x + 0.5,
                        blockVar.getValues().get(0).y + 0.5,
                        blockVar.getValues().get(0).z + 0.5);
                return Vector3d.directionTo(sourcePosition, blockCenter);
            }
            if (var instanceof PositionVar posVar && posVar.getValues().get(0) != null) {
                return Vector3d.directionTo(sourcePosition, posVar.getValues().get(0));
            }
        }
        return null;
    }

    @Nullable
    public static Vector3f resolveRotation(@Nonnull HexVar vars,
            @Nonnull ComponentAccessor<EntityStore> accessor) {
        Vector3d dir = resolveDirection(vars, null, accessor);
        return dir != null ? Vector3f.lookAt(dir) : null;
    }

    @Nullable
    public static Double resolveNumberOrDefault(@Nonnull HexVar var, Double defaultValue) {
        Double result = resolveNumber(var);
        return result != null ? result : defaultValue;
    }

    @Nullable
    public static Double resolveNumber(@Nonnull HexVar var) {
        if (var == null || var.size() == 0)
            return null;

        if (var instanceof NumberVar numberVar) {
            return numberVar.getAt(0);
        }

        // if entity or block vars - return length
        if (var instanceof EntityVar || var instanceof BlockVar) {
            return (double) var.size();
        }

        return null;
    }
}
