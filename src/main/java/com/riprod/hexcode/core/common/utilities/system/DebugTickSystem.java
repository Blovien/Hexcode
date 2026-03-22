package com.riprod.hexcode.core.common.utilities.system;

import javax.annotation.Nonnull;

import com.hypixel.hytale.builtin.mounts.MountedComponent;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.matrix.Matrix4d;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.packets.player.DisplayDebug;
import com.hypixel.hytale.server.core.modules.debug.DebugUtils;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.utilities.component.DebugComponent;

public class DebugTickSystem extends EntityTickingSystem<EntityStore> {

    @Override
    public Query<EntityStore> getQuery() {
        return DebugComponent.getComponentType();
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> chunk,
            @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> buffer) {

        DebugComponent debug = chunk.getComponent(index, DebugComponent.getComponentType());
        if (debug == null) {
            return;
        }

        debug.setTimer(debug.getTimer() - dt);
        if (debug.getTimer() > 0) {
            return;
        }

        debug.setTimer(debug.getRespawnInterval());

        TransformComponent transform = chunk.getComponent(index, TransformComponent.getComponentType());
        if (transform == null) {
            return;
        }

        Vector3d pos = transform.getPosition();

        MountedComponent mount = chunk.getComponent(index, MountedComponent.getComponentType());
        if (mount != null) {
            Ref<EntityStore> parentRef = mount.getMountedToEntity();
            if (parentRef != null && parentRef.isValid()) {
                TransformComponent parentTransform = store.getComponent(parentRef,
                        TransformComponent.getComponentType());
                if (parentTransform != null) {
                    Vector3d parentPos = parentTransform.getPosition();
                    Vector3f offset = mount.getAttachmentOffset();
                    pos = new Vector3d(
                            parentPos.x + offset.getX(),
                            parentPos.y + offset.getY(),
                            parentPos.z + offset.getZ());
                }
            }
        }

        Vector3d scale = debug.getScale();
        Matrix4d matrix = new Matrix4d();
        matrix.identity();
        matrix.translate(pos.x, pos.y, pos.z);
        matrix.scale(scale.x, scale.y, scale.z);

        Ref<EntityStore> targetRef = debug.getTargetRef();
        if (targetRef != null && targetRef.isValid()) {
            PlayerRef playerRef = store.getComponent(targetRef, PlayerRef.getComponentType());
            if (playerRef != null) {
                DisplayDebug packet = new DisplayDebug(
                        debug.getShape(), matrix.asFloatData(),
                        new com.hypixel.hytale.protocol.Vector3f(
                                debug.getColor().x, debug.getColor().y, debug.getColor().z),
                        debug.getFadeTime(), (byte) debug.getFlags(), null, debug.getOpacity());
                playerRef.getPacketHandler().write(packet);
            }
        } else {
            World world = buffer.getExternalData().getWorld();
            DebugUtils.add(world, debug.getShape(), matrix, debug.getColor(), debug.getOpacity(),
                    debug.getFadeTime(), debug.getFlags());
        }
    }
}
