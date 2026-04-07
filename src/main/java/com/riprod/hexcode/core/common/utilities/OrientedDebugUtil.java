package com.riprod.hexcode.core.common.utilities;

import com.hypixel.hytale.math.matrix.Matrix4d;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.DebugShape;
import com.hypixel.hytale.server.core.modules.debug.DebugUtils;
import com.hypixel.hytale.server.core.universe.world.World;

public final class OrientedDebugUtil {

    private static final double EPSILON = 0.001;

    private OrientedDebugUtil() {
    }

    public static void addCone(World world, Vector3d from, Vector3d to,
            Vector3f color, double diameter, float time) {
        Vector3d direction = new Vector3d(to.x - from.x, to.y - from.y, to.z - from.z);
        double length = direction.length();
        if (length < EPSILON) return;

        Matrix4d matrix = buildOrientedMatrix(from, direction, length, diameter);
        DebugUtils.add(world, DebugShape.Cone, matrix, color, time, DebugUtils.FLAG_NO_WIREFRAME);
    }

    private static Matrix4d buildOrientedMatrix(Vector3d origin, Vector3d direction,
            double length, double diameter) {
        Matrix4d tmp = new Matrix4d();
        Matrix4d matrix = new Matrix4d();
        matrix.identity();
        matrix.translate(origin);

        double angleY = Math.atan2(direction.z, direction.x);
        matrix.rotateAxis(angleY + (Math.PI / 2), 0.0, 1.0, 0.0, tmp);

        double xzLen = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        double angleX = Math.atan2(xzLen, direction.y);
        matrix.rotateAxis(angleX, 1.0, 0.0, 0.0, tmp);

        // cone is center-anchored; shift base to origin then scale
        matrix.translate(0.0, length / 2.0, 0.0);
        matrix.scale(diameter, length, diameter);
        return matrix;
    }
}
