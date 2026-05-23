package com.riprod.hexcode.utils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Rotation3f;

import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector3i;
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
            Rotation3f bodyRot = accessor.getComponent(entityRef, TransformComponent.getComponentType()).getRotation();
            return new RotationVar(bodyRot).forward();
        }
        if (var instanceof RotationVar rotVar && rotVar.getValue() != null) {
            return rotVar.forward();
        }
        if (var instanceof PositionVar posVar && posVar.getValue() != null) {
            if (posVar.isAbsolute()) {
                if (sourcePosition != null) {
                    Vector3d dir = new Vector3d(posVar.getValue()).sub(sourcePosition);
                    if (dir.length() > 1e-9) return dir.normalize();
                }
                return null;
            }
            Vector3d offset = new Vector3d(posVar.getValue());
            if (offset.length() < 1e-9) return null;
            return offset.normalize();
        }
        if (var instanceof BlockVar blockVar) {
            Vector3i bv = blockVar.getValue();
            if (bv != null && sourcePosition != null) {
                Vector3d blockCenter = new Vector3d(bv.x + 0.5, bv.y + 0.5, bv.z + 0.5);
                Vector3d dir = blockCenter.sub(sourcePosition);
                if (dir.length() > 1e-9) return dir.normalize();
            }
            return blockVar.toRotation(accessor).forward();
        }
        return null;
    }

    @Nullable
    public static Rotation3f resolveRotation(@Nullable HexVar var,
            @Nonnull ComponentAccessor<EntityStore> accessor) {
        Vector3d dir = resolveDirection(var, null, accessor);
        if (dir == null) return null;
        float yaw = (float) Math.atan2(-dir.x, -dir.z);
        float pitch = (float) Math.asin(Math.max(-1.0, Math.min(1.0, -dir.y)));
        return new Rotation3f(pitch, yaw, 0f);
    }
}
