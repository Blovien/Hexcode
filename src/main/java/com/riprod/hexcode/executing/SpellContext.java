package com.riprod.hexcode.executing;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.universe.world.World;
import com.riprod.hexcode.glyph.Glyph;

import java.util.*;

/**
 * Runtime context for spell execution.
 *
 * <p>The SpellContext is the central data structure passed between glyphs during
 * execution. It carries all state needed for spell execution including:
 * <ul>
 *   <li>Immutable base info: caster, origin, direction, cast number</li>
 *   <li>Mutable execution state: targets, multipliers</li>
 *   <li>Execution history: which glyphs have executed and how many times</li>
 *   <li>Extensible metadata for custom glyph data</li>
 * </ul>
 *
 * <h2>Context Flow</h2>
 * <p>The context flows differently for Hexes vs Chains:
 * <ul>
 *   <li><b>Hex (nested)</b>: Same context flows through parent → child.
 *       Modifications are visible to all nested children.</li>
 *   <li><b>Chain (sequential)</b>: Each chain element gets a fresh COPY of
 *       the original context. Siblings are isolated.</li>
 * </ul>
 *
 * <h2>Power Decay</h2>
 * <p>The context tracks execution for power decay calculations:
 * <ul>
 *   <li><b>Cast Decay</b>: effectivePower = basePower * (1.0 / castNumber)</li>
 *   <li><b>Glyph Repetition Decay</b>: effectivePower = basePower * (1.0 / executionCount)</li>
 * </ul>
 *
 * @see Glyph#cast(SpellContext)
 */
public class SpellContext {

    // ========== IMMUTABLE BASE INFO ==========

    private final UUID casterId;
    private final Ref<EntityStore> caster;
    private final Store<EntityStore> store;
    private final World world;
    private final Vector3d castOrigin;
    private final Vector3d castDirection;
    private final int castNumber;

    // ========== MUTABLE EXECUTION STATE ==========

    private final List<Ref<EntityStore>> targets;
    private final List<Vector3d> targetPositions;
    private float powerMultiplier;
    private float rangeMultiplier;
    private float durationMultiplier;
    private float speedMultiplier;

    // ========== EXECUTION HISTORY ==========

    private final List<GlyphExecutionRecord> executedGlyphs;
    private final Map<String, Integer> glyphExecutionCounts;

    // ========== EXTENSIBLE METADATA ==========

    private final Map<String, Object> metadata;

    /**
     * Private constructor - use factory methods instead.
     */
    private SpellContext(UUID casterId, Ref<EntityStore> caster, Store<EntityStore> store,
                         World world, Vector3d castOrigin, Vector3d castDirection, int castNumber) {
        this.casterId = casterId;
        this.caster = caster;
        this.store = store;
        this.world = world;
        this.castOrigin = castOrigin;
        this.castDirection = castDirection;
        this.castNumber = castNumber;

        this.targets = new ArrayList<>();
        this.targetPositions = new ArrayList<>();
        this.powerMultiplier = 1.0f;
        this.rangeMultiplier = 1.0f;
        this.durationMultiplier = 1.0f;
        this.speedMultiplier = 1.0f;

        this.executedGlyphs = new ArrayList<>();
        this.glyphExecutionCounts = new HashMap<>();
        this.metadata = new HashMap<>();
    }

    /**
     * Copy constructor for chain isolation.
     */
    private SpellContext(SpellContext source) {
        // Immutable fields - share references
        this.casterId = source.casterId;
        this.caster = source.caster;
        this.store = source.store;
        this.world = source.world;
        this.castOrigin = source.castOrigin;
        this.castDirection = source.castDirection;
        this.castNumber = source.castNumber;

        // Mutable state - deep copy
        this.targets = new ArrayList<>(source.targets);
        this.targetPositions = new ArrayList<>(source.targetPositions);
        this.powerMultiplier = source.powerMultiplier;
        this.rangeMultiplier = source.rangeMultiplier;
        this.durationMultiplier = source.durationMultiplier;
        this.speedMultiplier = source.speedMultiplier;

        // Execution history - deep copy
        this.executedGlyphs = new ArrayList<>(source.executedGlyphs);
        this.glyphExecutionCounts = new HashMap<>(source.glyphExecutionCounts);

        // Metadata - deep copy
        this.metadata = new HashMap<>(source.metadata);
    }

    // ========== FACTORY METHODS ==========

    /**
     * Create a new SpellContext for a spell cast.
     *
     * @param caster The entity casting the spell
     * @param store The entity store for component access
     * @param world The world where the spell is cast
     * @param origin The position where the spell originates
     * @param direction The direction the caster is looking
     * @param castNumber The cast number (1 for first cast, 2 for second, etc.)
     * @return A new SpellContext
     */
    public static SpellContext create(Ref<EntityStore> caster, Store<EntityStore> store,
                                       World world, Vector3d origin, Vector3d direction, int castNumber) {
        UUID casterId = caster != null ? UUID.nameUUIDFromBytes(caster.toString().getBytes()) : UUID.randomUUID();
        return new SpellContext(casterId, caster, store, world, origin, direction, castNumber);
    }

    /**
     * Create a deep copy of this context for chain isolation.
     *
     * <p>Chain elements receive a copy so their modifications don't affect siblings.
     *
     * @return A new SpellContext with copied mutable state
     */
    public SpellContext copy() {
        return new SpellContext(this);
    }

    // ========== IMMUTABLE GETTERS ==========

    /**
     * @return UUID of the caster (derived from entity reference)
     */
    public UUID getCasterId() {
        return casterId;
    }

    /**
     * @return Reference to the casting entity
     */
    public Ref<EntityStore> getCaster() {
        return caster;
    }

    /**
     * @return The entity store for component access
     */
    public Store<EntityStore> getStore() {
        return store;
    }

    /**
     * @return The world where the spell is cast
     */
    public World getWorld() {
        return world;
    }

    /**
     * @return The origin position of the spell cast
     */
    public Vector3d getCastOrigin() {
        return castOrigin;
    }

    /**
     * @return The direction the caster was looking
     */
    public Vector3d getCastDirection() {
        return castDirection;
    }

    /**
     * @return The cast number (1, 2, 3...) for decay calculation
     */
    public int getCastNumber() {
        return castNumber;
    }

    // ========== TARGET MANAGEMENT ==========

    /**
     * Add a target entity.
     */
    public void addTarget(Ref<EntityStore> entity) {
        if (entity != null && !targets.contains(entity)) {
            targets.add(entity);
        }
    }

    /**
     * Add a target position.
     */
    public void addTargetPosition(Vector3d position) {
        if (position != null) {
            targetPositions.add(position);
        }
    }

    /**
     * Set all target entities, replacing any existing.
     */
    public void setTargets(List<Ref<EntityStore>> entities) {
        targets.clear();
        if (entities != null) {
            targets.addAll(entities);
        }
    }

    /**
     * Set all target positions, replacing any existing.
     */
    public void setTargetPositions(List<Vector3d> positions) {
        targetPositions.clear();
        if (positions != null) {
            targetPositions.addAll(positions);
        }
    }

    /**
     * Clear all targets.
     */
    public void clearTargets() {
        targets.clear();
        targetPositions.clear();
    }

    /**
     * @return Unmodifiable list of target entities
     */
    public List<Ref<EntityStore>> getTargets() {
        return Collections.unmodifiableList(targets);
    }

    /**
     * @return Unmodifiable list of target positions
     */
    public List<Vector3d> getTargetPositions() {
        return Collections.unmodifiableList(targetPositions);
    }

    /**
     * @return true if there are any targets (entities or positions)
     */
    public boolean hasTargets() {
        return !targets.isEmpty() || !targetPositions.isEmpty();
    }

    /**
     * @return Number of target entities
     */
    public int getTargetCount() {
        return targets.size();
    }

    /**
     * @return Number of target positions
     */
    public int getPositionCount() {
        return targetPositions.size();
    }

    // ========== MULTIPLIER MANAGEMENT ==========

    /**
     * Multiply the power multiplier by a factor.
     */
    public void multiplyPower(float factor) {
        this.powerMultiplier *= factor;
    }

    /**
     * Multiply the range multiplier by a factor.
     */
    public void multiplyRange(float factor) {
        this.rangeMultiplier *= factor;
    }

    /**
     * Multiply the duration multiplier by a factor.
     */
    public void multiplyDuration(float factor) {
        this.durationMultiplier *= factor;
    }

    /**
     * Multiply the speed multiplier by a factor.
     */
    public void multiplySpeed(float factor) {
        this.speedMultiplier *= factor;
    }

    /**
     * @return Current power multiplier (default 1.0)
     */
    public float getPowerMultiplier() {
        return powerMultiplier;
    }

    /**
     * @return Current range multiplier (default 1.0)
     */
    public float getRangeMultiplier() {
        return rangeMultiplier;
    }

    /**
     * @return Current duration multiplier (default 1.0)
     */
    public float getDurationMultiplier() {
        return durationMultiplier;
    }

    /**
     * @return Current speed multiplier (default 1.0)
     */
    public float getSpeedMultiplier() {
        return speedMultiplier;
    }

    /**
     * Get effective power including cast decay and context multiplier.
     *
     * @return Effective power multiplier with cast decay applied
     */
    public float getEffectivePower() {
        float castDecay = 1.0f / castNumber;
        return powerMultiplier * castDecay;
    }

    /**
     * Get effective range with context multiplier.
     */
    public float getEffectiveRange() {
        return rangeMultiplier;
    }

    /**
     * Get effective duration with context multiplier.
     */
    public float getEffectiveDuration() {
        return durationMultiplier;
    }

    /**
     * Get effective speed with context multiplier.
     */
    public float getEffectiveSpeed() {
        return speedMultiplier;
    }

    // ========== EXECUTION TRACKING ==========

    /**
     * Record that a glyph has executed.
     *
     * <p>Updates execution counts for decay calculations.
     *
     * @param glyph The glyph that executed
     */
    public void recordGlyphExecution(Glyph glyph) {
        String glyphId = glyph.getId();

        // Increment execution count
        int count = glyphExecutionCounts.getOrDefault(glyphId, 0) + 1;
        glyphExecutionCounts.put(glyphId, count);

        // Add execution record
        GlyphExecutionRecord record = new GlyphExecutionRecord(
                glyphId,
                System.currentTimeMillis(),
                glyph.getAccuracy(),
                targets.size()
        );
        executedGlyphs.add(record);
    }

    /**
     * Get how many times a glyph has executed in this spell.
     *
     * @param glyphId The glyph ID
     * @return Execution count (0 if never executed)
     */
    public int getGlyphExecutionCount(String glyphId) {
        return glyphExecutionCounts.getOrDefault(glyphId, 0);
    }

    /**
     * @return Unmodifiable list of execution records
     */
    public List<GlyphExecutionRecord> getExecutionHistory() {
        return Collections.unmodifiableList(executedGlyphs);
    }

    /**
     * Calculate effective power for a glyph with both cast decay and repetition decay.
     *
     * <p>Formula: basePower * (1.0 / castNumber) * (1.0 / executionCount)
     *
     * @param glyph The glyph to calculate power for
     * @return Effective power with all decay applied
     */
    public float calculateDecayedPower(Glyph glyph) {
        float basePower = glyph.getAssetDefinition().getBasePower();

        // Cast decay
        float castDecay = 1.0f / castNumber;

        // Repetition decay (current execution will be count + 1)
        int executionCount = getGlyphExecutionCount(glyph.getId()) + 1;
        float repetitionDecay = 1.0f / executionCount;

        // Apply accuracy bonus (optional)
        float accuracyBonus = glyph.getAccuracy();

        return basePower * powerMultiplier * castDecay * repetitionDecay * accuracyBonus;
    }

    // ========== METADATA ==========

    /**
     * Set a metadata value.
     *
     * @param key The metadata key
     * @param value The value to store
     */
    public void setMetadata(String key, Object value) {
        metadata.put(key, value);
    }

    /**
     * Get a metadata value.
     *
     * @param key The metadata key
     * @param type The expected type
     * @return The value, or null if not found or wrong type
     */
    @SuppressWarnings("unchecked")
    public <T> T getMetadata(String key, Class<T> type) {
        Object value = metadata.get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    /**
     * Check if metadata exists.
     */
    public boolean hasMetadata(String key) {
        return metadata.containsKey(key);
    }

    /**
     * Remove metadata.
     */
    public void removeMetadata(String key) {
        metadata.remove(key);
    }

    // ========== INNER CLASS ==========

    /**
     * Record of a single glyph execution.
     */
    public static class GlyphExecutionRecord {
        private final String glyphId;
        private final long timestamp;
        private final float accuracy;
        private final int resultingTargetCount;

        public GlyphExecutionRecord(String glyphId, long timestamp, float accuracy, int resultingTargetCount) {
            this.glyphId = glyphId;
            this.timestamp = timestamp;
            this.accuracy = accuracy;
            this.resultingTargetCount = resultingTargetCount;
        }

        public String getGlyphId() {
            return glyphId;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public float getAccuracy() {
            return accuracy;
        }

        public int getResultingTargetCount() {
            return resultingTargetCount;
        }

        @Override
        public String toString() {
            return String.format("GlyphExecutionRecord{glyphId='%s', accuracy=%.2f, targets=%d}",
                    glyphId, accuracy, resultingTargetCount);
        }
    }

    @Override
    public String toString() {
        return String.format("SpellContext{castNumber=%d, targets=%d, positions=%d, power=%.2f, range=%.2f, duration=%.2f}",
                castNumber, targets.size(), targetPositions.size(), powerMultiplier, rangeMultiplier, durationMultiplier);
    }
}
