package com.riprod.hexcode.hex;

import com.riprod.hexcode.glyph.Glyph;
import com.riprod.hexcode.glyph.GlyphRegistry;
import com.riprod.hexcode.glyph.GlyphRole;
import com.riprod.hexcode.glyph.selects.SelfGlyph;

/**
 * Builder for creating Hex spell structures programmatically.
 *
 * Provides a fluent API for constructing hex trees:
 * - beam(power(fire())) creates BEAM[POWER[FIRE[]]]
 * - self(fire(), ice()) creates SELF[FIRE[], ICE[]]
 */
public class HexBuilder {
    private final GlyphRegistry registry;

    public HexBuilder() {
        this.registry = GlyphRegistry.getInstance();
    }

    /**
     * Create a hex from a single glyph (will be wrapped with SELF if not a SELECT).
     */
    public Hex build(HexNode root) {
        if (root == null) {
            return new Hex();
        }

        // If root is not a SELECT, wrap with implicit SELF
        if (root.getGlyph().getRole() != GlyphRole.SELECT) {
            HexNode selfNode = new HexNode(registry.getImplicitSelf());
            selfNode.addChild(root);
            return new Hex(selfNode);
        }

        return new Hex(root);
    }

    /**
     * Create a hex from a glyph ID string (e.g., "fire" -> SELF[FIRE[]]).
     */
    public Hex buildFromId(String glyphId) {
        Glyph glyph = registry.getGlyph(glyphId);
        if (glyph == null) {
            glyph = registry.getGlyph("hexcode:" + glyphId);
        }
        if (glyph == null) {
            throw new IllegalArgumentException("Unknown glyph: " + glyphId);
        }
        return build(new HexNode(glyph));
    }

    // ========== FLUENT BUILDER METHODS ==========

    /**
     * Create an EFFECT node.
     */
    public HexNode effect(String id) {
        return node(id);
    }

    /**
     * Create a node for any glyph by ID.
     */
    public HexNode node(String id) {
        String fullId = id.contains(":") ? id : "hexcode:" + id;
        Glyph glyph = registry.getGlyphOrThrow(fullId);
        return new HexNode(glyph);
    }

    /**
     * Create a MODIFIER node wrapping a child.
     */
    public HexNode modifier(String id, HexNode child) {
        HexNode node = node(id);
        if (!node.addChild(child)) {
            throw new IllegalStateException("Cannot add child to " + id);
        }
        return node;
    }

    /**
     * Create a SELECT node wrapping children.
     */
    public HexNode select(String id, HexNode... children) {
        HexNode node = node(id);
        for (HexNode child : children) {
            if (!node.addChild(child)) {
                throw new IllegalStateException("Cannot add child to " + id);
            }
        }
        return node;
    }

    // ========== CONVENIENCE METHODS ==========

    // Effects
    public HexNode fire() { return effect("fire"); }
    public HexNode ice() { return effect("ice"); }
    public HexNode lightning() { return effect("lightning"); }
    public HexNode earth() { return effect("earth"); }
    public HexNode voidEffect() { return effect("void"); }
    public HexNode light() { return effect("light"); }
    public HexNode shield() { return effect("shield"); }
    public HexNode blink() { return effect("blink"); }
    public HexNode heal() { return effect("heal"); }
    public HexNode push() { return effect("push"); }

    // Modifiers
    public HexNode power(HexNode child) { return modifier("power", child); }
    public HexNode range(HexNode child) { return modifier("range", child); }
    public HexNode duration(HexNode child) { return modifier("duration", child); }
    public HexNode speed(HexNode child) { return modifier("speed", child); }
    public HexNode split(HexNode child) { return modifier("split", child); }

    // Selects
    public HexNode self(HexNode... children) { return select("self", children); }
    public HexNode touch(HexNode... children) { return select("touch", children); }
    public HexNode gaze(HexNode... children) { return select("gaze", children); }
    public HexNode beam(HexNode... children) { return select("beam", children); }
    public HexNode projectile(HexNode... children) { return select("projectile", children); }
    public HexNode burst(HexNode... children) { return select("burst", children); }
    public HexNode cone(HexNode... children) { return select("cone", children); }

    // ========== EXAMPLE BUILDS ==========

    /**
     * Example: BEAM[POWER[FIRE[]], ICE[]]
     */
    public static Hex exampleBeamFireIce() {
        HexBuilder b = new HexBuilder();
        return b.build(b.beam(b.power(b.fire()), b.ice()));
    }

    /**
     * Example: SELF[HEAL[]]
     */
    public static Hex exampleSelfHeal() {
        HexBuilder b = new HexBuilder();
        return b.build(b.self(b.heal()));
    }

    /**
     * Example: BURST[POWER[LIGHTNING[]]]
     */
    public static Hex exampleBurstLightning() {
        HexBuilder b = new HexBuilder();
        return b.build(b.burst(b.power(b.lightning())));
    }

    /**
     * Example: PROJECTILE[DURATION[FIRE[]], EARTH[]]
     */
    public static Hex exampleProjectileFireEarth() {
        HexBuilder b = new HexBuilder();
        return b.build(b.projectile(b.duration(b.fire()), b.earth()));
    }
}
