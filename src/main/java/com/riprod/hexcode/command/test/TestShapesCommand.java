package com.riprod.hexcode.command.test;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.matrix.Matrix4d;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.DebugShape;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.modules.debug.DebugUtils;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class TestShapesCommand extends AbstractPlayerCommand {

    private static final float DURATION = 30.0f;
    private static final double THICKNESS = 0.5;
    private static final double LENGTH = 8.0;
    private static final Vector3f COLOR = new Vector3f(0.6f, 0.2f, 1.0f);

    public TestShapesCommand() {
        super("shapes", "Spawn different debug shape lines for visual comparison");
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> playerEntityRef, @Nonnull PlayerRef playerRef, @Nonnull World world) {

        TransformComponent transform = store.getComponent(playerEntityRef, TransformComponent.getComponentType());
        ModelComponent playerModel = store.getComponent(playerEntityRef, ModelComponent.getComponentType());
        if (transform == null || playerModel == null) return;

        float eyeHeight = playerModel.getModel().getEyeHeight();
        Vector3d pos = new Vector3d(transform.getPosition()).add(0, eyeHeight, 0);

        Vector3d start = new Vector3d(pos).add(2, 0, 0);
        double spacing = 2.0;

        // 1: cylinder (default addLine)
        Vector3d s1 = new Vector3d(start);
        Vector3d e1 = new Vector3d(s1).add(0, 0, LENGTH);
        DebugUtils.addLine(world, s1, e1, COLOR, THICKNESS, DURATION, false);
        playerRef.sendMessage(Message.raw("1: Cylinder (addLine) - no fade"));

        // 2: cylinder with fade
        Vector3d s2 = new Vector3d(start).add(spacing, 0, 0);
        Vector3d e2 = new Vector3d(s2).add(0, 0, LENGTH);
        DebugUtils.addLine(world, s2, e2, COLOR, THICKNESS, DURATION, true);
        playerRef.sendMessage(Message.raw("2: Cylinder (addLine) - fade"));

        // 3: cube line
        Vector3d s3 = new Vector3d(start).add(spacing * 2, 0, 0);
        Vector3d e3 = new Vector3d(s3).add(0, 0, LENGTH);
        spawnLineShape(world, s3, e3, COLOR, THICKNESS, DURATION, false, DebugShape.Cube);
        playerRef.sendMessage(Message.raw("3: Cube - no fade"));

        // 4: cube line with fade
        Vector3d s4 = new Vector3d(start).add(spacing * 3, 0, 0);
        Vector3d e4 = new Vector3d(s4).add(0, 0, LENGTH);
        spawnLineShape(world, s4, e4, COLOR, THICKNESS, DURATION, true, DebugShape.Cube);
        playerRef.sendMessage(Message.raw("4: Cube - fade"));

        // 5: cone line
        Vector3d s5 = new Vector3d(start).add(spacing * 4, 0, 0);
        Vector3d e5 = new Vector3d(s5).add(0, 0, LENGTH);
        spawnLineShape(world, s5, e5, COLOR, THICKNESS, DURATION, false, DebugShape.Cone);
        playerRef.sendMessage(Message.raw("5: Cone - no fade"));

        // 6: cylinder thinner
        Vector3d s6 = new Vector3d(start).add(spacing * 5, 0, 0);
        Vector3d e6 = new Vector3d(s6).add(0, 0, LENGTH);
        DebugUtils.addLine(world, s6, e6, COLOR, 0.01, DURATION, false);
        playerRef.sendMessage(Message.raw("6: Cylinder thin (0.01)"));

        // 7: cylinder thicker
        Vector3d s7 = new Vector3d(start).add(spacing * 6, 0, 0);
        Vector3d e7 = new Vector3d(s7).add(0, 0, LENGTH);
        DebugUtils.addLine(world, s7, e7, COLOR, 0.06, DURATION, false);
        playerRef.sendMessage(Message.raw("7: Cylinder thick (0.06)"));

        // 8: cylinder low opacity
        Vector3d s8 = new Vector3d(start).add(spacing * 7, 0, 0);
        Vector3d e8 = new Vector3d(s8).add(0, 0, LENGTH);
        spawnLineOpacity(world, s8, e8, COLOR, THICKNESS, 0.3f, DURATION, false, DebugShape.Cylinder);
        playerRef.sendMessage(Message.raw("8: Cylinder opacity 0.3"));

        // 9: cylinder high opacity
        Vector3d s9 = new Vector3d(start).add(spacing * 8, 0, 0);
        Vector3d e9 = new Vector3d(s9).add(0, 0, LENGTH);
        spawnLineOpacity(world, s9, e9, COLOR, THICKNESS, 1.0f, DURATION, false, DebugShape.Cylinder);
        playerRef.sendMessage(Message.raw("9: Cylinder opacity 1.0"));

        // 10: cube low opacity
        Vector3d s10 = new Vector3d(start).add(spacing * 9, 0, 0);
        Vector3d e10 = new Vector3d(s10).add(0, 0, LENGTH);
        spawnLineOpacity(world, s10, e10, COLOR, THICKNESS, 0.3f, DURATION, false, DebugShape.Cube);
        playerRef.sendMessage(Message.raw("10: Cube opacity 0.3"));

        // 11: cube high opacity
        Vector3d s11 = new Vector3d(start).add(spacing * 10, 0, 0);
        Vector3d e11 = new Vector3d(s11).add(0, 0, LENGTH);
        spawnLineOpacity(world, s11, e11, COLOR, THICKNESS, 1.0f, DURATION, false, DebugShape.Cube);
        playerRef.sendMessage(Message.raw("11: Cube opacity 1.0"));

        // 12: sphere chain (small spheres along the line)
        Vector3d s12 = new Vector3d(start).add(spacing * 11, 0, 0);
        int sphereCount = 20;
        for (int i = 0; i <= sphereCount; i++) {
            double t = (double) i / sphereCount;
            Vector3d p = new Vector3d(s12).add(0, 0, LENGTH * t);
            DebugUtils.addSphere(world, p, COLOR, 0.04, DURATION);
        }
        playerRef.sendMessage(Message.raw("12: Sphere chain (20 spheres)"));

        playerRef.sendMessage(Message.raw("Spawned 12 line styles at your position. Walk forward to compare."));
    }

    private static void spawnLineShape(World world, Vector3d start, Vector3d end,
            Vector3f color, double thickness, float time, boolean fade, DebugShape shape) {
        double dirX = end.x - start.x;
        double dirY = end.y - start.y;
        double dirZ = end.z - start.z;
        double length = Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
        if (length < 0.001) return;

        Matrix4d tmp = new Matrix4d();
        Matrix4d matrix = new Matrix4d();
        matrix.identity();
        matrix.translate(start.x, start.y, start.z);
        double angleY = Math.atan2(dirZ, dirX);
        matrix.rotateAxis(angleY + (Math.PI / 2), 0.0, 1.0, 0.0, tmp);
        double angleX = Math.atan2(Math.sqrt(dirX * dirX + dirZ * dirZ), dirY);
        matrix.rotateAxis(angleX, 1.0, 0.0, 0.0, tmp);
        matrix.translate(0.0, length / 2.0, 0.0);
        matrix.scale(thickness, length, thickness);
        DebugUtils.add(world, shape, matrix, color, time, fade);
    }

    private static void spawnLineOpacity(World world, Vector3d start, Vector3d end,
            Vector3f color, double thickness, float opacity, float time, boolean fade, DebugShape shape) {
        double dirX = end.x - start.x;
        double dirY = end.y - start.y;
        double dirZ = end.z - start.z;
        double length = Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
        if (length < 0.001) return;

        Matrix4d tmp = new Matrix4d();
        Matrix4d matrix = new Matrix4d();
        matrix.identity();
        matrix.translate(start.x, start.y, start.z);
        double angleY = Math.atan2(dirZ, dirX);
        matrix.rotateAxis(angleY + (Math.PI / 2), 0.0, 1.0, 0.0, tmp);
        double angleX = Math.atan2(Math.sqrt(dirX * dirX + dirZ * dirZ), dirY);
        matrix.rotateAxis(angleX, 1.0, 0.0, 0.0, tmp);
        matrix.translate(0.0, length / 2.0, 0.0);
        matrix.scale(thickness, length, thickness);
        DebugUtils.add(world, shape, matrix, color, opacity, time, fade);
    }
}
