package com.riprod.hexcode.glyph.selects;

import com.riprod.hexcode.asset.GlyphAssetDefinition;
import com.riprod.hexcode.execution.SpellContext;
import com.riprod.hexcode.glyph.GlyphVisual;
import com.riprod.hexcode.hex.HexNode;
import com.hypixel.hytale.math.vector.Vector3d;

import java.util.UUID;

/**
 * Projectile select glyph - launches a thrown projectile.
 *
 * <p>Projectiles are delayed selects with arc-based travel. They:
 * <ul>
 *   <li>Launch from the caster in their look direction</li>
 *   <li>Travel slower than beams with optional gravity</li>
 *   <li>Execute children on the first entity hit</li>
 *   <li>Can be modified by RANGE (longer distance), SPEED (faster travel), SPLIT (multiple)</li>
 * </ul>
 *
 * <h2>Asset Properties</h2>
 * <ul>
 *   <li>speed: Base travel speed (default 20.0)</li>
 *   <li>range: Maximum travel distance (default 40.0)</li>
 *   <li>gravity: Gravity effect (default 0.0, no arc)</li>
 *   <li>delayed: Always true for projectiles</li>
 * </ul>
 *
 * @see DelayedExecutionManager
 */
public class ProjectileGlyph extends SelectGlyph {

    public static final String ID = "hexcode:projectile";
    public static final float DEFAULT_SPEED = 20.0f;
    public static final float DEFAULT_RANGE = 40.0f;
    public static final float DEFAULT_GRAVITY = 0.0f;

    // Reference to the current hex node (set during execution)
    private HexNode currentNode;

    /**
     * Create a projectile glyph from asset definition.
     *
     * @param assetDefinition The asset definition
     */
    public ProjectileGlyph(GlyphAssetDefinition assetDefinition) {
        super(assetDefinition, GlyphVisual.select("projectile"), true);
    }

    /**
     * Set the current hex node for execution.
     *
     * <p>This is called by the HexExecutor before cast() to provide
     * access to the node's children for delayed execution.
     *
     * @param node The current hex node
     */
    public void setCurrentNode(HexNode node) {
        this.currentNode = node;
    }

    @Override
    protected void selectTargets(SpellContext context) {
        // Queue delayed execution - actual targeting happens when projectile hits
        Vector3d origin = context.getCastOrigin();
        Vector3d direction = context.getCastDirection();

        // Store the execution ID in context metadata
        context.setMetadata("delayType", "projectile");

        // Note: The actual projectile entity spawning happens in DelayedExecutionManager
        // after HexExecutor detects the pendingDelayId metadata
    }

    /**
     * Get the projectile speed from asset properties.
     *
     * @return Base projectile speed in blocks per second
     */
    public float getProjectileSpeed() {
        return getProperty("speed", DEFAULT_SPEED);
    }

    /**
     * Get the projectile range from asset properties.
     *
     * @return Base projectile range in blocks
     */
    public float getProjectileRange() {
        return getProperty("range", DEFAULT_RANGE);
    }

    /**
     * Get the projectile gravity from asset properties.
     *
     * @return Gravity multiplier (0 = no arc, positive = arc down)
     */
    public float getGravity() {
        return getProperty("gravity", DEFAULT_GRAVITY);
    }

    /**
     * Get the effective projectile speed after context modifiers.
     *
     * @param context The spell context
     * @return Modified speed
     */
    public float getEffectiveSpeed(SpellContext context) {
        return getProjectileSpeed() * context.getSpeedMultiplier();
    }

    /**
     * Get the effective projectile range after context modifiers.
     *
     * @param context The spell context
     * @return Modified range
     */
    public float getEffectiveRange(SpellContext context) {
        return getProjectileRange() * context.getRangeMultiplier();
    }
}
