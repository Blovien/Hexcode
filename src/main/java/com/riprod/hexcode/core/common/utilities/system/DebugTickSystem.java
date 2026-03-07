package com.riprod.hexcode.core.common.utilities.system;

import javax.annotation.Nonnull;

import com.hypixel.hytale.builtin.mounts.MountedComponent;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.modules.debug.DebugUtils;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
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
                TransformComponent parentTransform = store.getComponent(parentRef, TransformComponent.getComponentType());
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

        World world = buffer.getExternalData().getWorld();

        switch (debug.getShape()) {
            case Cube:
                DebugUtils.addCube(world, pos, debug.getColor(), debug.getScale(), debug.getFadeTime());
                break;
            case Sphere:
                DebugUtils.addSphere(world, pos, debug.getColor(), debug.getScale(), debug.getFadeTime());
                break;
            case Cylinder:
                DebugUtils.addCylinder(world, pos, debug.getColor(), debug.getScale(), debug.getFadeTime());
                break;
            case Cone:
                DebugUtils.addCone(world, pos, debug.getColor(), debug.getScale(), debug.getFadeTime());
                break;
            default:
                break;
        }
    }
}
