package com.riprod.hexcode.utils;

import java.util.List;

import com.hypixel.hytale.math.vector.Vector3d;
import com.riprod.hexcode.core.glyphs.component.GlyphComponent;

public class GlyphMath {

    private GlyphMath() {
    }

    public static Vector3d sphericalToCartesian(Vector3d origin, float yaw, float pitch, double distance) {
        double x = origin.x + distance * (Math.cos(pitch) * -Math.sin(yaw));
        double y = origin.y + distance * Math.sin(pitch);
        double z = origin.z + distance * (Math.cos(pitch) * -Math.cos(yaw));
        return new Vector3d(x, y, z);
    }

    public static Vector3d sphericalToCartesian(SphericalPosition pos) {
        return sphericalToCartesian(new Vector3d(0, 0, 0), pos.yaw, pos.pitch, pos.distance);
    }

    public static Vector3d sphericalToCartesian(Vector3d origin, SphericalPosition pos) {
        return sphericalToCartesian(origin, pos.yaw, pos.pitch, pos.distance);
    }

    public static SphericalPosition cartesianToSpherical(Vector3d origin, Vector3d point) {
        double dx = point.x - origin.x;
        double dy = point.y - origin.y;
        double dz = point.z - origin.z;

        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance < 0.0001) {
            return SphericalPosition.zero();
        }

        float yaw = (float) Math.atan2(dx, dz);
        float pitch = (float) Math.asin(dy / distance);

        return new SphericalPosition(yaw, pitch, distance);
    }

    public static boolean isPointInGlyphArea(SphericalPosition glyphPos, SphericalPosition lookPos, float scale) {
        // todo: check if look direction hits glyph selection area
        // area scales with glyph scale (nested glyphs are smaller)
        float angularDistance = calculateAngularDistance(glyphPos, lookPos);
        float selectionRadius = getSelectionRadius(scale);
        return angularDistance <= selectionRadius;
    }

    public static float calculateAngularDistance(SphericalPosition a, SphericalPosition b) {
        // todo: calculate angular distance between two spherical positions
        // uses spherical law of cosines
        double cosAngle = Math.sin(a.pitch) * Math.sin(b.pitch) +
                Math.cos(a.pitch) * Math.cos(b.pitch) * Math.cos(a.yaw - b.yaw);
        cosAngle = Math.max(-1.0, Math.min(1.0, cosAngle));
        return (float) Math.acos(cosAngle);
    }

    public static float getSelectionRadius(float scale) {
        // todo: return selection radius in radians based on glyph scale
        // base radius ~15 degrees = ~0.26 radians, scaled by visual size
        float baseRadius = 0.0624f;
        return baseRadius * scale;
    }

    public static void distributeChildAngles(List<GlyphComponent> children, float parentScale) {
        if (children == null || children.isEmpty()) {
            return;
        }
        
        if (children.size() == 1) {
            children.get(0).setYaw(0f);
            children.get(0).setPitch(0f);
            return;
        }

        float angleIncrement = (float) (2 * Math.PI / children.size());
        float angularRadius = getSelectionRadius(parentScale) * parentScale * 3; // scales with the parent scale

        for (int i = 0; i < children.size(); i++) {
            float theta = i * angleIncrement;
            children.get(i).setYaw(angularRadius * (float) Math.cos(theta));
            children.get(i).setPitch(angularRadius * (float) Math.sin(theta));
        }
    }
}
