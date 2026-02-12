package com.riprod.hexcode.command.test;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.packets.buildertools.BuilderToolLaserPointer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
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

// hypothesis: the laser has a minimum render length and ignores actual distance
// or: the distance between start/end controls length but there's a min
// this sends a laser at a configurable distance to find the threshold
public class TestLaserDistanceCommand extends AbstractPlayerCommand {

    private final OptionalArg<Float> distanceArg;

    public TestLaserDistanceCommand() {
        super("distance", "Laser at configurable distance");
        addAliases("d");
        this.distanceArg = this.withOptionalArg("blocks", "Distance in blocks (default 2.0)", ArgTypes.FLOAT);
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

        Float dist = distanceArg.get(ctx);
        if (dist == null) dist = 2.0f;

        Vector3d pos = transform.getPosition();
        float eyeHeight = modelComp.getModel().getEyeHeight(ref, store);
        Vector3f rot = headRot.getRotation();

        double eyeX = pos.x;
        double eyeY = pos.y + eyeHeight;
        double eyeZ = pos.z;

        double dirX = -Math.sin(rot.getYaw()) * Math.cos(rot.getPitch());
        double dirY = Math.sin(rot.getPitch());
        double dirZ = -Math.cos(rot.getYaw()) * Math.cos(rot.getPitch());

        // laser from eye to dist blocks in look direction
        float startX = (float) eyeX;
        float startY = (float) eyeY;
        float startZ = (float) eyeZ;
        float endX = (float) (eyeX + dirX * dist);
        float endY = (float) (eyeY + dirY * dist);
        float endZ = (float) (eyeZ + dirZ * dist);

        BuilderToolLaserPointer packet = new BuilderToolLaserPointer(
                networkId.getId(),
                startX, startY, startZ,
                endX, endY, endZ,
                0xFFFF00, 5000);
        PlayerUtil.broadcastPacketToPlayers(store, packet);

        float actualDist = (float) Math.sqrt(
                Math.pow(endX - startX, 2) + Math.pow(endY - startY, 2) + Math.pow(endZ - startZ, 2));

        playerRef.sendMessage(Message.raw("[distance] laser from eye, " + dist + " blocks forward"));
        playerRef.sendMessage(Message.raw("  start: " + String.format("%.2f, %.2f, %.2f", startX, startY, startZ)));
        playerRef.sendMessage(Message.raw("  end:   " + String.format("%.2f, %.2f, %.2f", endX, endY, endZ)));
        playerRef.sendMessage(Message.raw("  actual distance: " + String.format("%.4f", actualDist)));
    }
}
