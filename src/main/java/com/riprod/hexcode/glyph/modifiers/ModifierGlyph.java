package com.riprod.hexcode.glyph.modifiers;

import com.riprod.hexcode.asset.GlyphAssetDefinition;
import com.riprod.hexcode.executing.SpellContext;
import com.riprod.hexcode.glyph.Glyph;
import com.riprod.hexcode.glyph.GlyphRole;
import com.riprod.hexcode.glyph.GlyphVisual;

/**
 * Base class for MODIFIER glyphs - inner shells that amplify/alter behavior.
 *
 * <p>Modifier glyphs wrap exactly one glyph and only affect that direct child.
 * They modify the spell context (power, range, duration, speed) before child
 * execution.
 *
 * <h2>Asset-Driven Properties</h2>
 * <p>Modifiers read their multiplier from the asset definition's basePower field.
 * For example, a power modifier with basePower=1.5 increases power by 50%.
 *
 * <h2>Execution Flow</h2>
 * <p>The {@link #cast(SpellContext)} method:
 * <ol>
 *   <li>Records this execution in the context</li>
 *   <li>Calls {@link #applyModifier} to modify the context</li>
 *   <li>Returns the modified context for child glyphs</li>
 * </ol>
 *
 * <h2>Modifier Types</h2>
 * Standard modifiers affect context multipliers:
 * <ul>
 *   <li>POWER - context.multiplyPower(multiplier)</li>
 *   <li>RANGE - context.multiplyRange(multiplier)</li>
 *   <li>DURATION - context.multiplyDuration(multiplier)</li>
 *   <li>SPEED - context.multiplySpeed(multiplier)</li>
 * </ul>
 *
 * @see GlyphAssetDefinition
 * @see SpellContext
 */
public abstract class ModifierGlyph implements Glyph {

    private final GlyphAssetDefinition assetDefinition;
    private final GlyphVisual visual;

    // Per-execution data
    private float accuracy = 0.75f;
    private float drawSpeed = 0f;

    /**
     * Create a modifier glyph from an asset definition.
     *
     * @param assetDefinition The asset definition containing all glyph properties
     */
    protected ModifierGlyph(GlyphAssetDefinition assetDefinition) {
        this.assetDefinition = assetDefinition;

        // Create visual from asset definition
        String textureName = extractTextureName(assetDefinition.getId());
        this.visual = GlyphVisual.modifier(textureName);
    }

    /**
     * Create a modifier glyph with custom visual.
     *
     * @param assetDefinition The asset definition
     * @param visual Custom visual properties
     */
    protected ModifierGlyph(GlyphAssetDefinition assetDefinition, GlyphVisual visual) {
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

    @Override
    public GlyphRole getRole() {
        return GlyphRole.MODIFIER;
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
     * Execute this modifier glyph.
     *
     * <p>Records execution and applies the modifier to the context.
     *
     * @param context The spell context
     * @return The modified context for child glyphs
     */
    @Override
    public SpellContext cast(SpellContext context) {
        // Record this execution
        context.recordGlyphExecution(this);

        // Apply the modifier
        applyModifier(context);

        return context;
    }

    // ========== ABSTRACT METHODS ==========

    /**
     * Apply this modifier's effect to the context.
     *
     * <p>Subclasses implement this to modify context multipliers.
     * For example, PowerGlyph calls context.multiplyPower(getMultiplier()).
     *
     * @param context The spell context to modify
     */
    protected abstract void applyModifier(SpellContext context);

    // ========== HELPER METHODS ==========

    /**
     * Get the multiplier value from the asset definition.
     *
     * <p>Uses basePower as the multiplier. For example, basePower=1.5
     * means a 50% increase.
     *
     * @return The multiplier value
     */
    protected float getMultiplier() {
        return assetDefinition.getBasePower();
    }

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
     * Extract texture name from glyph ID.
     */
    private String extractTextureName(String id) {
        int colonIndex = id.lastIndexOf(':');
        return colonIndex >= 0 ? id.substring(colonIndex + 1) : id;
    }
}
