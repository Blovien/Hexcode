package com.riprod.hexcode.utils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.riprod.hexcode.core.common.glyphs.variables.BlockVar;
import com.riprod.hexcode.core.common.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.common.glyphs.variables.PositionVar;
import com.riprod.hexcode.core.common.glyphs.variables.RotationVar;

public class HexDirectionUtil {

    private HexDirectionUtil() {
    }

    @Nullable
    public static Vector3d resolveEyePosition(@Nullable HexVar var,
            @Nonnull ComponentAccessor<EntityStore> accessor) {
        if (var instanceof EntityVar entityVar
                && entityVar.getValue() != null
                && entityVar.getValue().isValid()) {
            Ref<EntityStore> entityRef = entityVar.getValue().getEntity(accessor);
            if (entityRef != null && entityRef.isValid()) {
                HeadRotation headRot = accessor.getComponent(entityRef, HeadRotation.getComponentType());
                if (headRot != null) {
                    return TargetUtil.getLook(entityRef, accessor).getPosition();
                }
                TransformComponent tc = accessor.getComponent(entityRef, TransformComponent.getComponentType());
                if (tc != null) return new Vector3d(tc.getPosition());
            }
        }
        return HexVarUtil.position(var, accessor);
    }

    @Nullable
    public static Vector3d resolveDirection(@Nullable HexVar var,
            @Nullable Vector3d sourcePosition,
            @Nonnull ComponentAccessor<EntityStore> accessor) {
        if (var == null) return null;

        if (var instanceof EntityVar entityVar
                && entityVar.getValue() != null
                && entityVar.getValue().isValid()) {
            Ref<EntityStore> entityRef = entityVar.getValue().getEntity(accessor);
            if (entityRef == null || !entityRef.isValid()) return null;
            try {
                HeadRotation headRot = accessor.getComponent(entityRef, HeadRotation.getComponentType());
                if (headRot != null) return headRot.getDirection();
            } catch (Exception e) {
            }
            Vector3f bodyRot = accessor.getComponent(entityRef, TransformComponent.getComponentType()).getRotation();
            return new RotationVar(bodyRot).forward();
        }
        if (var instanceof RotationVar rotVar && rotVar.getValue() != null) {
            return rotVar.forward();
        }
        if (var instanceof PositionVar posVar && posVar.getValue() != null) {
            if (posVar.isAbsolute()) {
                if (sourcePosition != null) {
                    return Vector3d.directionTo(sourcePosition, posVar.getValue());
                }
                return null;
            }
            return new Vector3d(posVar.getValue()).normalize();
        }
        if (sourcePosition != null && var instanceof BlockVar blockVar && blockVar.getValue() != null) {
            Vector3i bv = blockVar.getValue();
            Vector3d blockCenter = new Vector3d(bv.x + 0.5, bv.y + 0.5, bv.z + 0.5);
            return Vector3d.directionTo(sourcePosition, blockCenter);
        }
        return null;
    }

    @Nullable
    public static Vector3f resolveRotation(@Nullable HexVar var,
            @Nonnull ComponentAccessor<EntityStore> accessor) {
        Vector3d dir = resolveDirection(var, null, accessor);
        return dir != null ? Vector3f.lookAt(dir) : null;
    }
}
