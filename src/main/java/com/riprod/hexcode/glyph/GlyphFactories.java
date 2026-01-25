package com.riprod.hexcode.glyph;

import com.riprod.hexcode.glyph.effects.*;
import com.riprod.hexcode.glyph.modifiers.*;
import com.riprod.hexcode.glyph.selects.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Factory registry for built-in glyph types.
 *
 * <p>Provides factory methods for creating each built-in glyph from an asset definition.
 * Factories are registered by glyph ID and can be looked up via {@link #getFactory(String)}.
 *
 * <h2>Usage</h2>
 * <p>During initialization, assets are loaded and then glyphs are created using factories:
 * <pre>{@code
 * GlyphAssetDefinition fireAsset = assetLoader.loadGlyphAsset("hexcode:fire");
 * GlyphFactory fireFactory = GlyphFactories.getFactory("hexcode:fire");
 * Glyph fireGlyph = fireFactory.create(fireAsset);
 * registry.registerGlyph(fireGlyph);
 * }</pre>
 *
 * <h2>Adding Custom Factories</h2>
 * <p>External plugins can register their own factories:
 * <pre>{@code
 * GlyphFactories.registerFactory("myplugin:custom", MyCustomGlyph::new);
 * }</pre>
 *
 * @see GlyphFactory
 * @see GlyphRegistry
 */
public final class GlyphFactories {

    private static final Map<String, GlyphFactory> FACTORIES = new HashMap<>();

    // ========== EFFECT GLYPH FACTORIES ==========

    public static final GlyphFactory FIRE = FireGlyph::new;
    public static final GlyphFactory ICE = IceGlyph::new;
    public static final GlyphFactory LIGHTNING = LightningGlyph::new;
    public static final GlyphFactory EARTH = EarthGlyph::new;
    public static final GlyphFactory VOID = VoidGlyph::new;
    public static final GlyphFactory LIGHT = LightGlyph::new;
    public static final GlyphFactory SHIELD = ShieldGlyph::new;
    public static final GlyphFactory BLINK = BlinkGlyph::new;
    public static final GlyphFactory HEAL = HealGlyph::new;
    public static final GlyphFactory PUSH = PushGlyph::new;

    // ========== MODIFIER GLYPH FACTORIES ==========

    public static final GlyphFactory POWER = PowerGlyph::new;
    public static final GlyphFactory RANGE = RangeGlyph::new;
    public static final GlyphFactory DURATION = DurationGlyph::new;
    public static final GlyphFactory SPEED = SpeedGlyph::new;

    // ========== SELECT GLYPH FACTORIES ==========

    public static final GlyphFactory SELF = SelfGlyph::new;
    public static final GlyphFactory TOUCH = TouchGlyph::new;
    public static final GlyphFactory BEAM = BeamGlyph::new;
    public static final GlyphFactory PROJECTILE = ProjectileGlyph::new;
    public static final GlyphFactory BURST = BurstGlyph::new;
    public static final GlyphFactory CONE = ConeGlyph::new;

    static {
        // Register all built-in effect factories
        registerFactory("hexcode:fire", FIRE);
        registerFactory("hexcode:ice", ICE);
        registerFactory("hexcode:lightning", LIGHTNING);
        registerFactory("hexcode:earth", EARTH);
        registerFactory("hexcode:void", VOID);
        registerFactory("hexcode:light", LIGHT);
        registerFactory("hexcode:shield", SHIELD);
        registerFactory("hexcode:blink", BLINK);
        registerFactory("hexcode:heal", HEAL);
        registerFactory("hexcode:push", PUSH);

        // Register all built-in modifier factories
        registerFactory("hexcode:power", POWER);
        registerFactory("hexcode:range", RANGE);
        registerFactory("hexcode:duration", DURATION);
        registerFactory("hexcode:speed", SPEED);

        // Register all built-in select factories
        registerFactory("hexcode:self", SELF);
        registerFactory("hexcode:touch", TOUCH);
        registerFactory("hexcode:beam", BEAM);
        registerFactory("hexcode:projectile", PROJECTILE);
        registerFactory("hexcode:burst", BURST);
        registerFactory("hexcode:cone", CONE);
    }

    private GlyphFactories() {
        // Prevent instantiation
    }

    /**
     * Get a factory by glyph ID.
     *
     * @param glyphId The glyph ID (e.g., "hexcode:fire")
     * @return The factory, or null if not found
     */
    public static GlyphFactory getFactory(String glyphId) {
        return FACTORIES.get(glyphId);
    }

    /**
     * Check if a factory exists for the given glyph ID.
     *
     * @param glyphId The glyph ID
     * @return true if a factory is registered
     */
    public static boolean hasFactory(String glyphId) {
        return FACTORIES.containsKey(glyphId);
    }

    /**
     * Register a factory for a glyph ID.
     *
     * <p>External plugins can use this to register custom glyph factories.
     *
     * @param glyphId The glyph ID
     * @param factory The factory to register
     */
    public static void registerFactory(String glyphId, GlyphFactory factory) {
        if (glyphId == null || glyphId.isEmpty()) {
            throw new IllegalArgumentException("Glyph ID cannot be null or empty");
        }
        if (factory == null) {
            throw new IllegalArgumentException("Factory cannot be null");
        }
        FACTORIES.put(glyphId, factory);
    }

    /**
     * Get all registered factory IDs.
     *
     * @return Iterable of registered glyph IDs
     */
    public static Iterable<String> getRegisteredIds() {
        return FACTORIES.keySet();
    }

    /**
     * Get the number of registered factories.
     *
     * @return Factory count
     */
    public static int getFactoryCount() {
        return FACTORIES.size();
    }
}
