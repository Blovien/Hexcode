package com.riprod.hexcode.config;

/**
 * Configuration for the Hexcode mod.
 *
 * Contains all configurable values for glyph mode, composition, casting, and visuals.
 */
public class HexcodeConfig {
    // Singleton instance
    private static HexcodeConfig instance;

    // Glyph Mode settings
    private float staminaDrainPerSecond = 5.0f;
    private float movementSpeedMultiplier = 0.5f;
    private float orbitalRadius = 2.5f;
    private float orbitSpeed = 0.05f;  // Radians per second (full rotation ~125 seconds)
    private float maxCompositionRange = 10.0f;
    private float dragDistance = 2.5f;

    // Composition settings
    private int maxHexDepth = 10;
    private int maxSiblingsPerSelect = 8;
    private int undoStackSize = 20;

    // Casting settings
    private float minManaPercentage = 0.75f;
    private float baseCooldown = 0.5f;

    // Visual settings
    private float glyphLightRadius = 6.0f;
    private float glyphLightIntensity = 10.0f;
    private int dragTrailParticles = 15;
    private float dragTrailDuration = 0.3f;

    // Execution settings
    private float beamSpeed = 50.0f;
    private float projectileSpeed = 20.0f;
    private float burstDefaultRadius = 5.0f;

    private HexcodeConfig() {}

    /**
     * Get the singleton config instance.
     */
    public static HexcodeConfig getInstance() {
        if (instance == null) {
            instance = new HexcodeConfig();
        }
        return instance;
    }

    // ========== GLYPH MODE ==========

    public float getStaminaDrainPerSecond() {
        return staminaDrainPerSecond;
    }

    public void setStaminaDrainPerSecond(float rate) {
        this.staminaDrainPerSecond = rate;
    }

    public float getMovementSpeedMultiplier() {
        return movementSpeedMultiplier;
    }

    public void setMovementSpeedMultiplier(float multiplier) {
        this.movementSpeedMultiplier = multiplier;
    }

    public float getOrbitalRadius() {
        return orbitalRadius;
    }

    public void setOrbitalRadius(float radius) {
        this.orbitalRadius = radius;
    }

    public float getOrbitSpeed() {
        return orbitSpeed;
    }

    public void setOrbitSpeed(float speed) {
        this.orbitSpeed = speed;
    }

    public float getMaxCompositionRange() {
        return maxCompositionRange;
    }

    public void setMaxCompositionRange(float range) {
        this.maxCompositionRange = range;
    }

    public float getDragDistance() {
        return dragDistance;
    }

    public void setDragDistance(float distance) {
        this.dragDistance = distance;
    }

    // ========== COMPOSITION ==========

    public int getMaxHexDepth() {
        return maxHexDepth;
    }

    public void setMaxHexDepth(int depth) {
        this.maxHexDepth = depth;
    }

    public int getMaxSiblingsPerSelect() {
        return maxSiblingsPerSelect;
    }

    public void setMaxSiblingsPerSelect(int max) {
        this.maxSiblingsPerSelect = max;
    }

    public int getUndoStackSize() {
        return undoStackSize;
    }

    public void setUndoStackSize(int size) {
        this.undoStackSize = size;
    }

    // ========== CASTING ==========

    public float getMinManaPercentage() {
        return minManaPercentage;
    }

    public void setMinManaPercentage(float percentage) {
        this.minManaPercentage = percentage;
    }

    public float getBaseCooldown() {
        return baseCooldown;
    }

    public void setBaseCooldown(float cooldown) {
        this.baseCooldown = cooldown;
    }

    // ========== VISUALS ==========

    public float getGlyphLightRadius() {
        return glyphLightRadius;
    }

    public void setGlyphLightRadius(float radius) {
        this.glyphLightRadius = radius;
    }

    public float getGlyphLightIntensity() {
        return glyphLightIntensity;
    }

    public void setGlyphLightIntensity(float intensity) {
        this.glyphLightIntensity = intensity;
    }

    public int getDragTrailParticles() {
        return dragTrailParticles;
    }

    public void setDragTrailParticles(int count) {
        this.dragTrailParticles = count;
    }

    public float getDragTrailDuration() {
        return dragTrailDuration;
    }

    public void setDragTrailDuration(float duration) {
        this.dragTrailDuration = duration;
    }

    // ========== EXECUTION ==========

    public float getBeamSpeed() {
        return beamSpeed;
    }

    public void setBeamSpeed(float speed) {
        this.beamSpeed = speed;
    }

    public float getProjectileSpeed() {
        return projectileSpeed;
    }

    public void setProjectileSpeed(float speed) {
        this.projectileSpeed = speed;
    }

    public float getBurstDefaultRadius() {
        return burstDefaultRadius;
    }

    public void setBurstDefaultRadius(float radius) {
        this.burstDefaultRadius = radius;
    }

    /**
     * Reset config to defaults.
     */
    public void resetToDefaults() {
        staminaDrainPerSecond = 5.0f;
        movementSpeedMultiplier = 0.5f;
        orbitalRadius = 2.5f;
        orbitSpeed = 0.05f;
        maxCompositionRange = 10.0f;
        dragDistance = 2.5f;
        maxHexDepth = 10;
        maxSiblingsPerSelect = 8;
        undoStackSize = 20;
        minManaPercentage = 0.75f;
        baseCooldown = 0.5f;
        glyphLightRadius = 6.0f;
        glyphLightIntensity = 10.0f;
        dragTrailParticles = 15;
        dragTrailDuration = 0.3f;
        beamSpeed = 50.0f;
        projectileSpeed = 20.0f;
        burstDefaultRadius = 5.0f;
    }
}
