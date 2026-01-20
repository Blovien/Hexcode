package com.riprod.hexcode.hex;

import com.riprod.hexcode.glyph.Glyph;
import com.riprod.hexcode.glyph.GlyphRole;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates hex tree structures for correctness.
 *
 * Checks:
 * - Structure rules (effects are leaves, modifiers have one child, etc.)
 * - Modifier compatibility
 * - Depth limits
 */
public class HexValidator {
    private final int maxDepth;
    private final int maxSiblings;

    public HexValidator() {
        this(10, 8); // Default limits
    }

    public HexValidator(int maxDepth, int maxSiblings) {
        this.maxDepth = maxDepth;
        this.maxSiblings = maxSiblings;
    }

    /**
     * Validate a hex and return validation result.
     *
     * @param hex The hex to validate
     * @return Validation result with errors if any
     */
    public ValidationResult validate(Hex hex) {
        List<String> errors = new ArrayList<>();

        if (hex == null || hex.getRoot() == null) {
            errors.add("Hex is empty");
            return new ValidationResult(false, errors);
        }

        validateNode(hex.getRoot(), 0, errors);

        return new ValidationResult(errors.isEmpty(), errors);
    }

    private void validateNode(HexNode node, int depth, List<String> errors) {
        Glyph glyph = node.getGlyph();
        GlyphRole role = glyph.getRole();

        // Check depth
        if (depth > maxDepth) {
            errors.add("Hex exceeds maximum depth of " + maxDepth);
            return;
        }

        // Validate based on role
        switch (role) {
            case EFFECT:
                validateEffect(node, errors);
                break;
            case MODIFIER:
                validateModifier(node, depth, errors);
                break;
            case SELECT:
                validateSelect(node, depth, errors);
                break;
        }
    }

    private void validateEffect(HexNode node, List<String> errors) {
        // Effects must be leaves (no children)
        if (!node.isLeaf()) {
            errors.add("Effect glyph '" + node.getGlyph().getDisplayName() + "' cannot have children");
        }
    }

    private void validateModifier(HexNode node, int depth, List<String> errors) {
        // Modifiers must have exactly one child
        if (node.getChildCount() != 1) {
            errors.add("Modifier glyph '" + node.getGlyph().getDisplayName() + "' must have exactly one child");
            return;
        }

        HexNode child = node.getChildren().get(0);

        // Check compatibility
        if (!child.getGlyph().isCompatibleWith(node.getGlyph())) {
            errors.add("Modifier '" + node.getGlyph().getDisplayName() + "' is incompatible with '"
                    + child.getGlyph().getDisplayName() + "'");
        }

        // Validate child
        validateNode(child, depth + 1, errors);
    }

    private void validateSelect(HexNode node, int depth, List<String> errors) {
        // Selects must have at least one child
        if (node.getChildCount() == 0) {
            errors.add("Select glyph '" + node.getGlyph().getDisplayName() + "' must have at least one child");
            return;
        }

        // Check sibling limit
        if (node.getChildCount() > maxSiblings) {
            errors.add("Select glyph '" + node.getGlyph().getDisplayName() + "' exceeds maximum of "
                    + maxSiblings + " children");
        }

        // Validate all children
        for (HexNode child : node.getChildren()) {
            validateNode(child, depth + 1, errors);
        }
    }

    /**
     * Check if a modifier can wrap a target glyph.
     *
     * @param modifier The modifier glyph
     * @param target The target glyph
     * @return true if compatible
     */
    public boolean isCompatible(Glyph modifier, Glyph target) {
        if (modifier.getRole() != GlyphRole.MODIFIER) {
            return false;
        }
        return target.isCompatibleWith(modifier);
    }

    /**
     * Result of hex validation.
     */
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;

        public ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors;
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getErrors() {
            return errors;
        }

        public String getErrorMessage() {
            if (errors.isEmpty()) {
                return "Valid";
            }
            return String.join("; ", errors);
        }

        @Override
        public String toString() {
            return valid ? "Valid" : "Invalid: " + getErrorMessage();
        }
    }
}
