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

// hypothesis: durationMs might affect visual length, not just time
// or: very low durations might not render at all
// test with 0, 1, 50, 300, 5000ms
public class TestLaserDurationCommand extends AbstractPlayerCommand {

    private final OptionalArg<Integer> durationArg;

    public TestLaserDurationCommand() {
        super("duration", "Laser with configurable duration");
        addAliases("dur");
        this.durationArg = this.withOptionalArg("ms", "Duration in ms (default 300)", ArgTypes.INTEGER);
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

        Integer durationMs = durationArg.get(ctx);
        if (durationMs == null) durationMs = 300;

        Vector3d pos = transform.getPosition();
        float eyeHeight = modelComp.getModel().getEyeHeight(ref, store);
        Vector3f rot = headRot.getRotation();

        double eyeX = pos.x;
        double eyeY = pos.y + eyeHeight;
        double eyeZ = pos.z;

        double dirX = -Math.sin(rot.getYaw()) * Math.cos(rot.getPitch());
        double dirY = Math.sin(rot.getPitch());
        double dirZ = -Math.cos(rot.getYaw()) * Math.cos(rot.getPitch());

        // fixed 2-block distance, variable duration
        float dist = 2.0f;

        BuilderToolLaserPointer packet = new BuilderToolLaserPointer(
                networkId.getId(),
                (float) eyeX, (float) eyeY, (float) eyeZ,
                (float) (eyeX + dirX * dist), (float) (eyeY + dirY * dist), (float) (eyeZ + dirZ * dist),
                0xFF00FF, durationMs);
        PlayerUtil.broadcastPacketToPlayers(store, packet);

        playerRef.sendMessage(Message.raw("[duration] laser 2 blocks forward, duration=" + durationMs + "ms"));
    }
}
