package com.riprod.hexcode.glyph.selects;

import com.riprod.hexcode.asset.GlyphAssetDefinition;
import com.riprod.hexcode.execution.SpellContext;
import com.riprod.hexcode.glyph.Glyph;
import com.riprod.hexcode.glyph.GlyphRole;
import com.riprod.hexcode.glyph.GlyphVisual;

/**
 * Base class for SELECT glyphs - outer shells that determine targeting/delivery.
 *
 * <p>Select glyphs wrap one glyph OR a linked chain of siblings. They determine
 * how targets are selected for the wrapped effects.
 *
 * <h2>Select Types</h2>
 * <ul>
 *   <li><b>Instant</b>: SELF, TOUCH, GAZE, BURST, CONE - targets selected immediately</li>
 *   <li><b>Delayed</b>: BEAM, PROJECTILE - children execute when projectile hits</li>
 * </ul>
 *
 * <h2>Asset-Driven Properties</h2>
 * <p>Selects read their properties from the asset definition:
 * <ul>
 *   <li>basePower - may affect selection parameters</li>
 *   <li>properties.range - base range for targeting</li>
 *   <li>properties.speed - projectile speed for delayed selects</li>
 *   <li>properties.radius - area for burst/cone selects</li>
 *   <li>properties.delayed - whether this is a delayed select</li>
 * </ul>
 *
 * <h2>Execution Flow</h2>
 * <p>The {@link #cast(SpellContext)} method:
 * <ol>
 *   <li>Records this execution in the context</li>
 *   <li>Calls {@link #selectTargets} to populate context targets</li>
 *   <li>Returns the context with targets set</li>
 * </ol>
 *
 * <h2>Delayed Execution</h2>
 * <p>For delayed selects (BEAM, PROJECTILE), the {@link #selectTargets} method
 * should spawn the projectile and queue delayed execution. The context should
 * have "pendingDelayId" metadata set for the executor to handle.
 *
 * @see GlyphAssetDefinition
 * @see SpellContext
 */
public abstract class SelectGlyph implements Glyph {

    private final GlyphAssetDefinition assetDefinition;
    private final GlyphVisual visual;
    private final boolean delayed;

    // Per-execution data
    private float accuracy = 0.75f;
    private float drawSpeed = 0f;

    /**
     * Create a select glyph from an asset definition.
     *
     * @param assetDefinition The asset definition containing all glyph properties
     */
    protected SelectGlyph(GlyphAssetDefinition assetDefinition) {
        this.assetDefinition = assetDefinition;

        // Create visual from asset definition
        String textureName = extractTextureName(assetDefinition.getId());
        this.visual = GlyphVisual.select(textureName);

        // Check if delayed from asset properties
        this.delayed = assetDefinition.getPropertyBoolean("delayed", false);
    }

    /**
     * Create a select glyph with custom visual and delayed flag.
     *
     * @param assetDefinition The asset definition
     * @param visual Custom visual properties
     * @param delayed Whether this is a delayed select
     */
    protected SelectGlyph(GlyphAssetDefinition assetDefinition, GlyphVisual visual, boolean delayed) {
        this.assetDefinition = assetDefinition;
        this.visual = visual;
        this.delayed = delayed;
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

    // ========== DELAYED FLAG ==========

    /**
     * @return true if this select has delayed execution (travel time)
     */
    public boolean isDelayed() {
        return delayed;
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
     * Execute this select glyph.
     *
     * <p>Records execution and selects targets into the context.
     *
     * @param context The spell context
     * @return The context with targets populated
     */
    @Override
    public SpellContext cast(SpellContext context) {
        // Record this execution
        context.recordGlyphExecution(this);

        // Select targets
        selectTargets(context);

        return context;
    }

    // ========== ABSTRACT METHODS ==========

    /**
     * Select targets and populate the context.
     *
     * <p>Implementations should:
     * <ul>
     *   <li>For instant selects: add entities to context.addTarget() and/or
     *       positions to context.addTargetPosition()</li>
     *   <li>For delayed selects: spawn projectile/beam and set
     *       context.setMetadata("pendingDelayId", executionId)</li>
     * </ul>
     *
     * @param context The spell context to populate with targets
     */
    protected abstract void selectTargets(SpellContext context);

    // ========== HELPER METHODS ==========

    /**
     * Get the base range from asset properties.
     *
     * @return Base range in blocks
     */
    protected float getBaseRange() {
        return getProperty("range", 10.0f);
    }

    /**
     * Get the effective range after context multiplier.
     *
     * @param context The spell context
     * @return Modified range
     */
    protected float getModifiedRange(SpellContext context) {
        return getBaseRange() * context.getRangeMultiplier();
    }

    /**
     * Get the base speed from asset properties.
     *
     * @return Base speed (blocks per second for projectiles)
     */
    protected float getBaseSpeed() {
        return getProperty("speed", 20.0f);
    }

    /**
     * Get the effective speed after context multiplier.
     *
     * @param context The spell context
     * @return Modified speed
     */
    protected float getModifiedSpeed(SpellContext context) {
        return getBaseSpeed() * context.getSpeedMultiplier();
    }

    /**
     * Get the base radius from asset properties.
     *
     * @return Base radius in blocks (for burst/cone selects)
     */
    protected float getBaseRadius() {
        return getProperty("radius", 5.0f);
    }

    /**
     * Get the effective radius after context multiplier (uses range).
     *
     * @param context The spell context
     * @return Modified radius
     */
    protected float getModifiedRadius(SpellContext context) {
        return getBaseRadius() * context.getRangeMultiplier();
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
