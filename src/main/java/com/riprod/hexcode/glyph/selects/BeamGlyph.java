package com.riprod.hexcode.glyph.selects;

import com.riprod.hexcode.asset.GlyphAssetDefinition;
import com.riprod.hexcode.executing.SpellContext;
import com.riprod.hexcode.glyph.GlyphVisual;
import com.riprod.hexcode.hex.HexNode;
import com.hypixel.hytale.math.vector.Vector3d;

import java.util.UUID;

/**
 * Beam select glyph - fires a fast raycast-style projectile.
 *
 * <p>Beams are delayed selects with fast travel time. They:
 * <ul>
 *   <li>Fire from the caster in their look direction</li>
 *   <li>Travel very fast (near-instant)</li>
 *   <li>Execute children on the first entity hit</li>
 *   <li>Can be modified by RANGE (longer distance) and SPEED (faster travel)</li>
 * </ul>
 *
 * <h2>Asset Properties</h2>
 * <ul>
 *   <li>speed: Base travel speed (default 50.0)</li>
 *   <li>range: Maximum travel distance (default 100.0)</li>
 *   <li>delayed: Always true for beams</li>
 * </ul>
 *
 * @see DelayedExecutionManager
 */
public class BeamGlyph extends SelectGlyph {

    public static final String ID = "hexcode:beam";
    public static final float DEFAULT_SPEED = 50.0f;
    public static final float DEFAULT_RANGE = 100.0f;

    // Reference to the current hex node (set during execution)
    private HexNode currentNode;

    /**
     * Create a beam glyph from asset definition.
     *
     * @param assetDefinition The asset definition
     */
    public BeamGlyph(GlyphAssetDefinition assetDefinition) {
        super(assetDefinition, GlyphVisual.select("Beam"), true);
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
        // Queue delayed execution - actual targeting happens when beam hits
        Vector3d origin = context.getCastOrigin();
        Vector3d direction = context.getCastDirection();

        // Store the execution ID in context metadata
        // context.setMetadata("pendingDelayId", executionId);
        context.setMetadata("delayType", "Beam");

        // Note: The actual beam entity spawning happens in DelayedExecutionManager
        // after HexExecutor detects the pendingDelayId metadata
    }

    /**
     * Get the beam speed from asset properties.
     *
     * @return Base beam speed in blocks per second
     */
    public float getBeamSpeed() {
        return getProperty("speed", DEFAULT_SPEED);
    }

    /**
     * Get the beam range from asset properties.
     *
     * @return Base beam range in blocks
     */
    public float getBeamRange() {
        return getProperty("range", DEFAULT_RANGE);
    }

    /**
     * Get the effective beam speed after context modifiers.
     *
     * @param context The spell context
     * @return Modified speed
     */
    public float getEffectiveSpeed(SpellContext context) {
        return getBeamSpeed() * context.getSpeedMultiplier();
    }

    /**
     * Get the effective beam range after context modifiers.
     *
     * @param context The spell context
     * @return Modified range
     */
    public float getEffectiveRange(SpellContext context) {
        return getBeamRange() * context.getRangeMultiplier();
    }
}
