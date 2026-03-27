package com.riprod.hexcode.builtin.glyphs.effect.glaciate;

import com.hypixel.hytale.server.core.modules.projectile.config.StandardPhysicsConfig;

public class GlaciatePhysicsConfig extends StandardPhysicsConfig {

    public static final GlaciatePhysicsConfig INSTANCE = new GlaciatePhysicsConfig();

    private GlaciatePhysicsConfig() {
        this.gravity = 20;
        this.bounceCount = -1;
        this.bounciness = 0;
        this.sticksVertically = false;
        this.computeYaw = false;
        this.computePitch = false;
        this.terminalVelocityAir = 50.0;
        this.terminalVelocityWater = 10.0;
    }
}
