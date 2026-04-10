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
import com.riprod.hexcode.core.common.glyphs.variables.NumberVar;
import com.riprod.hexcode.core.common.glyphs.variables.PositionVar;
import com.riprod.hexcode.core.common.glyphs.variables.RotationVar;

public class SpellVarUtil {

    private SpellVarUtil() {
    }

    public static boolean isVectorVar(@Nullable HexVar var) {
        return var instanceof PositionVar || var instanceof RotationVar || var instanceof EntityVar;
    }

    /**
     * If `var` is already an EntityVar, return it. If it's a NumberVar, treat the
     * number as a slot key and dereference via hexContext. Otherwise return null.
     * Lets handlers accept either a wired entity or a numeric slot reference uniformly.
     */
    @Nullable
    public static EntityVar resolveEntityVar(@Nullable HexVar var,
            @Nonnull com.riprod.hexcode.core.state.execution.component.HexContext ctx) {
        if (var instanceof EntityVar ev) return ev;
        if (var instanceof NumberVar nv) {
            HexVar deref = ctx.getVariable(String.valueOf((int) nv.getValue()));
            return deref instanceof EntityVar ? (EntityVar) deref : null;
        }
        return null;
    }

    @Nullable
    public static BlockVar resolveBlockVar(@Nullable HexVar var,
            @Nonnull com.riprod.hexcode.core.state.execution.component.HexContext ctx) {
        if (var instanceof BlockVar bv) return bv;
        if (var instanceof NumberVar nv) {
            HexVar deref = ctx.getVariable(String.valueOf((int) nv.getValue()));
            return deref instanceof BlockVar ? (BlockVar) deref : null;
        }
        return null;
    }

    @Nullable
    public static PositionVar resolvePositionVar(@Nullable HexVar var,
            @Nonnull com.riprod.hexcode.core.state.execution.component.HexContext ctx) {
        if (var instanceof PositionVar pv) return pv;
        if (var instanceof NumberVar nv) {
            HexVar deref = ctx.getVariable(String.valueOf((int) nv.getValue()));
            return deref instanceof PositionVar ? (PositionVar) deref : null;
        }
        return null;
    }

    @Nullable
    public static RotationVar resolveRotationVar(@Nullable HexVar var,
            @Nonnull com.riprod.hexcode.core.state.execution.component.HexContext ctx) {
        if (var instanceof RotationVar rv) return rv;
        if (var instanceof NumberVar nv) {
            HexVar deref = ctx.getVariable(String.valueOf((int) nv.getValue()));
            return deref instanceof RotationVar ? (RotationVar) deref : null;
        }
        return null;
    }

    @Nullable
    public static Vector3d resolveAsPosition(@Nullable HexVar var,
            @Nonnull ComponentAccessor<EntityStore> accessor) {
        if (var == null) return null;
        if (var instanceof PositionVar posVar) return posVar.getValue();
        if (var instanceof RotationVar rotVar) {
            Vector3f r = rotVar.getValue();
            if (r == null) return null;
            return new Vector3d(r.getYaw(), r.getPitch());
        }
        return resolvePosition(var, accessor);
    }

    @Nullable
    public static Vector3f resolveAsRotation(@Nullable HexVar var,
            @Nonnull ComponentAccessor<EntityStore> accessor) {
        if (var == null) return null;
        if (var instanceof RotationVar rotVar) return rotVar.getValue();
        if (var instanceof PositionVar posVar) {
            Vector3d p = posVar.getValue();
            if (p == null) return null;
            return new Vector3f((float) p.getX(), (float) p.getY(), (float) p.getZ());
        }
        if (var instanceof EntityVar entityVar) {
            Ref<EntityStore> entityRef = entityVar.getRef(accessor);
            if (entityRef == null || !entityRef.isValid()) return null;
            try {
                HeadRotation headRot = accessor.getComponent(entityRef, HeadRotation.getComponentType());
                if (headRot != null) return headRot.getRotation();
            } catch (Exception e) {
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
    public static Vector3d resolvePosition(@Nullable HexVar var,
            @Nonnull ComponentAccessor<EntityStore> accessor) {
        if (var == null) return null;

        if (var instanceof EntityVar entityVar) {
            Ref<EntityStore> entityRef = entityVar.getRef(accessor);
            if (entityRef != null && entityRef.isValid()) {
                return accessor.getComponent(entityRef, TransformComponent.getComponentType()).getPosition().clone();
            }
        }
        if (var instanceof BlockVar blockVar && blockVar.getValue() != null) {
            Vector3i pos = blockVar.getValue();
            return new Vector3d(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5);
        }
        if (var instanceof PositionVar posVar) {
            return posVar.getValue();
        }
        return null;
    }

    @Nullable
    public static Vector3d resolveEyePosition(@Nullable HexVar var,
            @Nonnull ComponentAccessor<EntityStore> accessor) {
        if (var instanceof EntityVar entityVar && entityVar.getValue() != null && entityVar.getValue().isValid()) {
            Ref<EntityStore> entityRef = entityVar.getValue().getEntity(accessor);
            if (entityRef != null && entityRef.isValid()) {
                return TargetUtil.getLook(entityRef, accessor).getPosition();
            }
        }
        return resolvePosition(var, accessor);
    }

    @Nullable
    public static Vector3d resolveDirection(@Nullable HexVar var,
            @Nullable Vector3d sourcePosition,
            @Nonnull ComponentAccessor<EntityStore> accessor) {
        if (var == null) return null;

        if (var instanceof EntityVar entityVar && entityVar.getValue() != null && entityVar.getValue().isValid()) {
            Ref<EntityStore> entityRef = entityVar.getValue().getEntity(accessor);
            if (entityRef == null || !entityRef.isValid()) return null;

            try {
                HeadRotation headRot = accessor.getComponent(entityRef, HeadRotation.getComponentType());
                if (headRot != null) return headRot.getDirection();
            } catch (Exception e) {
            }
            Vector3f bodyRot = accessor.getComponent(entityRef, TransformComponent.getComponentType())
                    .getRotation();
            return new Vector3d(bodyRot.getYaw(), bodyRot.getPitch());
        }
        if (var instanceof RotationVar rotVar && rotVar.getValue() != null) {
            return new Vector3d(rotVar.getValue().getYaw(), rotVar.getValue().getPitch());
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
    public static Vector3f resolveRotation(@Nullable HexVar vars,
            @Nonnull ComponentAccessor<EntityStore> accessor) {
        Vector3d dir = resolveDirection(vars, null, accessor);
        return dir != null ? Vector3f.lookAt(dir) : null;
    }

    public static Double resolveNumberOrDefault(@Nullable HexVar var, Double defaultValue) {
        Double result = resolveNumber(var);
        return result != null ? result : defaultValue;
    }

    @Nullable
    public static Double resolveNumber(@Nullable HexVar var) {
        if (var == null) return null;
        if (var instanceof NumberVar numberVar) {
            return numberVar.getValue();
        }
        return null;
    }
}
