package com.riprod.hexcode.utils;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.glyphs.variables.BlockVar;
import com.riprod.hexcode.core.common.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.common.glyphs.variables.NumberVar;
import com.riprod.hexcode.core.common.glyphs.variables.PositionVar;
import com.riprod.hexcode.core.common.glyphs.variables.RotationVar;

public class HexMathUtil {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private HexMathUtil() {
    }

    public static HexVar add(HexVar a, HexVar b) {
        if (a instanceof NumberVar na && b instanceof NumberVar nb)
            return new NumberVar(na.getValue() + nb.getValue());
        if (a instanceof PositionVar pa && b instanceof PositionVar pb)
            return new PositionVar(new Vector3d(pa.getValue()).add(pb.getValue()), pa.isAbsolute() || pb.isAbsolute());
        if (a instanceof RotationVar ra && b instanceof RotationVar rb)
            return new RotationVar(new Vector3f(ra.getValue()).add(rb.getValue()));
        if (a instanceof PositionVar pa && b instanceof NumberVar nb)
            return new PositionVar(new Vector3d(pa.getValue()).add(nb.getValue()), pa.isAbsolute());
        if (a instanceof NumberVar na && b instanceof PositionVar pb)
            return new PositionVar(new Vector3d(pb.getValue()).add(na.getValue()), pb.isAbsolute());
        if (a instanceof RotationVar ra && b instanceof NumberVar nb)
            return new RotationVar(new Vector3f(ra.getValue()).add((float) nb.getValue(), (float) nb.getValue(), (float) nb.getValue()));
        if (a instanceof NumberVar na && b instanceof RotationVar rb)
            return new RotationVar(new Vector3f(rb.getValue()).add((float) na.getValue(), (float) na.getValue(), (float) na.getValue()));
        if (a instanceof EntityVar || a instanceof BlockVar || b instanceof EntityVar || b instanceof BlockVar) {
            return addViaPosition(a, b);
        }
        LOGGER.atWarning().log("add: unsupported types %s + %s", a.getClass().getSimpleName(), b.getClass().getSimpleName());
        return a;
    }

    public static HexVar negate(HexVar a) {
        if (a instanceof NumberVar na) return new NumberVar(-na.getValue());
        if (a instanceof PositionVar pa) {
            Vector3d v = pa.getValue();
            return new PositionVar(new Vector3d(-v.x, -v.y, -v.z), false);
        }
        if (a instanceof RotationVar ra) {
            Vector3f v = ra.getValue();
            return new RotationVar(new Vector3f(-v.x, -v.y, -v.z));
        }
        LOGGER.atWarning().log("negate: unsupported type %s", a.getClass().getSimpleName());
        return a;
    }

    public static HexVar subtract(HexVar a, HexVar b) {
        if (a == null || b == null) return a;
        if (a instanceof NumberVar na && b instanceof NumberVar nb)
            return new NumberVar(na.getValue() - nb.getValue());
        if (a instanceof PositionVar pa && b instanceof PositionVar pb)
            return new PositionVar(new Vector3d(pa.getValue()).subtract(pb.getValue()), pa.isAbsolute() && !pb.isAbsolute());
        if (a instanceof RotationVar ra && b instanceof RotationVar rb)
            return new RotationVar(new Vector3f(ra.getValue()).subtract(rb.getValue()));
        if (a instanceof PositionVar pa && b instanceof NumberVar nb)
            return new PositionVar(new Vector3d(pa.getValue()).subtract(nb.getValue()), pa.isAbsolute());
        if (a instanceof RotationVar ra && b instanceof NumberVar nb)
            return new RotationVar(new Vector3f(ra.getValue()).subtract((float) nb.getValue(), (float) nb.getValue(), (float) nb.getValue()));
        if (a instanceof EntityVar || a instanceof BlockVar || b instanceof EntityVar || b instanceof BlockVar) {
            return subtractViaPosition(a, b);
        }
        LOGGER.atWarning().log("subtract: unsupported types %s - %s", a.getClass().getSimpleName(), b.getClass().getSimpleName());
        return a;
    }

    public static HexVar multiply(HexVar a, HexVar b) {
        if (a instanceof NumberVar na && b instanceof NumberVar nb)
            return new NumberVar(na.getValue() * nb.getValue());
        if (a instanceof PositionVar pa && b instanceof PositionVar pb)
            return new PositionVar(new Vector3d(pa.getValue()).scale(pb.getValue()), false);
        if (a instanceof PositionVar pa && b instanceof NumberVar nb)
            return new PositionVar(new Vector3d(pa.getValue()).scale(nb.getValue()), false);
        if (a instanceof NumberVar na && b instanceof PositionVar pb)
            return new PositionVar(new Vector3d(pb.getValue()).scale(na.getValue()), false);
        if (a instanceof RotationVar ra && b instanceof NumberVar nb)
            return new RotationVar(new Vector3f(ra.getValue()).scale((float) nb.getValue()));
        if (a instanceof NumberVar na && b instanceof RotationVar rb)
            return new RotationVar(new Vector3f(rb.getValue()).scale((float) na.getValue()));
        if (a instanceof RotationVar ra && b instanceof RotationVar rb)
            return new RotationVar(new Vector3f(ra.getValue()).scale(rb.getValue()));
        LOGGER.atWarning().log("multiply: unsupported types %s * %s", a.getClass().getSimpleName(), b.getClass().getSimpleName());
        return a;
    }

    public static HexVar divide(HexVar a, HexVar b) {
        if (a instanceof NumberVar na && b instanceof NumberVar nb)
            return new NumberVar(nb.getValue() != 0 ? na.getValue() / nb.getValue() : na.getValue());
        if (a instanceof PositionVar pa && b instanceof NumberVar nb) {
            double s = nb.getValue();
            return new PositionVar(s != 0 ? new Vector3d(pa.getValue()).scale(1.0 / s) : new Vector3d(pa.getValue()), false);
        }
        if (a instanceof PositionVar pa && b instanceof PositionVar pb) {
            Vector3d va = pa.getValue(); Vector3d vb = pb.getValue();
            return new PositionVar(new Vector3d(
                    vb.x != 0 ? va.x / vb.x : va.x,
                    vb.y != 0 ? va.y / vb.y : va.y,
                    vb.z != 0 ? va.z / vb.z : va.z), false);
        }
        if (a instanceof RotationVar ra && b instanceof NumberVar nb) {
            double s = nb.getValue();
            return new RotationVar(s != 0 ? new Vector3f(ra.getValue()).scale((float) (1.0 / s)) : new Vector3f(ra.getValue()));
        }
        if (a instanceof RotationVar ra && b instanceof RotationVar rb) {
            Vector3f va = ra.getValue(); Vector3f vb = rb.getValue();
            return new RotationVar(new Vector3f(
                    vb.x != 0 ? va.x / vb.x : va.x,
                    vb.y != 0 ? va.y / vb.y : va.y,
                    vb.z != 0 ? va.z / vb.z : va.z));
        }
        LOGGER.atWarning().log("divide: unsupported types %s / %s", a.getClass().getSimpleName(), b.getClass().getSimpleName());
        return a;
    }

    private static HexVar addViaPosition(HexVar a, HexVar b) {
        LOGGER.atWarning().log("add: converting %s + %s to position math", a.getClass().getSimpleName(), b.getClass().getSimpleName());
        return a;
    }

    private static HexVar subtractViaPosition(HexVar a, HexVar b) {
        LOGGER.atWarning().log("subtract: converting %s - %s to position math", a.getClass().getSimpleName(), b.getClass().getSimpleName());
        return a;
    }

    public static HexVar addViaPosition(HexVar a, HexVar b, ComponentAccessor<EntityStore> accessor) {
        Vector3d posA = SpellVarUtil.resolvePosition(a, accessor);
        Vector3d posB = SpellVarUtil.resolvePosition(b, accessor);
        if (posA == null || posB == null) return a;
        return new PositionVar(new Vector3d(posA).add(posB), true);
    }

    public static HexVar subtractViaPosition(HexVar a, HexVar b, ComponentAccessor<EntityStore> accessor) {
        Vector3d posA = SpellVarUtil.resolvePosition(a, accessor);
        Vector3d posB = SpellVarUtil.resolvePosition(b, accessor);
        if (posA == null || posB == null) return a;
        return new PositionVar(new Vector3d(posA).subtract(posB), false);
    }

    public static boolean isEqual(HexVar a, HexVar b) {
        if (a == null || b == null) return false;
        return a.equalTo(b);
    }

    public static boolean isGreater(HexVar a, HexVar b) {
        if (a == null || b == null) return false;
        return a.compareTo(b) > 0;
    }

    public static boolean isLess(HexVar a, HexVar b) {
        if (a == null || b == null) return false;
        return a.compareTo(b) < 0;
    }
}
