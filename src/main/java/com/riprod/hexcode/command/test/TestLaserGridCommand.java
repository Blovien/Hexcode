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

// hypothesis: visually compare multiple lasers at known lengths side-by-side
// spawns 5 horizontal lasers in front of the player at y+0, y+0.5, y+1, y+1.5, y+2
// with lengths 0.1, 0.5, 1.0, 2.0, 5.0 blocks
// also adds a debug sphere at each endpoint for reference
public class TestLaserGridCommand extends AbstractPlayerCommand {
    public TestLaserGridCommand() {
        super("grid", "Grid of lasers at different lengths");
        addAliases("g");
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

        // use only yaw (horizontal), ignore pitch so lasers are level
        float yaw = rot.getYaw();
        double dirX = -Math.sin(yaw);
        double dirZ = -Math.cos(yaw);

        // perpendicular direction for spacing
        double perpX = dirZ;
        double perpZ = -dirX;

        double baseX = pos.x + dirX * 3;
        double baseY = pos.y + eyeHeight;
        double baseZ = pos.z + dirZ * 3;

        float[] lengths = {0.1f, 0.25f, 0.5f, 1.0f, 2.0f};
        int[] colors = {0xFF0000, 0xFF8800, 0xFFFF00, 0x00FF00, 0x0088FF};
        String[] labels = {"0.1", "0.25", "0.5", "1.0", "2.0"};

        for (int i = 0; i < lengths.length; i++) {
            double offsetY = i * 0.5;

            float startX = (float) baseX;
            float startY = (float) (baseY + offsetY);
            float startZ = (float) baseZ;

            float endX = (float) (baseX + dirX * lengths[i]);
            float endY = (float) (baseY + offsetY);
            float endZ = (float) (baseZ + dirZ * lengths[i]);

            BuilderToolLaserPointer packet = new BuilderToolLaserPointer(
                    networkId.getId(),
                    startX, startY, startZ,
                    endX, endY, endZ,
                    colors[i], 10000);
            PlayerUtil.broadcastPacketToPlayers(store, packet);

            // also add a debug sphere at start and end for visual reference
            com.hypixel.hytale.server.core.modules.debug.DebugUtils.addSphere(
                    world, new Vector3d(startX, startY, startZ),
                    new com.hypixel.hytale.math.vector.Vector3f(1f, 1f, 1f), 0.05, 10.0f);
            com.hypixel.hytale.server.core.modules.debug.DebugUtils.addSphere(
                    world, new Vector3d(endX, endY, endZ),
                    new com.hypixel.hytale.math.vector.Vector3f(1f, 1f, 1f), 0.05, 10.0f);
        }

        playerRef.sendMessage(Message.raw("[grid] spawned 5 lasers 3 blocks ahead, stacked vertically:"));
        for (int i = 0; i < lengths.length; i++) {
            playerRef.sendMessage(Message.raw("  y+" + String.format("%.1f", i * 0.5) + ": " + labels[i] + " blocks (color " + Integer.toHexString(colors[i]) + ")"));
        }
        playerRef.sendMessage(Message.raw("  white debug spheres at start/end of each for reference"));
    }
}
