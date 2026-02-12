package com.riprod.hexcode.builtin.glyphs.velocity;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.ChangeVelocityType;
import com.hypixel.hytale.server.core.entity.knockback.KnockbackComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.builtin.glyphs.GlyphConstants;

public final class VelocityEffect {
    private VelocityEffect() {}

    public static void applyKnockback(Ref<EntityStore> targetRef, Ref<EntityStore> casterRef, ComponentAccessor<EntityStore> accessor) {
        Vector3d direction = VelocityUtil.getLookDirection(casterRef, accessor);
        if (direction == null) return;

        Vector3d force = new Vector3d(
            direction.x * GlyphConstants.VELOCITY_KNOCKBACK_STRENGTH,
            direction.y * GlyphConstants.VELOCITY_KNOCKBACK_STRENGTH,
            direction.z * GlyphConstants.VELOCITY_KNOCKBACK_STRENGTH
        );

        KnockbackComponent knockback = new KnockbackComponent();
        knockback.setVelocity(force);
        knockback.setVelocityType(ChangeVelocityType.Add);
        knockback.setDuration(GlyphConstants.VELOCITY_KNOCKBACK_DURATION);
        knockback.setTimer(0.0f);
        accessor.putComponent(targetRef, KnockbackComponent.getComponentType(), knockback);
    }
}
