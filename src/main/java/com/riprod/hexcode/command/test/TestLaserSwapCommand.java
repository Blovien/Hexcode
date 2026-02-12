package com.riprod.hexcode.command.test;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.packets.buildertools.BuilderToolLaserPointer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.modules.debug.DebugUtils;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.PlayerUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

// tests three hypotheses about start/end behavior:
// 1. normal: start=pointA, end=pointB (what we had)
// 2. swapped: start=pointB, end=pointA
// 3. no player: playerNetworkId=0 with start=pointA, end=pointB
//
// uses two fixed world-space points 3 blocks ahead of player,
// 0.5 blocks apart horizontally. debug spheres mark A (red) and B (green).
public class TestLaserSwapCommand extends AbstractPlayerCommand {
    public TestLaserSwapCommand() {
        super("swap", "Test start/end swap and no-player modes");
        addAliases("s");
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx, @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {

        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        ModelComponent modelComp = store.getComponent(ref, ModelComponent.getComponentType());
        HeadRotation headRot = store.getComponent(ref, HeadRotation.getComponentType());
        NetworkId networkId = store.getComponent(ref, NetworkId.getComponentType());

        if (transform == null || modelComp == null || headRot == null || networkId == null) {
            playerRef.sendMessage(Message.raw("missing components"));
            return;
        }

        Vector3d pos = transform.getPosition();
        float eyeHeight = modelComp.getModel().getEyeHeight(ref, store);
        Vector3f rot = headRot.getRotation();

        float yaw = rot.getYaw();
        double dirX = -Math.sin(yaw);
        double dirZ = -Math.cos(yaw);

        // perpendicular for side offset
        double perpX = dirZ;
        double perpZ = -dirX;

        double eyeY = pos.y + eyeHeight;

        // point A: 3 blocks ahead
        double ax = pos.x + dirX * 3.0;
        double ay = eyeY;
        double az = pos.z + dirZ * 3.0;

        // point B: 3 blocks ahead + 0.5 blocks further
        double bx = ax + dirX * 0.5;
        double by = eyeY;
        double bz = az + dirZ * 0.5;

        // mark A and B with debug spheres
        DebugUtils.addSphere(world, new Vector3d(ax, ay, az),
                new com.hypixel.hytale.math.vector.Vector3f(1f, 0f, 0f), 0.1, 10.0f);
        DebugUtils.addSphere(world, new Vector3d(bx, by, bz),
                new com.hypixel.hytale.math.vector.Vector3f(0f, 1f, 0f), 0.1, 10.0f);

        // test 1: normal order (start=A, end=B) - offset left
        double off1X = perpX * -1.0;
        double off1Z = perpZ * -1.0;
        BuilderToolLaserPointer normal = new BuilderToolLaserPointer(
                networkId.getId(),
                (float)(ax + off1X), (float) ay, (float)(az + off1Z),
                (float)(bx + off1X), (float) by, (float)(bz + off1Z),
                0xFF0000, 10000);
        PlayerUtil.broadcastPacketToPlayers(store, normal);
        DebugUtils.addSphere(world, new Vector3d(ax + off1X, ay, az + off1Z),
                new com.hypixel.hytale.math.vector.Vector3f(1f, 0.5f, 0.5f), 0.05, 10.0f);
        DebugUtils.addSphere(world, new Vector3d(bx + off1X, by, bz + off1Z),
                new com.hypixel.hytale.math.vector.Vector3f(1f, 0.5f, 0.5f), 0.05, 10.0f);

        // test 2: swapped (start=B, end=A) - center
        BuilderToolLaserPointer swapped = new BuilderToolLaserPointer(
                networkId.getId(),
                (float) bx, (float) by, (float) bz,
                (float) ax, (float) ay, (float) az,
                0x00FF00, 10000);
        PlayerUtil.broadcastPacketToPlayers(store, swapped);

        // test 3: no player id (start=A, end=B) - offset right
        double off3X = perpX * 1.0;
        double off3Z = perpZ * 1.0;
        BuilderToolLaserPointer noPlayer = new BuilderToolLaserPointer(
                0,
                (float)(ax + off3X), (float) ay, (float)(az + off3Z),
                (float)(bx + off3X), (float) by, (float)(bz + off3Z),
                0x0088FF, 10000);
        PlayerUtil.broadcastPacketToPlayers(store, noPlayer);
        DebugUtils.addSphere(world, new Vector3d(ax + off3X, ay, az + off3Z),
                new com.hypixel.hytale.math.vector.Vector3f(0.5f, 0.5f, 1f), 0.05, 10.0f);
        DebugUtils.addSphere(world, new Vector3d(bx + off3X, by, bz + off3Z),
                new com.hypixel.hytale.math.vector.Vector3f(0.5f, 0.5f, 1f), 0.05, 10.0f);

        // test 4: no player id, swapped (start=B, end=A) - offset far right
        double off4X = perpX * 2.0;
        double off4Z = perpZ * 2.0;
        BuilderToolLaserPointer noPlayerSwapped = new BuilderToolLaserPointer(
                0,
                (float)(bx + off4X), (float) by, (float)(bz + off4Z),
                (float)(ax + off4X), (float) ay, (float)(az + off4Z),
                0xFFFF00, 10000);
        PlayerUtil.broadcastPacketToPlayers(store, noPlayerSwapped);
        DebugUtils.addSphere(world, new Vector3d(ax + off4X, ay, az + off4Z),
                new com.hypixel.hytale.math.vector.Vector3f(1f, 1f, 0.5f), 0.05, 10.0f);
        DebugUtils.addSphere(world, new Vector3d(bx + off4X, by, bz + off4Z),
                new com.hypixel.hytale.math.vector.Vector3f(1f, 1f, 0.5f), 0.05, 10.0f);

        playerRef.sendMessage(Message.raw("[swap] 4 lasers between two points 0.5 blocks apart, 3 blocks ahead:"));
        playerRef.sendMessage(Message.raw("  red sphere = point A, green sphere = point B (center)"));
        playerRef.sendMessage(Message.raw("  LEFT  - red laser:    normal (start=A, end=B, with player id)"));
        playerRef.sendMessage(Message.raw("  CENTER- green laser:  swapped (start=B, end=A, with player id)"));
        playerRef.sendMessage(Message.raw("  RIGHT - blue laser:   normal (start=A, end=B, NO player id)"));
        playerRef.sendMessage(Message.raw("  FAR R - yellow laser: swapped (start=B, end=A, NO player id)"));
        playerRef.sendMessage(Message.raw("  small debug spheres mark start/end of each laser"));
    }
}
