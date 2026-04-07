package com.riprod.hexcode.utils;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.ChangeVelocityType;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.modules.projectile.config.StandardPhysicsProvider;
import com.hypixel.hytale.server.core.modules.splitvelocity.VelocityConfig;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class VelocityUtil {

    public static boolean isProjectile(Ref<EntityStore> ref, CommandBuffer<EntityStore> buffer) {
        return buffer.getComponent(ref, StandardPhysicsProvider.getComponentType()) != null;
    }

    public static void applyVelocity(Ref<EntityStore> ref, Vector3d velocity,
            ChangeVelocityType type, VelocityConfig config,
            CommandBuffer<EntityStore> buffer) {
        if (type == ChangeVelocityType.Set && isProjectile(ref, buffer)) {
            StandardPhysicsProvider physics = buffer.getComponent(ref,
                    StandardPhysicsProvider.getComponentType());
            physics.getForceProviderStandardState().nextTickVelocity.assign(velocity);
            if (physics.getState() != StandardPhysicsProvider.STATE.ACTIVE) {
                physics.setState(StandardPhysicsProvider.STATE.ACTIVE);
            }
            return;
        }

        Velocity vel = buffer.getComponent(ref, Velocity.getComponentType());
        if (vel != null) {
            vel.addInstruction(new Vector3d(velocity), config, type);
        }
    }
}
