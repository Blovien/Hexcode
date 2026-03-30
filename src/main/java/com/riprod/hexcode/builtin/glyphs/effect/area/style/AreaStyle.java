package com.riprod.hexcode.builtin.glyphs.effect.area.style;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.math.matrix.Matrix4d;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.DebugShape;
import com.hypixel.hytale.server.core.modules.debug.DebugUtils;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.state.execution.component.HexColors;
import com.riprod.hexcode.utils.VfxUtil;

public class AreaStyle {

    private static final Vector3f DEFAULT_COLOR = new Vector3f(0.3f, 0.6f, 0.9f);
    private static final float SPHERE_DURATION = 1.0f;

    private AreaStyle() {
    }

    public static void render(Vector3d center, double radius, HexColors colors,
            ComponentAccessor<EntityStore> accessor) {
        Vector3f color = DEFAULT_COLOR;
        if (colors != null && colors.getPrimaryColor() != null) {
            color = HexColors.toVector3f(colors.getPrimaryColor());
        }

        World world = accessor.getExternalData().getWorld();

        Matrix4d matrix = new Matrix4d();
        matrix.identity();
        matrix.translate(center.x, center.y, center.z);
        matrix.scale(radius * 2.0, radius * 2.0, radius * 2.0);

        int flags = DebugUtils.FLAG_FADE | DebugUtils.FLAG_NO_WIREFRAME;
        DebugUtils.add(world, DebugShape.Sphere, matrix, color, SPHERE_DURATION, flags);

        VfxUtil.effect("Area_Pulse", "SFX_Magic_Area", center, accessor);
    }
}
