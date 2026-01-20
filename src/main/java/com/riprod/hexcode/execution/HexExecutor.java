package com.riprod.hexcode.execution;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.riprod.hexcode.glyph.Glyph;
import com.riprod.hexcode.glyph.GlyphRegistry;
import com.riprod.hexcode.glyph.GlyphRole;
import com.riprod.hexcode.glyph.effects.EffectGlyph;
import com.riprod.hexcode.glyph.selects.SelectGlyph;
import com.riprod.hexcode.hex.Hex;
import com.riprod.hexcode.hex.HexNode;
import com.riprod.hexcode.util.RaycastUtil;
import com.hypixel.hytale.math.vector.Vector3d;

/**
 * Executes a completed hex on behalf of a caster.
 *
 * Execution order:
 * 1. If no top-level SELECT, implicit SELF[] wraps everything
 * 2. Traverse tree depth-first
 * 3. SELECT establishes targets for all its children
 * 4. Siblings share the same targets from their parent SELECT
 * 5. Delayed SELECTs (BEAM, PROJECTILE) pause execution until hit
 * 6. After ALL delayed SELECTs resolve, remaining siblings execute
 * 7. Nested SELECTs use parent's origin, not sibling's result
 */
public class HexExecutor {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final GlyphRegistry registry;

    public HexExecutor() {
        this.registry = GlyphRegistry.getInstance();
    }

    /**
     * Execute a hex.
     *
     * @param hex The hex to execute
     * @param caster The entity casting the spell
     * @param store The entity store
     * @param world The world
     * @param origin The cast origin position
     * @param direction The cast direction
     * @return Execution result
     */
    public ExecutionResult execute(Hex hex, Ref<EntityStore> caster, Store<EntityStore> store,
                                   World world, Vector3d direction) {
        if (hex == null || hex.getRoot() == null) {
            return ExecutionResult.failure("Hex is empty");
        }

        HexNode root = hex.getRoot();

        // If root is not a SELECT, wrap with implicit SELF
        if (root.getGlyph().getRole() != GlyphRole.SELECT) {
            Glyph selfGlyph = registry.getImplicitSelf();
            HexNode selfNode = new HexNode(selfGlyph);
            selfNode.addChild(root);
            root = selfNode;
        }

        TransformComponent transform = store.getComponent(caster, TransformComponent.getComponentType());
        Vector3d origin = RaycastUtil.getPlayerEyePosition(transform);

        // Create execution context
        ExecutionContext ctx = new ExecutionContext(caster, store, world, origin, direction);

        try {
            // Execute the tree
            executeNode(root, ctx);
            return ExecutionResult.success();
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("Error executing hex");
            return ExecutionResult.failure("Execution error: " + e.getMessage());
        }
    }

    /**
     * Execute a single node and its subtree.
     */
    public void executeNode(HexNode node, ExecutionContext ctx) {
        Glyph glyph = node.getGlyph();

        switch (glyph.getRole()) {
            case SELECT:
                executeSelect(node, ctx);
                break;
            case MODIFIER:
                executeModifier(node, ctx);
                break;
            case EFFECT:
                executeEffect(node, ctx);
                break;
        }
    }

    /**
     * Execute a SELECT glyph.
     */
    private void executeSelect(HexNode node, ExecutionContext ctx) {
        SelectGlyph select = (SelectGlyph) node.getGlyph();

        if (select.isDelayed()) {
            // For delayed selects (BEAM, PROJECTILE), queue for later execution
            // The actual execution happens when the projectile hits something
            executeDelayedSelect(node, ctx, select);
        } else {
            // For instant selects, select targets now and execute children
            TargetSet targets = select.selectTargets(ctx);
            ctx.pushTargets(targets);

            for (HexNode child : node.getChildren()) {
                executeNode(child, ctx);
            }

            ctx.popTargets();
        }
    }

    /**
     * Execute a delayed SELECT (BEAM, PROJECTILE).
     */
    private void executeDelayedSelect(HexNode node, ExecutionContext ctx, SelectGlyph select) {
        DelayedExecutionManager manager = DelayedExecutionManager.getInstance();

        // Get or create the delayed execution state for this SELECT's parent
        // If this is a top-level delayed select, create a new state
        DelayedExecutionManager.DelayedExecutionState state =
            manager.beginDelayedExecution(ctx, node, this);

        // Spawn the appropriate entity based on the select type
        String selectId = select.getId();
        if (selectId.equals("hexcode:beam")) {
            manager.spawnBeam(state, node, select, ctx.getStore());
        } else if (selectId.equals("hexcode:projectile")) {
            manager.spawnProjectile(state, node, select, ctx.getStore());
        } else {
            LOGGER.atWarning().log("Unknown delayed select type: %s", selectId);
        }

        LOGGER.atInfo().log("Delayed select '%s' spawned - waiting for hit", select.getDisplayName());
    }

    /**
     * Execute a MODIFIER glyph.
     */
    private void executeModifier(HexNode node, ExecutionContext ctx) {
        Glyph modifier = node.getGlyph();

        // Push the modifier's multiplier onto the context
        ctx.pushModifier(modifier.getId(), modifier.getModifierMultiplier());

        // Execute the single child
        if (!node.getChildren().isEmpty()) {
            executeNode(node.getChildren().get(0), ctx);
        }

        // Pop the modifier
        ctx.popModifier(modifier.getId());
    }

    /**
     * Execute an EFFECT glyph.
     */
    private void executeEffect(HexNode node, ExecutionContext ctx) {
        EffectGlyph effect = (EffectGlyph) node.getGlyph();
        TargetSet targets = ctx.getCurrentTargets();

        LOGGER.atInfo().log("Executing effect '%s' on %d targets with multiplier %.2f",
                effect.getDisplayName(), targets.getTotalCount(), ctx.getTotalModifierMultiplier());

        // Apply the effect to all targets
        effect.applyEffect(ctx, targets);
    }

    /**
     * Calculate the total mana cost of a hex execution.
     * Formula: Sum of (effect_base_cost × target_multiplier × modifier_multiplier)
     */
    public int calculateManaCost(Hex hex, int estimatedTargetCount) {
        if (hex == null || hex.getRoot() == null) {
            return 0;
        }
        return calculateNodeCost(hex.getRoot(), 1.0f, estimatedTargetCount);
    }

    private int calculateNodeCost(HexNode node, float modifierMultiplier, int targetCount) {
        Glyph glyph = node.getGlyph();

        switch (glyph.getRole()) {
            case EFFECT:
                // Effects contribute their base cost × modifiers × targets
                return Math.round(glyph.getBaseCost() * modifierMultiplier * targetCount);

            case MODIFIER:
                // Modifiers multiply their child's cost
                float newMultiplier = modifierMultiplier * glyph.getCostMultiplier();
                if (!node.getChildren().isEmpty()) {
                    return calculateNodeCost(node.getChildren().get(0), newMultiplier, targetCount);
                }
                return 0;

            case SELECT:
                // Selects sum their children's costs
                int totalCost = 0;
                for (HexNode child : node.getChildren()) {
                    totalCost += calculateNodeCost(child, modifierMultiplier, targetCount);
                }
                return totalCost;

            default:
                return 0;
        }
    }

    /**
     * Result of hex execution.
     */
    public static class ExecutionResult {
        private final boolean success;
        private final String message;
        private final int manaCost;

        private ExecutionResult(boolean success, String message, int manaCost) {
            this.success = success;
            this.message = message;
            this.manaCost = manaCost;
        }

        public static ExecutionResult success() {
            return new ExecutionResult(true, "Success", 0);
        }

        public static ExecutionResult success(int manaCost) {
            return new ExecutionResult(true, "Success", manaCost);
        }

        public static ExecutionResult failure(String message) {
            return new ExecutionResult(false, message, 0);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public int getManaCost() {
            return manaCost;
        }
    }
}
