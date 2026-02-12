package com.riprod.hexcode.command.test;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.packets.buildertools.BuilderToolLaserPointer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.PlayerUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

// hypothesis: start==end or very short distance might still render a fixed-length beam
// this tests a near-zero length laser (0.1 blocks forward from eye)
public class TestLaserPointCommand extends AbstractPlayerCommand {
    public TestLaserPointCommand() {
        super("point", "Laser with near-zero length");
        addAliases("p");
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

        double eyeX = pos.x;
        double eyeY = pos.y + eyeHeight;
        double eyeZ = pos.z;

        // direction from head rotation (pitch/yaw in radians)
        double dirX = -Math.sin(rot.getYaw()) * Math.cos(rot.getPitch());
        double dirY = Math.sin(rot.getPitch());
        double dirZ = -Math.cos(rot.getYaw()) * Math.cos(rot.getPitch());

        // test 1: start and end at same point
        BuilderToolLaserPointer samePoint = new BuilderToolLaserPointer(
                networkId.getId(),
                (float) eyeX, (float) eyeY, (float) eyeZ,
                (float) eyeX, (float) eyeY, (float) eyeZ,
                0xFF0000, 5000);
        PlayerUtil.broadcastPacketToPlayers(store, samePoint);

        // test 2: 0.1 blocks forward
        BuilderToolLaserPointer tiny = new BuilderToolLaserPointer(
                networkId.getId(),
                (float) eyeX, (float) eyeY, (float) eyeZ,
                (float) (eyeX + dirX * 0.1), (float) (eyeY + dirY * 0.1), (float) (eyeZ + dirZ * 0.1),
                0x00FF00, 5000);
        PlayerUtil.broadcastPacketToPlayers(store, tiny);

        // test 3: 0.5 blocks forward
        BuilderToolLaserPointer half = new BuilderToolLaserPointer(
                networkId.getId(),
                (float) eyeX, (float) eyeY, (float) eyeZ,
                (float) (eyeX + dirX * 0.5), (float) (eyeY + dirY * 0.5), (float) (eyeZ + dirZ * 0.5),
                0x0000FF, 5000);
        PlayerUtil.broadcastPacketToPlayers(store, half);

        playerRef.sendMessage(Message.raw("[point] sent 3 lasers from eye pos:"));
        playerRef.sendMessage(Message.raw("  red = start==end (zero length)"));
        playerRef.sendMessage(Message.raw("  green = 0.1 blocks"));
        playerRef.sendMessage(Message.raw("  blue = 0.5 blocks"));
        playerRef.sendMessage(Message.raw("  eye pos: " + String.format("%.2f, %.2f, %.2f", eyeX, eyeY, eyeZ)));
    }
}
