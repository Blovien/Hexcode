package com.riprod.hexcode.builtin;

import com.riprod.hexcode.builtin.glyphs.beam.Beam;
import com.riprod.hexcode.builtin.glyphs.blink.Blink;
import com.riprod.hexcode.builtin.glyphs.cold.Cold;
import com.riprod.hexcode.builtin.glyphs.death.Death;
import com.riprod.hexcode.builtin.glyphs.delay.Delay;
import com.riprod.hexcode.builtin.glyphs.fire.Fire;
import com.riprod.hexcode.builtin.glyphs.grow.Grow;
import com.riprod.hexcode.builtin.glyphs.ice.Ice;
import com.riprod.hexcode.builtin.glyphs.life.Life;
import com.riprod.hexcode.builtin.glyphs.plasma.Plasma;
import com.riprod.hexcode.builtin.glyphs.projectile.Projectile;
import com.riprod.hexcode.builtin.glyphs.stamina.Stamina;
import com.riprod.hexcode.builtin.glyphs.velocity.Velocity;
import com.riprod.hexcode.builtin.styles.ArcStyle;
import com.riprod.hexcode.builtin.styles.RingStyle;
import com.riprod.hexcode.builtin.styles.SphereStyle;
import com.riprod.hexcode.core.casting.registery.CastingStyleRegistry;
import com.riprod.hexcode.core.glyphs.registry.GlyphRegistry;

public class BuiltinPlugin {

    private static boolean initialized = false;

    private BuiltinPlugin() {
    }

    public static void startup() {
        if (initialized) {
            return;
        }
        RegisterGlyphs();
        RegisterStyles();

        initialized = true;
    }

    private static void RegisterGlyphs() {
        GlyphRegistry.register(new Blink());
        GlyphRegistry.register(new Cold());
        GlyphRegistry.register(new Death());
        GlyphRegistry.register(new Fire());
        GlyphRegistry.register(new Grow());
        GlyphRegistry.register(new Ice());
        GlyphRegistry.register(new Life());
        GlyphRegistry.register(new Plasma());
        GlyphRegistry.register(new Stamina());
        GlyphRegistry.register(new Velocity());
        GlyphRegistry.register(new Projectile());
        GlyphRegistry.register(new Beam());
        GlyphRegistry.register(new Delay());
    }

    private static void RegisterStyles() {
        CastingStyleRegistry.register(new ArcStyle());
        CastingStyleRegistry.register(new RingStyle());
        CastingStyleRegistry.register(new SphereStyle());

    }
}
