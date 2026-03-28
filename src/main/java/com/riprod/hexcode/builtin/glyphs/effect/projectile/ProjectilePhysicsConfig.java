package com.riprod.hexcode.builtin.glyphs.effect.projectile;

import com.hypixel.hytale.server.core.modules.projectile.config.StandardPhysicsConfig;

public class ProjectilePhysicsConfig extends StandardPhysicsConfig {

    public ProjectilePhysicsConfig() {
        this(0, 0);
    }

    public ProjectilePhysicsConfig(double gravity, int bounces) {
        this.gravity = gravity;
        this.bounceCount = bounces;
        this.sticksVertically = true;
        this.computeYaw = true;
        this.computePitch = true;
        this.terminalVelocityAir = 200.0;
        this.terminalVelocityWater = 100.0;
    }
}
