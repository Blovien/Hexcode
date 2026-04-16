package com.riprod.hexcode.builtin;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.builtin.glyphs.effect.add.AddGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.arc.ArcGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.arc.component.ArcComponent;
import com.riprod.hexcode.builtin.glyphs.effect.bolt.BoltGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.concentration.ConcentrationGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.concentration.ConcentrationConstructHandler;
import com.riprod.hexcode.builtin.glyphs.effect.concentration.component.ConcentrationTriggerComponent;
import com.riprod.hexcode.builtin.glyphs.effect.chaos.ChaosGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.combust.CombustGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.conjure.ConjureGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.conjure.component.ConjureZoneComponent;
import com.riprod.hexcode.builtin.glyphs.effect.conjure.system.ConjureConstructHandler;
import com.riprod.hexcode.builtin.glyphs.effect.debug.DebugGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.delay.DelayGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.arc.ArcConstructHandler;
import com.riprod.hexcode.builtin.glyphs.effect.domain.DomainConstructHandler;
import com.riprod.hexcode.builtin.glyphs.effect.glaciate.GlaciateConstructHandler;
import com.riprod.hexcode.core.common.construct.ConstructRegistry;
import com.riprod.hexcode.core.common.construct.component.HexConstruct;
import com.riprod.hexcode.core.common.construct.system.HexConstructSystem;
import com.riprod.hexcode.core.common.effect.HexEffectHandler;
import com.riprod.hexcode.core.common.effect.HexEffectRegistry;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.riprod.hexcode.builtin.glyphs.effect.domain.DomainGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.domain.component.DomainAuraComponent;
import com.riprod.hexcode.builtin.glyphs.effect.domain.component.DomainZoneComponent;
import com.riprod.hexcode.builtin.glyphs.effect.gust.GustGlyph;
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
import com.riprod.hexcode.builtin.glyphs.effect.phase.PhaseConstructHandler;
import com.riprod.hexcode.builtin.glyphs.effect.phase.PhaseGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.force.ForceGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.fortify.FortifyGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.fortify.component.FortifyComponent;
import com.riprod.hexcode.builtin.glyphs.effect.fortify.system.FortifyDamageSystem;
import com.riprod.hexcode.builtin.glyphs.effect.fortify.system.FortifyTickSystem;
import com.riprod.hexcode.builtin.glyphs.effect.freeze.FreezeConstructHandler;
import com.riprod.hexcode.builtin.glyphs.effect.freeze.FreezeGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.freeze.component.FreezeComponent;
import com.riprod.hexcode.builtin.glyphs.effect.area.AreaGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.glaciate.GlaciateGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.glaciate.component.GlaciateComponent;
import com.riprod.hexcode.builtin.glyphs.effect.greater.GreaterGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.growth.GrowthGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.halt.HaltGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.halt.component.HaltProjectileComponent;
import com.riprod.hexcode.builtin.glyphs.effect.halt.system.HaltProjectileTickSystem;
import com.riprod.hexcode.builtin.glyphs.effect.ignite.IgniteGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.less.LessGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.levitate.LevitateGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.levitate.component.LevitateComponent;
import com.riprod.hexcode.builtin.glyphs.effect.levitate.system.LevitateTickSystem;
import com.riprod.hexcode.builtin.glyphs.effect.multiply.MultiplyGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.output.OutputGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.interfere.InterfereGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.resonate.ResonateGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.projectile.ProjectileConstructHandler;
import com.riprod.hexcode.builtin.glyphs.effect.scale.ScaleConstructHandler;
import com.riprod.hexcode.builtin.glyphs.effect.scale.ScaleGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.scale.component.ScaleComponent;
import com.riprod.hexcode.builtin.glyphs.effect.scale.component.ScaleTargetMarker;
import com.riprod.hexcode.builtin.glyphs.effect.projectile.ProjectileGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.projectile.component.ProjectileComponent;
import com.riprod.hexcode.builtin.glyphs.effect.ensnare.EnsnareConstructHandler;
import com.riprod.hexcode.builtin.glyphs.effect.ensnare.EnsnareGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.ensnare.component.EnsnareComponent;
import com.riprod.hexcode.builtin.glyphs.effect.beam.BeamGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.self.SelfGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.shatter.ShatterConstructHandler;
import com.riprod.hexcode.builtin.glyphs.effect.shatter.ShatterGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.shatter.component.ShatterComponent;
import com.riprod.hexcode.builtin.glyphs.effect.smelt.SmeltGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.subtract.SubtractGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.swap.SwapGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.terraform.TerraformGlyph;
import com.riprod.hexcode.builtin.glyphs.effect.warp.WarpGlyph;
import com.riprod.hexcode.builtin.glyphs.value.IsHoldingValue;
import com.riprod.hexcode.builtin.glyphs.value.NumberValue;
import com.riprod.hexcode.builtin.glyphs.value.PositionValue;
import com.riprod.hexcode.builtin.glyphs.value.RotationValue;
import com.riprod.hexcode.builtin.glyphs.value.VariableValue;
import com.riprod.hexcode.builtin.obelisks.accuracy.AccuracyObelisk;
import com.riprod.hexcode.builtin.obelisks.efficiency.EfficiencyObelisk;
import com.riprod.hexcode.builtin.obelisks.importexport.ImportExportObelisk;
import com.riprod.hexcode.builtin.obelisks.importexport.interactions.ImportInteraction;
import com.riprod.hexcode.builtin.obelisks.seeker.SeekerObelisk;
import com.riprod.hexcode.builtin.styles.ArcStyle;
import com.riprod.hexcode.builtin.styles.RingStyle;
import com.riprod.hexcode.builtin.styles.SphereStyle;
import com.riprod.hexcode.core.common.glyphs.registry.GlyphRegistry;
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
        RegisterConstructs();
        RegisterEffects();
        RegisterInteractions();

        initialized = true;
    }

    private void RegisterGlyphs() {

        // Tier 1
        GlyphRegistry.register("Glyph_Self", new SelfGlyph());
        GlyphRegistry.register("Glyph_Chaos", new ChaosGlyph());
        GlyphRegistry.register("Glyph_Force", new ForceGlyph());
        GlyphRegistry.register("Glyph_Delay", new DelayGlyph());
        GlyphRegistry.register("Glyph_Drain", new DrainGlyph());
        GlyphRegistry.register("Glyph_Halt", new HaltGlyph());

        // Tier 2
        GlyphRegistry.register("Glyph_Beam", new BeamGlyph());
        GlyphRegistry.register("Glyph_Area", new AreaGlyph());
        GlyphRegistry.register("Glyph_Projectile", new ProjectileGlyph());
        GlyphRegistry.register("Glyph_Gust", new GustGlyph());
        GlyphRegistry.register("Glyph_Conjure", new ConjureGlyph());
        GlyphRegistry.register("Glyph_Growth", new GrowthGlyph());
        GlyphRegistry.register("Glyph_Fortify", new FortifyGlyph());
        GlyphRegistry.register("Glyph_Erode", new ErodeGlyph());
        GlyphRegistry.register("Glyph_Interfere", new InterfereGlyph());
        GlyphRegistry.register("Glyph_Resonate", new ResonateGlyph());
        GlyphRegistry.register("Glyph_Levitate", new LevitateGlyph());
        GlyphRegistry.register("Glyph_Scale", new ScaleGlyph());
        GlyphRegistry.register("Glyph_Domain", new DomainGlyph());

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
        GlyphRegistry.register("Glyph_Ensnare", new EnsnareGlyph());
        GlyphRegistry.register("Glyph_Phase", new PhaseGlyph());
        GlyphRegistry.register("Glyph_Warp", new WarpGlyph());
        GlyphRegistry.register("Glyph_Swap", new SwapGlyph());

        // math glyphs (canReadValue + execute)
        GlyphRegistry.register("Glyph_Multiply", new MultiplyGlyph());
        GlyphRegistry.register("Glyph_Add", new AddGlyph());
        GlyphRegistry.register("Glyph_Subtract", new SubtractGlyph());
        GlyphRegistry.register("Glyph_Divide", new DivideGlyph());
        GlyphRegistry.register("Glyph_Equal", new EqualGlyph());
        GlyphRegistry.register("Glyph_Greater", new GreaterGlyph());
        GlyphRegistry.register("Glyph_Less", new LessGlyph());

        // constructor glyphs (canReadValue + execute)
        GlyphRegistry.register("Glyph_Position", new PositionValue());
        GlyphRegistry.register("Glyph_Rotation", new RotationValue());

        // numeric values
        for (int i = 1; i <= 16; i++) {
            GlyphRegistry.register("Number_" + i, new NumberValue(i));
        }
        GlyphRegistry.register("Glyph_Variable", new VariableValue());

        // debug / introspection
        GlyphRegistry.register("Glyph_Debug", new DebugGlyph());

        // output landmark (Wave 2)
        GlyphRegistry.register("Glyph_Output", new OutputGlyph());

        // caster state queries
        GlyphRegistry.register("Glyph_IsHolding", new IsHoldingValue());
        GlyphRegistry.register("Glyph_Concentration", new ConcentrationGlyph());
    }

    private void RegisterObelisks() {
        ObeliskHandlerRegistry.register("seeker", new SeekerObelisk());
        ObeliskHandlerRegistry.register("accuracy", new AccuracyObelisk());
        ObeliskHandlerRegistry.register("efficiency", new EfficiencyObelisk());
        ObeliskHandlerRegistry.register("import_export", new ImportExportObelisk());
    }

    private void RegisterStyles() {
        CastingStyleRegistry.register(new ArcStyle());
        CastingStyleRegistry.register(new RingStyle());
        CastingStyleRegistry.register(new SphereStyle());
    }

    private void RegisterInteractions() {
        Interaction.CODEC.register("HexImportExportInteraction", ImportInteraction.class, ImportInteraction.CODEC);
    }

    private void RegisterComponents() {
        ComponentRegistryProxy<EntityStore> entityStoreRegistry = this.getEntityStoreRegistry();

        // Projectile Component
        ComponentType<EntityStore, ProjectileComponent> projectileComponentType = entityStoreRegistry
                .registerComponent(ProjectileComponent.class, ProjectileComponent::new);
        ProjectileComponent.setComponentType(projectileComponentType);

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

        ComponentType<EntityStore, EnsnareComponent> ensnareComponentType = entityStoreRegistry
                .registerComponent(EnsnareComponent.class, EnsnareComponent::new);
        EnsnareComponent.setComponentType(ensnareComponentType);

        ComponentType<EntityStore, GlaciateComponent> glaciateComponentType = entityStoreRegistry
                .registerComponent(GlaciateComponent.class, GlaciateComponent::new);
        GlaciateComponent.setComponentType(glaciateComponentType);

        ComponentType<EntityStore, ShatterComponent> shatterComponentType = entityStoreRegistry
                .registerComponent(ShatterComponent.class, ShatterComponent::new);
        ShatterComponent.setComponentType(shatterComponentType);

        ComponentType<EntityStore, HaltProjectileComponent> haltProjectileComponentType = entityStoreRegistry
                .registerComponent(HaltProjectileComponent.class, HaltProjectileComponent::new);
        HaltProjectileComponent.setComponentType(haltProjectileComponentType);

        ComponentType<EntityStore, ScaleComponent> scaleComponentType = entityStoreRegistry
                .registerComponent(ScaleComponent.class, ScaleComponent::new);
        ScaleComponent.setComponentType(scaleComponentType);

        ComponentType<EntityStore, ScaleTargetMarker> scaleTargetMarkerType = entityStoreRegistry
                .registerComponent(ScaleTargetMarker.class, ScaleTargetMarker::new);
        ScaleTargetMarker.setComponentType(scaleTargetMarkerType);

        ComponentType<EntityStore, DomainZoneComponent> domainZoneComponentType = entityStoreRegistry
                .registerComponent(DomainZoneComponent.class, DomainZoneComponent::new);
        DomainZoneComponent.setComponentType(domainZoneComponentType);

        ComponentType<EntityStore, DomainAuraComponent> domainAuraComponentType = entityStoreRegistry
                .registerComponent(DomainAuraComponent.class, DomainAuraComponent::new);
        DomainAuraComponent.setComponentType(domainAuraComponentType);

        ComponentType<EntityStore, ConcentrationTriggerComponent> concentrationTriggerType = entityStoreRegistry
                .registerComponent(ConcentrationTriggerComponent.class, ConcentrationTriggerComponent::new);
        ConcentrationTriggerComponent.setComponentType(concentrationTriggerType);

        ComponentType<EntityStore, HexConstruct> hexConstructType = entityStoreRegistry
                .registerComponent(HexConstruct.class, HexConstruct::new);
        HexConstruct.setComponentType(hexConstructType);
    }

    private void RegisterSystems() {
        ComponentRegistryProxy<EntityStore> entityStoreRegistry = this.getEntityStoreRegistry();
        
        entityStoreRegistry.registerSystem(new DrainTickSystem());
        entityStoreRegistry.registerSystem(new ErodeTickSystem());
        entityStoreRegistry.registerSystem(new ErodeDamageSystem());
        entityStoreRegistry.registerSystem(new FortifyTickSystem());
        entityStoreRegistry.registerSystem(new FortifyDamageSystem());
        entityStoreRegistry.registerSystem(new LevitateTickSystem());
        entityStoreRegistry.registerSystem(new HaltProjectileTickSystem());
        entityStoreRegistry.registerSystem(new HexConstructSystem());
    }

    private void RegisterEffects() {
        HexEffectRegistry.register("fortify", new HexEffectHandler() {
            @Override
            public boolean isPresent(CommandBuffer<EntityStore> buffer, Ref<EntityStore> target) {
                return buffer.getComponent(target, FortifyComponent.getComponentType()) != null;
            }

            @Override
            public void strip(CommandBuffer<EntityStore> buffer, Ref<EntityStore> target) {
                buffer.removeComponent(target, FortifyComponent.getComponentType());
                removeEntityEffect(target, "Hexcode_Fortify", buffer);
            }
        });
        HexEffectRegistry.register("erode", new HexEffectHandler() {
            @Override
            public boolean isPresent(CommandBuffer<EntityStore> buffer, Ref<EntityStore> target) {
                return buffer.getComponent(target, ErodeComponent.getComponentType()) != null;
            }

            @Override
            public void strip(CommandBuffer<EntityStore> buffer, Ref<EntityStore> target) {
                buffer.removeComponent(target, ErodeComponent.getComponentType());
                removeEntityEffect(target, "Hexcode_Erode", buffer);
            }
        });
        HexEffectRegistry.register("levitate", new HexEffectHandler() {
            @Override
            public boolean isPresent(CommandBuffer<EntityStore> buffer, Ref<EntityStore> target) {
                return buffer.getComponent(target, LevitateComponent.getComponentType()) != null;
            }

            @Override
            public void strip(CommandBuffer<EntityStore> buffer, Ref<EntityStore> target) {
                buffer.removeComponent(target, LevitateComponent.getComponentType());
                removeEntityEffect(target, "Hexcode_Levitate", buffer);
            }
        });
    }

    private static void removeEntityEffect(Ref<EntityStore> ref, String effectId,
            CommandBuffer<EntityStore> buffer) {
        EffectControllerComponent controller = buffer.getComponent(
                ref, EffectControllerComponent.getComponentType());
        if (controller == null) return;
        int effectIndex = EntityEffect.getAssetMap().getIndex(effectId);
        if (effectIndex != Integer.MIN_VALUE) {
            controller.removeEffect(ref, effectIndex, buffer);
        }
    }

    private void RegisterConstructs() {
        ConstructRegistry.register("scale", new ScaleConstructHandler());
        ConstructRegistry.register("concentration", new ConcentrationConstructHandler());
        ConstructRegistry.register("domain", new DomainConstructHandler());
        ConstructRegistry.register("glaciate", new GlaciateConstructHandler());
        ConstructRegistry.register("arc", new ArcConstructHandler());
        ConstructRegistry.register("phase", new PhaseConstructHandler());
        ConstructRegistry.register("shatter", new ShatterConstructHandler());
        ConstructRegistry.register("conjure", new ConjureConstructHandler());
        ConstructRegistry.register("projectile", new ProjectileConstructHandler());
        ConstructRegistry.register("ensnare", new EnsnareConstructHandler());
        ConstructRegistry.register("freeze", new FreezeConstructHandler());
    }
}
