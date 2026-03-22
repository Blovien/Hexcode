package com.riprod.hexcode.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.entity.reference.PersistentRef;
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

    // --- add ---

    public static HexVar add(HexVar a, HexVar b) {
        if (a instanceof NumberVar na && b instanceof NumberVar nb) return addNumbers(na, nb);
        if (a instanceof PositionVar pa && b instanceof PositionVar pb) return addPositions(pa, pb);
        if (a instanceof RotationVar ra && b instanceof RotationVar rb) return addRotations(ra, rb);
        if (a instanceof PositionVar pa && b instanceof NumberVar nb) return addPositionNumber(pa, nb);
        if (a instanceof RotationVar ra && b instanceof NumberVar nb) return addRotationNumber(ra, nb);
        if (a instanceof NumberVar na && b instanceof RotationVar rb) return addRotationNumber(rb, na);
        if (a instanceof NumberVar na && b instanceof PositionVar pb) return addPositionNumber(pb, na);
        if (a instanceof EntityVar ea && b instanceof EntityVar eb) return concatEntities(ea, eb);
        if (a instanceof BlockVar ba && b instanceof BlockVar bb) return concatBlocks(ba, bb);
        LOGGER.atWarning().log("add: unsupported types " + a.getClass().getSimpleName() + " + " + b.getClass().getSimpleName());
        return a;
    }

    // --- negate ---

    public static HexVar negate(HexVar a) {
        if (a instanceof NumberVar na) return negateNumber(na);
        if (a instanceof PositionVar pa) return negatePosition(pa);
        if (a instanceof RotationVar ra) return negateRotation(ra);
        LOGGER.atWarning().log("negate: unsupported type " + a.getClass().getSimpleName());
        return a;
    }

    private static NumberVar negateNumber(NumberVar a) {
        List<Double> result = new ArrayList<>(a.size());
        for (int i = 0; i < a.size(); i++) {
            result.add(-a.getAt(i));
        }
        return new NumberVar(result);
    }

    private static PositionVar negatePosition(PositionVar a) {
        List<Vector3d> result = new ArrayList<>(a.size());
        for (int i = 0; i < a.size(); i++) {
            Vector3d v = a.getAt(i);
            result.add(new Vector3d(-v.x, -v.y, -v.z));
        }
        return new PositionVar(result);
    }

    private static RotationVar negateRotation(RotationVar a) {
        List<Vector3f> result = new ArrayList<>(a.size());
        for (int i = 0; i < a.size(); i++) {
            Vector3f v = a.getAt(i);
            result.add(new Vector3f(-v.x, -v.y, -v.z));
        }
        return new RotationVar(result);
    }

    // --- subtract ---

    public static HexVar subtract(HexVar a, HexVar b) {
        if (a == null || b == null) return a;
        if (a instanceof NumberVar na && b instanceof NumberVar nb) return subtractNumbers(na, nb);
        if (a instanceof PositionVar pa && b instanceof PositionVar pb) return subtractPositions(pa, pb);
        if (a instanceof RotationVar ra && b instanceof RotationVar rb) return subtractRotations(ra, rb);
        if (a instanceof PositionVar pa && b instanceof NumberVar nb) return subtractPositionNumber(pa, nb);
        if (a instanceof RotationVar ra && b instanceof NumberVar nb) return subtractRotationNumber(ra, nb);
        if (a instanceof EntityVar ea && b instanceof EntityVar eb) return removeEntities(ea, eb);
        if (a instanceof BlockVar ba && b instanceof BlockVar bb) return removeBlocks(ba, bb);
        LOGGER.atWarning().log("subtract: unsupported types " + a.getClass().getSimpleName() + " - " + b.getClass().getSimpleName());
        return a;
    }

    // --- multiply ---

    public static HexVar multiply(HexVar a, HexVar b) {
        if (a instanceof NumberVar na && b instanceof NumberVar nb) return multiplyNumbers(na, nb);
        if (a instanceof PositionVar pa && b instanceof PositionVar pb) return multiplyPositions(pa, pb);
        if (a instanceof PositionVar pa && b instanceof NumberVar nb) return multiplyPositionNumber(pa, nb);
        if (a instanceof RotationVar ra && b instanceof NumberVar nb) return multiplyRotationNumber(ra, nb);
        if (a instanceof NumberVar na && b instanceof PositionVar pb) return multiplyPositionNumber(pb, na);
        if (a instanceof NumberVar na && b instanceof RotationVar rb) return multiplyRotationNumber(rb, na);
        if (a instanceof RotationVar ra && b instanceof RotationVar rb) return multiplyRotations(ra, rb);
        LOGGER.atWarning().log("multiply: unsupported types " + a.getClass().getSimpleName() + " * " + b.getClass().getSimpleName());
        return a;
    }

    // --- divide ---

    public static HexVar divide(HexVar a, HexVar b) {
        if (a instanceof NumberVar na && b instanceof NumberVar nb) return divideNumbers(na, nb);
        if (a instanceof PositionVar pa && b instanceof NumberVar nb) return dividePositionNumber(pa, nb);
        if (a instanceof PositionVar pa && b instanceof PositionVar pb) return dividePositions(pa, pb);
        if (a instanceof RotationVar ra && b instanceof NumberVar nb) return divideRotationNumber(ra, nb);
        if (a instanceof RotationVar ra && b instanceof RotationVar rb) return divideRotations(ra, rb);
        LOGGER.atWarning().log("divide: unsupported types " + a.getClass().getSimpleName() + " / " + b.getClass().getSimpleName());
        return a;
    }

    // --- number operations ---

    private static NumberVar addNumbers(NumberVar a, NumberVar b) {
        return zipNumbers(a, b, (x, y) -> x + y);
    }

    private static NumberVar subtractNumbers(NumberVar a, NumberVar b) {
        return zipNumbers(a, b, (x, y) -> x - y);
    }

    private static NumberVar multiplyNumbers(NumberVar a, NumberVar b) {
        return zipNumbers(a, b, (x, y) -> x * y);
    }

    private static NumberVar divideNumbers(NumberVar a, NumberVar b) {
        return zipNumbers(a, b, (x, y) -> y != 0 ? x / y : x);
    }

    private static NumberVar zipNumbers(NumberVar a, NumberVar b, DoubleBinaryOp op) {
        boolean broadcastA = a.size() == 1 && b.size() > 1;
        boolean broadcastB = b.size() == 1 && a.size() > 1;

        int maxSize = Math.max(a.size(), b.size());
        List<Double> result = new ArrayList<>(maxSize);

        for (int i = 0; i < maxSize; i++) {
            double va = a.getAt(broadcastA ? 0 : Math.min(i, a.size() - 1));
            double vb = b.getAt(broadcastB ? 0 : Math.min(i, b.size() - 1));

            if (i < a.size() || broadcastA) {
                if (i < b.size() || broadcastB) {
                    result.add(op.apply(va, vb));
                } else {
                    result.add(va);
                }
            }
        }

        return new NumberVar(result);
    }

    // --- position operations ---

    private static PositionVar addPositions(PositionVar a, PositionVar b) {
        return zipPositions(a, b, (va, vb) -> new Vector3d(va).add(vb));
    }

    private static PositionVar subtractPositions(PositionVar a, PositionVar b) {
        return zipPositions(a, b, (va, vb) -> new Vector3d(va).subtract(vb));
    }

    private static PositionVar multiplyPositions(PositionVar a, PositionVar b) {
        return zipPositions(a, b, (va, vb) -> new Vector3d(va).scale(vb));
    }

    private static PositionVar dividePositions(PositionVar a, PositionVar b) {
        return zipPositions(a, b, (va, vb) -> new Vector3d(
                vb.x != 0 ? va.x / vb.x : va.x,
                vb.y != 0 ? va.y / vb.y : va.y,
                vb.z != 0 ? va.z / vb.z : va.z));
    }

    private static PositionVar addPositionNumber(PositionVar a, NumberVar b) {
        return mapPositionScalar(a, b, (pos, s) -> new Vector3d(pos).add(s));
    }

    private static PositionVar subtractPositionNumber(PositionVar a, NumberVar b) {
        return mapPositionScalar(a, b, (pos, s) -> new Vector3d(pos).subtract(s));
    }

    private static PositionVar multiplyPositionNumber(PositionVar a, NumberVar b) {
        return mapPositionScalar(a, b, (pos, s) -> new Vector3d(pos).scale(s));
    }

    private static PositionVar dividePositionNumber(PositionVar a, NumberVar b) {
        return mapPositionScalar(a, b, (pos, s) -> s != 0 ? new Vector3d(pos).scale(1.0 / s) : new Vector3d(pos));
    }

    private static PositionVar zipPositions(PositionVar a, PositionVar b, Vec3dBinaryOp op) {
        boolean broadcastA = a.size() == 1 && b.size() > 1;
        boolean broadcastB = b.size() == 1 && a.size() > 1;

        int maxSize = Math.max(a.size(), b.size());
        List<Vector3d> result = new ArrayList<>(maxSize);

        for (int i = 0; i < maxSize; i++) {
            if (i < a.size() || broadcastA) {
                Vector3d va = a.getAt(broadcastA ? 0 : i);
                if (i < b.size() || broadcastB) {
                    Vector3d vb = b.getAt(broadcastB ? 0 : i);
                    result.add(op.apply(va, vb));
                } else {
                    result.add(new Vector3d(va));
                }
            }
        }

        return new PositionVar(result);
    }

    private static PositionVar mapPositionScalar(PositionVar a, NumberVar b, Vec3dScalarOp op) {
        boolean broadcastB = b.size() == 1;
        int maxSize = a.size();
        List<Vector3d> result = new ArrayList<>(maxSize);

        for (int i = 0; i < maxSize; i++) {
            double scalar = b.getAt(broadcastB ? 0 : Math.min(i, b.size() - 1));
            if (i < b.size() || broadcastB) {
                result.add(op.apply(a.getAt(i), scalar));
            } else {
                result.add(new Vector3d(a.getAt(i)));
            }
        }

        return new PositionVar(result);
    }

    // --- rotation operations ---

    private static RotationVar addRotations(RotationVar a, RotationVar b) {
        return zipRotations(a, b, (va, vb) -> new Vector3f(va).add(vb));
    }

    private static RotationVar subtractRotations(RotationVar a, RotationVar b) {
        return zipRotations(a, b, (va, vb) -> new Vector3f(va).subtract(vb));
    }

    private static RotationVar multiplyRotations(RotationVar a, RotationVar b) {
        return zipRotations(a, b, (va, vb) -> new Vector3f(va).scale(vb));
    }

    private static RotationVar divideRotations(RotationVar a, RotationVar b) {
        return zipRotations(a, b, (va, vb) -> new Vector3f(
                vb.x != 0 ? va.x / vb.x : va.x,
                vb.y != 0 ? va.y / vb.y : va.y,
                vb.z != 0 ? va.z / vb.z : va.z));
    }

    private static RotationVar addRotationNumber(RotationVar a, NumberVar b) {
        return mapRotationScalar(a, b, (rot, s) -> new Vector3f(rot).add((float)s, (float)s, (float)s));
    }

    private static RotationVar subtractRotationNumber(RotationVar a, NumberVar b) {
        return mapRotationScalar(a, b, (rot, s) -> new Vector3f(rot).subtract((float)s, (float)s, (float)s));
    }

    private static RotationVar multiplyRotationNumber(RotationVar a, NumberVar b) {
        return mapRotationScalar(a, b, (rot, s) -> new Vector3f(rot).scale((float) s));
    }

    private static RotationVar divideRotationNumber(RotationVar a, NumberVar b) {
        return mapRotationScalar(a, b, (rot, s) -> s != 0 ? new Vector3f(rot).scale((float) (1.0 / s)) : new Vector3f(rot));
    }

    private static RotationVar zipRotations(RotationVar a, RotationVar b, Vec3fBinaryOp op) {
        boolean broadcastA = a.size() == 1 && b.size() > 1;
        boolean broadcastB = b.size() == 1 && a.size() > 1;

        int maxSize = Math.max(a.size(), b.size());
        List<Vector3f> result = new ArrayList<>(maxSize);

        for (int i = 0; i < maxSize; i++) {
            if (i < a.size() || broadcastA) {
                Vector3f va = a.getAt(broadcastA ? 0 : i);
                if (i < b.size() || broadcastB) {
                    Vector3f vb = b.getAt(broadcastB ? 0 : i);
                    result.add(op.apply(va, vb));
                } else {
                    result.add(new Vector3f(va));
                }
            }
        }

        return new RotationVar(result);
    }

    private static RotationVar mapRotationScalar(RotationVar a, NumberVar b, Vec3fScalarOp op) {
        boolean broadcastB = b.size() == 1;
        int maxSize = a.size();
        List<Vector3f> result = new ArrayList<>(maxSize);

        for (int i = 0; i < maxSize; i++) {
            double scalar = b.getAt(broadcastB ? 0 : Math.min(i, b.size() - 1));
            if (i < b.size() || broadcastB) {
                result.add(op.apply(a.getAt(i), scalar));
            } else {
                result.add(new Vector3f(a.getAt(i)));
            }
        }

        return new RotationVar(result);
    }

    // --- entity/block list operations ---

    private static EntityVar concatEntities(EntityVar a, EntityVar b) {
        List<PersistentRef> result = new ArrayList<>(a.getValues());
        result.addAll(b.getValues());
        return new EntityVar(result);
    }

    private static EntityVar removeEntities(EntityVar a, EntityVar b) {
        Set<UUID> toRemove = new HashSet<>();
        for (PersistentRef ref : b.getValues()) {
            if (ref.getUuid() != null) toRemove.add(ref.getUuid());
        }
        List<PersistentRef> result = new ArrayList<>();
        for (PersistentRef ref : a.getValues()) {
            if (ref.getUuid() == null || !toRemove.contains(ref.getUuid())) {
                result.add(ref);
            }
        }
        return new EntityVar(result);
    }

    private static BlockVar concatBlocks(BlockVar a, BlockVar b) {
        List<Vector3i> result = new ArrayList<>(a.getValues());
        result.addAll(b.getValues());
        return new BlockVar(result);
    }

    private static BlockVar removeBlocks(BlockVar a, BlockVar b) {
        Set<Vector3i> toRemove = new HashSet<>(b.getValues());
        List<Vector3i> result = new ArrayList<>();
        for (Vector3i pos : a.getValues()) {
            if (!toRemove.contains(pos)) result.add(pos);
        }
        return new BlockVar(result);
    }

    // --- comparisons ---

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

    // --- functional interfaces ---

    @FunctionalInterface
    private interface DoubleBinaryOp {
        double apply(double a, double b);
    }

    @FunctionalInterface
    private interface Vec3dBinaryOp {
        Vector3d apply(Vector3d a, Vector3d b);
    }

    @FunctionalInterface
    private interface Vec3dScalarOp {
        Vector3d apply(Vector3d a, double b);
    }

    @FunctionalInterface
    private interface Vec3fBinaryOp {
        Vector3f apply(Vector3f a, Vector3f b);
    }

    @FunctionalInterface
    private interface Vec3fScalarOp {
        Vector3f apply(Vector3f a, double b);
    }
}
