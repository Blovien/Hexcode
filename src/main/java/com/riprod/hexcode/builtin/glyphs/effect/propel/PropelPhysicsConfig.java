package com.riprod.hexcode.builtin.glyphs.effect.propel;

import com.hypixel.hytale.server.core.modules.projectile.config.StandardPhysicsConfig;

public class PropelPhysicsConfig extends StandardPhysicsConfig {

    public static final PropelPhysicsConfig INSTANCE = new PropelPhysicsConfig();

    private PropelPhysicsConfig() {
        this.gravity = 0;
        this.bounceCount = 0;
        this.sticksVertically = true;
        this.computeYaw = true;
        this.computePitch = true;
        this.terminalVelocityAir = 200.0;
        this.terminalVelocityWater = 100.0;
    }
}
