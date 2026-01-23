package com.riprod.hexcode.glyph.effects;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.asset.GlyphAssetDefinition;
import com.riprod.hexcode.data.GlyphInstance;
import com.riprod.hexcode.execution.SpellContext;
import com.riprod.hexcode.glyph.Glyph;
import com.riprod.hexcode.glyph.GlyphRole;
import com.riprod.hexcode.glyph.GlyphVisual;

/**
 * Base class for EFFECT glyphs - leaf nodes that perform actions.
 *
 * <p>Effect glyphs are always leaves in the Hex tree. They cannot contain
 * other glyphs and are where actual spell effects are applied (damage,
 * healing, utility effects).
 *
 * <h2>Asset-Driven Properties</h2>
 * <p>All effect properties are loaded from the asset definition:
 * <ul>
 *   <li>basePower - multiplier for effect strength</li>
 *   <li>baseManaCost - mana cost before modifiers</li>
 *   <li>properties - role-specific values (damage, duration, etc.)</li>
 * </ul>
 *
 * <h2>Execution Flow</h2>
 * <p>The {@link #cast(SpellContext)} method:
 * <ol>
 *   <li>Records this execution in the context</li>
 *   <li>Calculates effective power with decay</li>
 *   <li>Calls {@link #applyEffect} for each target entity</li>
 *   <li>Calls {@link #applyEffectAtPosition} for each target position</li>
 * </ol>
 *
 * @see GlyphAssetDefinition
 * @see SpellContext
 */
public abstract class EffectGlyph implements Glyph {

    private final GlyphAssetDefinition assetDefinition;
    private final GlyphVisual visual;

    // Per-execution data
    private float accuracy = 0.75f;  // Default for starter glyphs
    private float drawSpeed = 0f;

    /**
     * Create an effect glyph from an asset definition.
     *
     * @param assetDefinition The asset definition containing all glyph properties
     */
    protected EffectGlyph(GlyphAssetDefinition assetDefinition) {
        this.assetDefinition = assetDefinition;

        // Create visual from asset definition
        String textureName = extractTextureName(assetDefinition.getId());
        this.visual = GlyphVisual.effect(GlyphVisual.COLOR_FIRE, textureName); // Color will be from asset later
    }

    /**
     * Create an effect glyph with custom visual.
     *
     * @param assetDefinition The asset definition
     * @param visual Custom visual properties
     */
    protected EffectGlyph(GlyphAssetDefinition assetDefinition, GlyphVisual visual) {
        this.assetDefinition = assetDefinition;
        this.visual = visual;
    }

    // ========== IDENTITY ==========

    @Override
    public String getId() {
        return assetDefinition.getId();
    }

    @Override
    public String getDisplayName() {
        return assetDefinition.getDisplayName();
    }

    @Override
    public GlyphAssetDefinition getAssetDefinition() {
        return assetDefinition;
    }

    @Override
    public GlyphVisual getVisual() {
        return visual;
    }

    // ========== EXECUTION DATA ==========

    @Override
    public float getAccuracy() {
        return accuracy;
    }

    @Override
    public float getDrawSpeed() {
        return drawSpeed;
    }

    @Override
    public void setExecutionData(float accuracy, float drawSpeed) {
        this.accuracy = accuracy;
        this.drawSpeed = drawSpeed;
    }

    // ========== MANA CALCULATION ==========

    @Override
    public float calculateManaCost(SpellContext context) {
        return assetDefinition.getBaseManaCost();
    }

    // ========== EXECUTION ==========

    /**
     * Execute this effect glyph.
     *
     * <p>Records execution, calculates power, and applies effects to all targets.
     *
     * @param context The spell context
     * @return The context (possibly modified)
     */
    @Override
    public SpellContext cast(SpellContext context) {
        // Record this execution
        context.recordGlyphExecution(this);

        // Calculate effective power with decay
        float effectivePower = context.calculateDecayedPower(this);

        // Apply effect to all target entities
        for (Ref<EntityStore> target : context.getTargets()) {
            applyEffect(context, target, effectivePower);
        }

        // Apply effect at all target positions
        for (Vector3d position : context.getTargetPositions()) {
            applyEffectAtPosition(context, position, effectivePower);
        }

        return context;
    }

    // ========== ABSTRACT METHODS ==========

    /**
     * Apply the effect to a target entity.
     *
     * @param context The spell context
     * @param target The target entity
     * @param power The effective power (after decay calculations)
     */
    protected abstract void applyEffect(SpellContext context, Ref<EntityStore> target, float power);

    /**
     * Apply the effect at a position.
     *
     * <p>Override this for effects that can target positions (like creating
     * fire at a location). Default implementation does nothing.
     *
     * @param context The spell context
     * @param position The target position
     * @param power The effective power
     */
    protected void applyEffectAtPosition(SpellContext context, Vector3d position, float power) {
        // Default: no position-based effect
    }

    // ========== HELPER METHODS ==========

    /**
     * Get a float property from the asset definition.
     *
     * @param key Property key
     * @param defaultValue Default if not found
     * @return The property value
     */
    protected float getProperty(String key, float defaultValue) {
        return assetDefinition.getPropertyFloat(key, defaultValue);
    }

    /**
     * Get a String property from the asset definition.
     *
     * @param key Property key
     * @param defaultValue Default if not found
     * @return The property value
     */
    protected String getProperty(String key, String defaultValue) {
        return assetDefinition.getPropertyString(key, defaultValue);
    }

    /**
     * Get an int property from the asset definition.
     *
     * @param key Property key
     * @param defaultValue Default if not found
     * @return The property value
     */
    protected int getProperty(String key, int defaultValue) {
        return assetDefinition.getPropertyInt(key, defaultValue);
    }

    /**
     * Get a boolean property from the asset definition.
     *
     * @param key Property key
     * @param defaultValue Default if not found
     * @return The property value
     */
    protected boolean getProperty(String key, boolean defaultValue) {
        return assetDefinition.getPropertyBoolean(key, defaultValue);
    }

    /**
     * Calculate modified amount using context power multiplier.
     *
     * @param baseAmount The base amount
     * @param context The spell context
     * @return Amount multiplied by context power
     */
    protected float getModifiedAmount(float baseAmount, SpellContext context) {
        return baseAmount * context.getPowerMultiplier();
    }

    /**
     * Calculate modified duration using context duration multiplier.
     *
     * @param baseDuration The base duration
     * @param context The spell context
     * @return Duration multiplied by context duration
     */
    protected float getModifiedDuration(float baseDuration, SpellContext context) {
        return baseDuration * context.getDurationMultiplier();
    }

    /**
     * Extract texture name from glyph ID.
     */
    private String extractTextureName(String id) {
        int colonIndex = id.lastIndexOf(':');
        return colonIndex >= 0 ? id.substring(colonIndex + 1) : id;
    }
}
