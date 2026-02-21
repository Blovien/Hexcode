package com.riprod.hexcode.core.glyphs.utils;

import java.util.List;
import java.util.Vector;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.riprod.hexcode.core.glyphs.variables.BlockVar;
import com.riprod.hexcode.core.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.glyphs.variables.PositionVar;
import com.riprod.hexcode.core.glyphs.variables.RotationVar;
import com.riprod.hexcode.core.glyphs.variables.SpellVar;

public class SpellVarUtil {

    private SpellVarUtil() {
    }

    @Nullable
    public static Vector3d resolvePosition(@Nonnull List<SpellVar> vars,
            @Nonnull ComponentAccessor<EntityStore> accessor) {
        if (vars.isEmpty()) return null;
        SpellVar var = vars.get(0);

        if (var instanceof EntityVar entityVar && entityVar.ref != null && entityVar.ref.isValid()) {
            return accessor.getComponent(entityVar.ref, TransformComponent.getComponentType()).getPosition();
        }
        if (var instanceof BlockVar blockVar && blockVar.position != null) {
            return new Vector3d(blockVar.position.x + 0.5, blockVar.position.y + 0.5, blockVar.position.z + 0.5);
        }
        if (var instanceof PositionVar posVar && posVar.position != null) {
            return posVar.position;
        }
        return null;
    }

    @Nullable
    public static Vector3d resolveEyePosition(@Nonnull List<SpellVar> vars,
            @Nonnull ComponentAccessor<EntityStore> accessor) {
        if (vars.isEmpty()) return null;
        SpellVar var = vars.get(0);

        if (var instanceof EntityVar entityVar && entityVar.ref != null && entityVar.ref.isValid()) {
            return TargetUtil.getLook(entityVar.ref, accessor).getPosition();
        }
        return resolvePosition(vars, accessor);
    }

    @Nullable
    public static Vector3d resolveDirection(@Nonnull List<SpellVar> vars,
            @Nullable Vector3d sourcePosition,
            @Nonnull ComponentAccessor<EntityStore> accessor) {
        if (vars.isEmpty()) return null;
        SpellVar var = vars.get(0);

        if (var instanceof EntityVar entityVar && entityVar.ref != null && entityVar.ref.isValid()) {
            try {
                HeadRotation headRot = accessor.getComponent(entityVar.ref, HeadRotation.getComponentType());
                if (headRot != null) {
                    return headRot.getDirection();
                }
            } catch (Exception e) {
                // no head rotation component, fall back to transform
            }
            Vector3f bodyRot = accessor.getComponent(entityVar.ref, TransformComponent.getComponentType()).getRotation();
            return new Vector3d(bodyRot.getYaw(), bodyRot.getPitch());
        }
        if (var instanceof RotationVar rotVar && rotVar.rotation != null) {
            return new Vector3d(rotVar.rotation.getYaw(), rotVar.rotation.getPitch());
        }
        if (sourcePosition != null) {
            if (var instanceof BlockVar blockVar && blockVar.position != null) {
                Vector3d blockCenter = new Vector3d(blockVar.position.x + 0.5, blockVar.position.y + 0.5, blockVar.position.z + 0.5);
                return Vector3d.directionTo(sourcePosition, blockCenter);
            }
            if (var instanceof PositionVar posVar && posVar.position != null) {
                return Vector3d.directionTo(sourcePosition, posVar.position);
            }
        }
        return null;
    }

    @Nullable
    public static Vector3f resolveRotation(@Nonnull List<SpellVar> vars,
            @Nonnull ComponentAccessor<EntityStore> accessor) {
        Vector3d dir = resolveDirection(vars, null, accessor);
        return dir != null ? Vector3f.lookAt(dir) : null;
    }
}
