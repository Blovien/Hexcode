package com.riprod.hexcode.execution;

import com.riprod.hexcode.glyph.Glyph;
import com.riprod.hexcode.glyph.GlyphRole;
import com.riprod.hexcode.hex.Hex;
import com.riprod.hexcode.hex.HexNode;

/**
 * Calculates mana cost for hexes.
 *
 * Formula: TotalCost = Sum of (effect_base_cost × target_multiplier × modifier_multiplier)
 *
 * Where:
 * - Only EFFECT glyphs have base_cost
 * - target_multiplier = estimated number of targets
 * - modifier_multiplier = product of all modifiers wrapping the effect
 */
public class ManaCostCalculator {
    public static final float MIN_MANA_PERCENTAGE = 0.75f;

    /**
     * Calculate base mana cost (before target multiplier).
     *
     * @param hex The hex to calculate cost for
     * @return Base mana cost
     */
    public int calculateBaseCost(Hex hex) {
        if (hex == null || hex.getRoot() == null) {
            return 0;
        }
        return calculateNodeCost(hex.getRoot(), 1.0f);
    }

    /**
     * Calculate mana cost with estimated target count.
     *
     * @param hex The hex to calculate cost for
     * @param estimatedTargets Estimated number of targets
     * @return Total mana cost
     */
    public int calculateCost(Hex hex, int estimatedTargets) {
        if (hex == null || hex.getRoot() == null) {
            return 0;
        }
        return calculateNodeCost(hex.getRoot(), 1.0f) * Math.max(1, estimatedTargets);
    }

    private int calculateNodeCost(HexNode node, float modifierMultiplier) {
        Glyph glyph = node.getGlyph();
        GlyphRole role = glyph.getRole();

        switch (role) {
            case EFFECT:
                // Effects contribute their base cost × modifiers
                return Math.round(glyph.getBaseCost() * modifierMultiplier);

            case MODIFIER:
                // Modifiers multiply their child's cost
                float newMultiplier = modifierMultiplier * glyph.getCostMultiplier();
                if (!node.getChildren().isEmpty()) {
                    return calculateNodeCost(node.getChildren().get(0), newMultiplier);
                }
                return 0;

            case SELECT:
                // Selects sum their children's costs
                int totalCost = 0;
                for (HexNode child : node.getChildren()) {
                    totalCost += calculateNodeCost(child, modifierMultiplier);
                }
                return totalCost;

            default:
                return 0;
        }
    }

    /**
     * Check if a cast can be attempted with the given mana.
     *
     * @param currentMana Current mana amount
     * @param requiredCost Required mana cost
     * @return CastResult indicating if cast is possible and at what power
     */
    public CastResult canCast(float currentMana, int requiredCost) {
        if (requiredCost <= 0) {
            return CastResult.EMPTY_HEX;
        }

        float percentage = currentMana / requiredCost;

        if (percentage >= 1.0f) {
            // Full power cast
            return new CastResult(true, 1.0f, requiredCost);
        } else if (percentage >= MIN_MANA_PERCENTAGE) {
            // Reduced power cast (use all mana)
            return new CastResult(true, percentage, (int) currentMana);
        } else {
            // Cannot cast
            return CastResult.INSUFFICIENT_MANA;
        }
    }

    /**
     * Result of a mana cost check.
     */
    public static class CastResult {
        public static final CastResult EMPTY_HEX = new CastResult(false, 0, 0);
        public static final CastResult INSUFFICIENT_MANA = new CastResult(false, 0, 0);

        private final boolean canCast;
        private final float powerMultiplier;
        private final int manaCost;

        public CastResult(boolean canCast, float powerMultiplier, int manaCost) {
            this.canCast = canCast;
            this.powerMultiplier = powerMultiplier;
            this.manaCost = manaCost;
        }

        public boolean canCast() {
            return canCast;
        }

        /**
         * @return Power multiplier (1.0 = full power, <1.0 = reduced power)
         */
        public float getPowerMultiplier() {
            return powerMultiplier;
        }

        /**
         * @return Mana that will be consumed
         */
        public int getManaCost() {
            return manaCost;
        }

        public boolean isFullPower() {
            return canCast && powerMultiplier >= 1.0f;
        }

        public boolean isReducedPower() {
            return canCast && powerMultiplier < 1.0f;
        }
    }
}
