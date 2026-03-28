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

    public static boolean isVectorVar(@Nullable HexVar var) {
        return var instanceof PositionVar || var instanceof RotationVar || var instanceof EntityVar;
    }

    @Nullable
    public static Vector3d resolveAsPosition(@Nullable HexVar var,
            @Nonnull ComponentAccessor<EntityStore> accessor) {
        if (var == null || var.size() == 0) return null;
        if (var instanceof PositionVar posVar) return posVar.getAt(0);
        if (var instanceof RotationVar rotVar) {
            Vector3f r = rotVar.getAt(0);
            return new Vector3d(r.getYaw(), r.getPitch());
        }
        return resolvePosition(var, accessor);
    }

    @Nullable
    public static Vector3f resolveAsRotation(@Nullable HexVar var,
            @Nonnull ComponentAccessor<EntityStore> accessor) {
        if (var == null || var.size() == 0) return null;
        if (var instanceof RotationVar rotVar) return rotVar.getAt(0);
        if (var instanceof PositionVar posVar) {
            Vector3d p = posVar.getAt(0);
            return new Vector3f((float) p.getX(), (float) p.getY(), (float) p.getZ());
        }
        if (var instanceof EntityVar entityVar) {
            Ref<EntityStore> entityRef = entityVar.getRef(0, accessor);
            if (entityRef == null || !entityRef.isValid()) return null;
            try {
                HeadRotation headRot = accessor.getComponent(entityRef, HeadRotation.getComponentType());
                if (headRot != null) return headRot.getRotation();
            } catch (Exception e) {
                // no head rotation, fall back to transform
            }
            return accessor.getComponent(entityRef, TransformComponent.getComponentType()).getRotation();
        }
        return null;
    }

    public static double resolvePositionAxis(@Nullable HexVar var, int axis,
            @Nonnull ComponentAccessor<EntityStore> accessor) {
        if (var == null) return 0.0;
        if (isVectorVar(var)) {
            Vector3d pos = resolveAsPosition(var, accessor);
            if (pos == null) return 0.0;
            return switch (axis) {
                case 0 -> pos.getX();
                case 1 -> pos.getY();
                case 2 -> pos.getZ();
                default -> 0.0;
            };
        }
        Double num = resolveNumber(var);
        return num != null ? num : 0.0;
    }

    public static double resolveRotationAxis(@Nullable HexVar var, int axis,
            @Nonnull ComponentAccessor<EntityStore> accessor) {
        if (var == null) return 0.0;
        if (isVectorVar(var)) {
            Vector3f rot = resolveAsRotation(var, accessor);
            if (rot == null) return 0.0;
            return switch (axis) {
                case 0 -> rot.getX();
                case 1 -> rot.getY();
                case 2 -> rot.getZ();
                default -> 0.0;
            };
        }
        Double num = resolveNumber(var);
        return num != null ? num : 0.0;
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
                return accessor.getComponent(entityRef, TransformComponent.getComponentType()).getPosition().clone();
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
        if (var instanceof PositionVar posVar && posVar.size() > 0) {
            if (posVar.isAbsolute()) {
                if (sourcePosition != null) {
                    return Vector3d.directionTo(sourcePosition, posVar.getAt(0));
                }
                return null;
            }
            return new Vector3d(posVar.getAt(0)).normalize();
        }
        if (sourcePosition != null) {
            if (var instanceof BlockVar blockVar && blockVar.getValues().get(0) != null) {
                Vector3d blockCenter = new Vector3d(blockVar.getValues().get(0).x + 0.5,
                        blockVar.getValues().get(0).y + 0.5,
                        blockVar.getValues().get(0).z + 0.5);
                return Vector3d.directionTo(sourcePosition, blockCenter);
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
