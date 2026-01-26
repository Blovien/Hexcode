package com.riprod.hexcode.executing;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.math.vector.Vector3d;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * Runtime context for Hex execution.
 *
 * Maintains the state during spell execution including:
 * - Current caster reference
 * - Target stack (each SELECT pushes targets for its children)
 * - Active modifiers with their multipliers
 * - Delayed execution queue for BEAM/PROJECTILE
 */
public class ExecutionContext {
    private final Ref<EntityStore> caster;
    private final Store<EntityStore> store;
    private final World world;
    private final Vector3d castOrigin;
    private final Vector3d castDirection;

    private final Deque<TargetSet> targetStack;
    private final Map<String, Float> activeModifiers;

    public ExecutionContext(Ref<EntityStore> caster, Store<EntityStore> store, World world,
                            Vector3d castOrigin, Vector3d castDirection) {
        this.caster = caster;
        this.store = store;
        this.world = world;
        this.castOrigin = castOrigin;
        this.castDirection = castDirection;
        this.targetStack = new ArrayDeque<>();
        this.activeModifiers = new HashMap<>();
    }

    /**
     * @return The entity casting the spell
     */
    public Ref<EntityStore> getCaster() {
        return caster;
    }

    /**
     * @return The entity store for accessing components
     */
    public Store<EntityStore> getStore() {
        return store;
    }

    /**
     * @return The world where the spell is being cast
     */
    public World getWorld() {
        return world;
    }

    /**
     * @return The position where the spell was cast from
     */
    public Vector3d getCastOrigin() {
        return castOrigin;
    }

    /**
     * @return The direction the caster was looking when casting
     */
    public Vector3d getCastDirection() {
        return castDirection;
    }

    // ========== TARGET STACK ==========

    /**
     * Push a new target set onto the stack.
     * Used by SELECT glyphs when establishing targets for children.
     */
    public void pushTargets(TargetSet targets) {
        targetStack.push(targets);
    }

    /**
     * Pop the current target set off the stack.
     * Used after a SELECT's children have executed.
     */
    public void popTargets() {
        if (!targetStack.isEmpty()) {
            targetStack.pop();
        }
    }

    /**
     * @return Current targets, or caster as default
     */
    public TargetSet getCurrentTargets() {
        if (targetStack.isEmpty()) {
            return TargetSet.of(caster);
        }
        return targetStack.peek();
    }

    /**
     * @return Origin position for current targeting (last hit point or cast origin)
     */
    public Vector3d getCurrentOrigin() {
        if (!targetStack.isEmpty()) {
            Vector3d origin = targetStack.peek().getOrigin();
            if (origin != null) {
                return origin;
            }
        }
        return castOrigin;
    }

    // ========== MODIFIER STACK ==========

    /**
     * Push a modifier onto the active stack.
     *
     * @param modifierId The modifier's ID
     * @param multiplier The multiplier value
     */
    public void pushModifier(String modifierId, float multiplier) {
        activeModifiers.put(modifierId, multiplier);
    }

    /**
     * Pop a modifier off the active stack.
     *
     * @param modifierId The modifier's ID to remove
     */
    public void popModifier(String modifierId) {
        activeModifiers.remove(modifierId);
    }

    /**
     * Get the multiplier for a specific modifier.
     *
     * @param modifierId The modifier's ID
     * @return The multiplier, or 1.0 if not active
     */
    public float getModifier(String modifierId) {
        return activeModifiers.getOrDefault(modifierId, 1.0f);
    }

    /**
     * @return Product of all active modifier multipliers
     */
    public float getTotalModifierMultiplier() {
        return activeModifiers.values().stream()
                .reduce(1.0f, (a, b) -> a * b);
    }

    /**
     * @return true if any modifiers are currently active
     */
    public boolean hasActiveModifiers() {
        return !activeModifiers.isEmpty();
    }

    /**
     * @return Map of all active modifiers and their multipliers
     */
    public Map<String, Float> getActiveModifiers() {
        return new HashMap<>(activeModifiers);
    }

    // ========== UTILITY ==========

    /**
     * Calculate effective damage/healing amount with all modifiers.
     */
    public float calculateModifiedAmount(float baseAmount) {
        float powerMod = getModifier("hexcode:power");
        return baseAmount * powerMod;
    }

    /**
     * Calculate effective duration with duration modifier.
     */
    public float calculateModifiedDuration(float baseDuration) {
        float durationMod = getModifier("hexcode:duration");
        return baseDuration * durationMod;
    }

    /**
     * Calculate effective range with range modifier.
     */
    public float calculateModifiedRange(float baseRange) {
        float rangeMod = getModifier("hexcode:range");
        return baseRange * rangeMod;
    }

    /**
     * Calculate effective speed with speed modifier.
     */
    public float calculateModifiedSpeed(float baseSpeed) {
        float speedMod = getModifier("hexcode:speed");
        return baseSpeed * speedMod;
    }
}
