package com.riprod.hexcode.builtin;

import com.riprod.hexcode.builtin.glyphs.effect.add.AddGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.anchor.AnchorGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.arc.ArcGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.bolt.BoltGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.chaos.ChaosGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.combust.CombustGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.conjure.ConjureGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.delay.DelayGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.detonate.DetonateGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.divide.DivideGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.drain.DrainGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.erode.ErodeGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.excavate.ExcavateGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.force.ForceGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.fortify.FortifyGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.freeze.FreezeGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.gather.GatherGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.glaciate.GlaciateGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.growth.GrowthGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.halt.HaltGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.ignite.IgniteGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.levitate.LevitateGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.multiply.MultiplyGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.nullify.NullifyGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.position.PositionGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.propel.PropelGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.rotation.RotationGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.rupture.RuptureGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.seek.SeekGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.self.SelfGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.shatter.ShatterGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.smelt.SmeltGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.subtract.SubtractGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.swap.SwapGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.terraform.TerraformGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.warp.WarpGlyph;
import com.riprod.hexcode.builtin.glyphs.value.NumberGlyph;
import com.riprod.hexcode.builtin.styles.ArcStyle;
import com.riprod.hexcode.builtin.styles.RingStyle;
import com.riprod.hexcode.builtin.styles.SphereStyle;
import com.riprod.hexcode.core.common.glyphs.registry.GlyphRegistry;
import com.riprod.hexcode.core.common.glyphs.registry.HexValueRegistry;
import com.riprod.hexcode.core.state.casting.registery.CastingStyleRegistry;

public class BuiltinPlugin {

    private static boolean initialized = false;

    private BuiltinPlugin() {
    }

    public static void startup() {
        if (initialized) {
            return;
        }
        RegisterGlyphs();
        RegisterStyles();

        initialized = true;
    }

    private static void RegisterGlyphs() {

        // Tier 1
        GlyphRegistry.register("Glyph_Self", new SelfGlyph());
        GlyphRegistry.register("Glyph_Chaos", new ChaosGlyph());
        GlyphRegistry.register("Glyph_Force", new ForceGlyph());
        GlyphRegistry.register("Glyph_Delay", new DelayGlyph());
        GlyphRegistry.register("Glyph_Drain", new DrainGlyph());
        GlyphRegistry.register("Glyph_Halt", new HaltGlyph());

        // Tier 2
        GlyphRegistry.register("Glyph_Seek", new SeekGlyph());
        GlyphRegistry.register("Glyph_Gather", new GatherGlyph());
        GlyphRegistry.register("Glyph_Propel", new PropelGlyph());
        GlyphRegistry.register("Glyph_Detonate", new DetonateGlyph());
        GlyphRegistry.register("Glyph_Conjure", new ConjureGlyph());
        GlyphRegistry.register("Glyph_Growth", new GrowthGlyph());
        GlyphRegistry.register("Glyph_Fortify", new FortifyGlyph());
        GlyphRegistry.register("Glyph_Erode", new ErodeGlyph());
        GlyphRegistry.register("Glyph_Nullify", new NullifyGlyph());
        GlyphRegistry.register("Glyph_Levitate", new LevitateGlyph());
        GlyphRegistry.register("Glyph_Anchor", new AnchorGlyph());


        // Tier 3
        GlyphRegistry.register("Glyph_Ignite", new IgniteGlyph());
        GlyphRegistry.register("Glyph_Combust", new CombustGlyph());
        GlyphRegistry.register("Glyph_Smelt", new SmeltGlyph());
        GlyphRegistry.register("Glyph_Bolt", new BoltGlyph());
        GlyphRegistry.register("Glyph_Arc", new ArcGlyph());
        GlyphRegistry.register("Glyph_Freeze", new FreezeGlyph());
        GlyphRegistry.register("Glyph_Shatter", new ShatterGlyph());
        GlyphRegistry.register("Glyph_Glaciate", new GlaciateGlyph());
        GlyphRegistry.register("Glyph_Terraform", new TerraformGlyph());
        GlyphRegistry.register("Glyph_Rupture", new RuptureGlyph());
        GlyphRegistry.register("Glyph_Excavate", new ExcavateGlyph());
        GlyphRegistry.register("Glyph_Warp", new WarpGlyph());
        GlyphRegistry.register("Glyph_Swap", new SwapGlyph());
        GlyphRegistry.register("Glyph_Multiply", new MultiplyGlyph());
        GlyphRegistry.register("Glyph_Add", new AddGlyph());
        GlyphRegistry.register("Glyph_Subtract", new SubtractGlyph());
        GlyphRegistry.register("Glyph_Divide", new DivideGlyph());
        GlyphRegistry.register("Glyph_Rotation", new RotationGlyph());
        GlyphRegistry.register("Glyph_Position", new PositionGlyph());
        
        // Values
        HexValueRegistry.register("Number_1", new NumberGlyph(1));
        HexValueRegistry.register("Number_2", new NumberGlyph(2));
        HexValueRegistry.register("Number_3", new NumberGlyph(3));
        HexValueRegistry.register("Number_4", new NumberGlyph(4));
        HexValueRegistry.register("Number_5", new NumberGlyph(5));
        HexValueRegistry.register("Number_6", new NumberGlyph(6));
        HexValueRegistry.register("Number_7", new NumberGlyph(7));
        HexValueRegistry.register("Number_8", new NumberGlyph(8));
        HexValueRegistry.register("Number_9", new NumberGlyph(9));
        HexValueRegistry.register("Number_10", new NumberGlyph(10));
        HexValueRegistry.register("Number_11", new NumberGlyph(11));
        HexValueRegistry.register("Number_12", new NumberGlyph(12));
        HexValueRegistry.register("Number_13", new NumberGlyph(13));
        HexValueRegistry.register("Number_14", new NumberGlyph(14));
        HexValueRegistry.register("Number_15", new NumberGlyph(15));
        HexValueRegistry.register("Number_16", new NumberGlyph(16));
        HexValueRegistry.register("Value_Variable", new NumberGlyph(16));
        HexValueRegistry.register("Value_Rotation", new NumberGlyph(16));
        HexValueRegistry.register("Value_Position", new NumberGlyph(16));

    }

    private static void RegisterStyles() {
        CastingStyleRegistry.register(new ArcStyle());
        CastingStyleRegistry.register(new RingStyle());
        CastingStyleRegistry.register(new SphereStyle());

    }
}
