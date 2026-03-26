package com.riprod.hexcode.builtin;

import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.builtin.glyphs.effect.add.AddGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.arc.ArcGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.arc.component.ArcComponent;
import com.riprod.hexcode.builtin.glyphs.effect.arc.system.ArcSystem;
import com.riprod.hexcode.builtin.glyphs.effect.bolt.BoltGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.chaos.ChaosGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.combust.CombustGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.conjure.ConjureGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.conjure.component.ConjureZoneComponent;
import com.riprod.hexcode.builtin.glyphs.effect.conjure.system.ConjureSystem;
import com.riprod.hexcode.builtin.glyphs.effect.delay.DelayGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.detonate.DetonateGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.divide.DivideGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.drain.DrainGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.drain.component.DrainComponent;
import com.riprod.hexcode.builtin.glyphs.effect.drain.system.DrainTickSystem;
import com.riprod.hexcode.builtin.glyphs.effect.equal.EqualGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.erode.ErodeGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.erode.component.ErodeComponent;
import com.riprod.hexcode.builtin.glyphs.effect.erode.system.ErodeDamageSystem;
import com.riprod.hexcode.builtin.glyphs.effect.erode.system.ErodeTickSystem;
import com.riprod.hexcode.builtin.glyphs.effect.phase.PhaseComponent;
import com.riprod.hexcode.builtin.glyphs.effect.phase.PhaseGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.phase.PhaseTickSystem;
import com.riprod.hexcode.builtin.glyphs.effect.force.ForceGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.fortify.FortifyGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.fortify.component.FortifyComponent;
import com.riprod.hexcode.builtin.glyphs.effect.fortify.system.FortifyDamageSystem;
import com.riprod.hexcode.builtin.glyphs.effect.fortify.system.FortifyTickSystem;
import com.riprod.hexcode.builtin.glyphs.effect.freeze.FreezeGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.freeze.component.FreezeComponent;
import com.riprod.hexcode.builtin.glyphs.effect.freeze.system.FreezeTickSystem;
import com.riprod.hexcode.builtin.glyphs.effect.gather.GatherGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.glaciate.GlaciateGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.glaciate.component.GlaciateComponent;
import com.riprod.hexcode.builtin.glyphs.effect.glaciate.system.GlaciateSystem;
import com.riprod.hexcode.builtin.glyphs.effect.greater.GreaterGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.growth.GrowthGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.halt.HaltGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.ignite.IgniteGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.less.LessGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.levitate.LevitateGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.levitate.component.LevitateComponent;
import com.riprod.hexcode.builtin.glyphs.effect.levitate.system.LevitateTickSystem;
import com.riprod.hexcode.builtin.glyphs.effect.multiply.MultiplyGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.interfere.InterfereGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.resonate.ResonateGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.propel.PropelGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.propel.component.PropelComponent;
import com.riprod.hexcode.builtin.glyphs.effect.propel.system.PropelSystem;
import com.riprod.hexcode.builtin.glyphs.effect.rupture.RuptureGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.rupture.component.RuptureComponent;
import com.riprod.hexcode.builtin.glyphs.effect.rupture.system.RuptureTickSystem;
import com.riprod.hexcode.builtin.glyphs.effect.seek.SeekGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.self.SelfGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.shatter.ShatterGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.smelt.SmeltGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.subtract.SubtractGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.swap.SwapGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.terraform.TerraformGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.warp.WarpGlyph;
import com.riprod.hexcode.builtin.glyphs.value.NumberValue;
import com.riprod.hexcode.builtin.glyphs.value.PositionValue;
import com.riprod.hexcode.builtin.glyphs.value.RotationValue;
import com.riprod.hexcode.builtin.glyphs.value.VariableValue;
import com.riprod.hexcode.builtin.obelisks.seeker.SeekerObelisk;
import com.riprod.hexcode.builtin.styles.ArcStyle;
import com.riprod.hexcode.builtin.styles.RingStyle;
import com.riprod.hexcode.builtin.styles.SphereStyle;
import com.riprod.hexcode.core.common.glyphs.registry.GlyphRegistry;
import com.riprod.hexcode.core.state.execution.component.HexSignal;
import com.riprod.hexcode.core.common.glyphs.registry.HexValueRegistry;
import com.riprod.hexcode.core.common.obelisk.registry.ObeliskHandlerRegistry;
import com.riprod.hexcode.core.state.casting.registery.CastingStyleRegistry;

public class BuiltinPlugin extends JavaPlugin {

    private boolean initialized = false;

    public BuiltinPlugin(JavaPluginInit init) {
        super(init);
    }

    public void startup() {
        if (initialized) {
            return;
        }
        RegisterGlyphs();
        RegisterStyles();
        RegisterObelisks();
        RegisterComponents();
        RegisterSystems();

        initialized = true;
    }

    private void RegisterGlyphs() {

        // Tier 1
        SelfGlyph self = new SelfGlyph();
        GlyphRegistry.register("Glyph_Self", self);
        HexValueRegistry.register("Glyph_Self", self);
        GlyphRegistry.register("Glyph_Chaos", new ChaosGlyph());
        GlyphRegistry.register("Glyph_Force", new ForceGlyph());
        GlyphRegistry.register("Glyph_Delay", new DelayGlyph());
        DrainGlyph drain = new DrainGlyph();
        GlyphRegistry.register("Glyph_Drain", drain);
        HexValueRegistry.register("Glyph_Drain", drain);
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
        GlyphRegistry.register("Glyph_Interfere", new InterfereGlyph());
        GlyphRegistry.register("Glyph_Resonate", new ResonateGlyph());
        GlyphRegistry.register("Glyph_Levitate", new LevitateGlyph());
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
        GlyphRegistry.register("Glyph_Phase", new PhaseGlyph());
        GlyphRegistry.register("Glyph_Warp", new WarpGlyph());
        GlyphRegistry.register("Glyph_Swap", new SwapGlyph());
        // hybrid: math glyphs (effect chain + value resolution)
        MultiplyGlyph multiply = new MultiplyGlyph();
        GlyphRegistry.register("Glyph_Multiply", multiply);
        HexValueRegistry.register("Glyph_Multiply", multiply);

        AddGlyph add = new AddGlyph();
        GlyphRegistry.register("Glyph_Add", add);
        HexValueRegistry.register("Glyph_Add", add);

        SubtractGlyph subtract = new SubtractGlyph();
        GlyphRegistry.register("Glyph_Subtract", subtract);
        HexValueRegistry.register("Glyph_Subtract", subtract);

        DivideGlyph divide = new DivideGlyph();
        GlyphRegistry.register("Glyph_Divide", divide);
        HexValueRegistry.register("Glyph_Divide", divide);

        GlyphRegistry.register("Glyph_Equal", new EqualGlyph());
        GlyphRegistry.register("Glyph_Greater", new GreaterGlyph());
        GlyphRegistry.register("Glyph_Less", new LessGlyph());

        // hybrid: constructor glyphs (effect chain + value resolution)
        PositionValue position = new PositionValue();
        GlyphRegistry.register("Glyph_Position", position);
        HexValueRegistry.register("Glyph_Position", position);

        RotationValue rotation = new RotationValue();
        GlyphRegistry.register("Glyph_Rotation", rotation);
        HexValueRegistry.register("Glyph_Rotation", rotation);

        // values
        HexValueRegistry.register("Number_1", new NumberValue(1));
        HexValueRegistry.register("Number_2", new NumberValue(2));
        HexValueRegistry.register("Number_3", new NumberValue(3));
        HexValueRegistry.register("Number_4", new NumberValue(4));
        HexValueRegistry.register("Number_5", new NumberValue(5));
        HexValueRegistry.register("Number_6", new NumberValue(6));
        HexValueRegistry.register("Number_7", new NumberValue(7));
        HexValueRegistry.register("Number_8", new NumberValue(8));
        HexValueRegistry.register("Number_9", new NumberValue(9));
        HexValueRegistry.register("Number_10", new NumberValue(10));
        HexValueRegistry.register("Number_11", new NumberValue(11));
        HexValueRegistry.register("Number_12", new NumberValue(12));
        HexValueRegistry.register("Number_13", new NumberValue(13));
        HexValueRegistry.register("Number_14", new NumberValue(14));
        HexValueRegistry.register("Number_15", new NumberValue(15));
        HexValueRegistry.register("Number_16", new NumberValue(16));
        HexValueRegistry.register("Glyph_Variable", new VariableValue());

    }

    private void RegisterObelisks() {
        ObeliskHandlerRegistry.register("seeker", new SeekerObelisk());
    }

    private void RegisterStyles() {
        CastingStyleRegistry.register(new ArcStyle());
        CastingStyleRegistry.register(new RingStyle());
        CastingStyleRegistry.register(new SphereStyle());
    }

    private void RegisterComponents() {
        ComponentRegistryProxy<EntityStore> entityStoreRegistry = this.getEntityStoreRegistry();

        ComponentType<EntityStore, HexSignal> hexSignalType = entityStoreRegistry
                .registerComponent(HexSignal.class, HexSignal::new);
        HexSignal.setComponentType(hexSignalType);

        // Propel Component
        ComponentType<EntityStore, PropelComponent> propelComponentType = entityStoreRegistry
                .registerComponent(PropelComponent.class, PropelComponent::new);
        PropelComponent.setComponentType(propelComponentType);

        // Conjure Zone Component
        ComponentType<EntityStore, ConjureZoneComponent> conjureZoneType = entityStoreRegistry
                .registerComponent(ConjureZoneComponent.class, ConjureZoneComponent::new);
        ConjureZoneComponent.setComponentType(conjureZoneType);

        // Arc Component
        ComponentType<EntityStore, ArcComponent> arcComponentType = entityStoreRegistry
                .registerComponent(ArcComponent.class, ArcComponent::new);
        ArcComponent.setComponentType(arcComponentType);

        // Drain Component
        ComponentType<EntityStore, DrainComponent> drainComponentType = entityStoreRegistry
                .registerComponent(DrainComponent.class, DrainComponent::new);
        DrainComponent.setComponentType(drainComponentType);

        ComponentType<EntityStore, ErodeComponent> erodeComponentType = entityStoreRegistry
                .registerComponent(ErodeComponent.class, ErodeComponent::new);
        ErodeComponent.setComponentType(erodeComponentType);

        ComponentType<EntityStore, FortifyComponent> fortifyComponentType = entityStoreRegistry
                .registerComponent(FortifyComponent.class, FortifyComponent::new);
        FortifyComponent.setComponentType(fortifyComponentType);

        ComponentType<EntityStore, LevitateComponent> levitateComponentType = entityStoreRegistry
                .registerComponent(LevitateComponent.class, LevitateComponent::new);
        LevitateComponent.setComponentType(levitateComponentType);

        ComponentType<EntityStore, PhaseComponent> phaseComponentType = entityStoreRegistry
                .registerComponent(PhaseComponent.class, PhaseComponent::new);
        PhaseComponent.setComponentType(phaseComponentType);

        ComponentType<EntityStore, FreezeComponent> freezeComponentType = entityStoreRegistry
                .registerComponent(FreezeComponent.class, FreezeComponent::new);
        FreezeComponent.setComponentType(freezeComponentType);

        ComponentType<EntityStore, RuptureComponent> ruptureComponentType = entityStoreRegistry
                .registerComponent(RuptureComponent.class, RuptureComponent::new);
        RuptureComponent.setComponentType(ruptureComponentType);

        ComponentType<EntityStore, GlaciateComponent> glaciateComponentType = entityStoreRegistry
                .registerComponent(GlaciateComponent.class, GlaciateComponent::new);
        GlaciateComponent.setComponentType(glaciateComponentType);
    }

    private void RegisterSystems() {
        ComponentRegistryProxy<EntityStore> entityStoreRegistry = this.getEntityStoreRegistry();
        
        entityStoreRegistry.registerSystem(new PropelSystem());
        entityStoreRegistry.registerSystem(new ConjureSystem());
        entityStoreRegistry.registerSystem(new ArcSystem());
        entityStoreRegistry.registerSystem(new DrainTickSystem());
        entityStoreRegistry.registerSystem(new ErodeTickSystem());
        entityStoreRegistry.registerSystem(new ErodeDamageSystem());
        entityStoreRegistry.registerSystem(new FortifyTickSystem());
        entityStoreRegistry.registerSystem(new FortifyDamageSystem());
        entityStoreRegistry.registerSystem(new LevitateTickSystem());
        entityStoreRegistry.registerSystem(new PhaseTickSystem());
        entityStoreRegistry.registerSystem(new FreezeTickSystem());
        entityStoreRegistry.registerSystem(new RuptureTickSystem());
        entityStoreRegistry.registerSystem(new GlaciateSystem());
    }
}
