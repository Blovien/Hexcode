package com.riprod.hexcode.builtin;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.construct.component.HexEffectsComponent;
import com.riprod.hexcode.core.common.construct.registry.ConstructRegistry;
import com.riprod.hexcode.core.common.effect.HexEffectHandler;
import com.riprod.hexcode.core.common.effect.HexEffectRegistry;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.riprod.hexcode.builtin.glyphs.isHolding.IsHoldingValue;
import com.riprod.hexcode.builtin.glyphs.add.AddGlyph;
import com.riprod.hexcode.builtin.glyphs.arc.ArcConstructHandler;
import com.riprod.hexcode.builtin.glyphs.arc.ArcGlyph;
import com.riprod.hexcode.builtin.glyphs.arc.component.ArcComponent;
import com.riprod.hexcode.builtin.glyphs.area.AreaGlyph;
import com.riprod.hexcode.builtin.glyphs.beam.BeamGlyph;
import com.riprod.hexcode.builtin.glyphs.bolt.BoltGlyph;
import com.riprod.hexcode.builtin.glyphs.chaos.ChaosGlyph;
import com.riprod.hexcode.builtin.glyphs.combust.CombustGlyph;
import com.riprod.hexcode.builtin.glyphs.concentration.ConcentrationConstructHandler;
import com.riprod.hexcode.builtin.glyphs.concentration.ConcentrationGlyph;
import com.riprod.hexcode.builtin.glyphs.concentration.component.ConcentrationTriggerComponent;
import com.riprod.hexcode.builtin.glyphs.conjure.ConjureGlyph;
import com.riprod.hexcode.builtin.glyphs.conjure.component.ConjureZoneComponent;
import com.riprod.hexcode.builtin.glyphs.conjure.system.ConjureConstructHandler;
import com.riprod.hexcode.builtin.glyphs.debug.DebugGlyph;
import com.riprod.hexcode.builtin.glyphs.delay.DelayGlyph;
import com.riprod.hexcode.builtin.glyphs.divide.DivideGlyph;
import com.riprod.hexcode.builtin.glyphs.domain.DomainConstructHandler;
import com.riprod.hexcode.builtin.glyphs.domain.DomainGlyph;
import com.riprod.hexcode.builtin.glyphs.domain.component.DomainAuraComponent;
import com.riprod.hexcode.builtin.glyphs.domain.component.DomainZoneComponent;
import com.riprod.hexcode.builtin.glyphs.drain.DrainGlyph;
import com.riprod.hexcode.builtin.glyphs.drain.component.DrainComponent;
import com.riprod.hexcode.builtin.glyphs.drain.system.DrainTickSystem;
import com.riprod.hexcode.builtin.glyphs.ensnare.EnsnareConstructHandler;
import com.riprod.hexcode.builtin.glyphs.ensnare.EnsnareGlyph;
import com.riprod.hexcode.builtin.glyphs.ensnare.component.EnsnareComponent;
import com.riprod.hexcode.builtin.glyphs.equal.EqualGlyph;
import com.riprod.hexcode.builtin.glyphs.erode.ErodeGlyph;
import com.riprod.hexcode.builtin.glyphs.erode.component.ErodeComponent;
import com.riprod.hexcode.builtin.glyphs.erode.system.ErodeDamageSystem;
import com.riprod.hexcode.builtin.glyphs.erode.system.ErodeTickSystem;
import com.riprod.hexcode.builtin.glyphs.force.ForceGlyph;
import com.riprod.hexcode.builtin.glyphs.fortify.FortifyGlyph;
import com.riprod.hexcode.builtin.glyphs.fortify.component.FortifyComponent;
import com.riprod.hexcode.builtin.glyphs.fortify.system.FortifyDamageSystem;
import com.riprod.hexcode.builtin.glyphs.fortify.system.FortifyTickSystem;
import com.riprod.hexcode.builtin.glyphs.freeze.FreezeConstructHandler;
import com.riprod.hexcode.builtin.glyphs.freeze.FreezeGlyph;
import com.riprod.hexcode.builtin.glyphs.freeze.component.FreezeComponent;
import com.riprod.hexcode.builtin.glyphs.glaciate.GlaciateConstructHandler;
import com.riprod.hexcode.builtin.glyphs.glaciate.GlaciateGlyph;
import com.riprod.hexcode.builtin.glyphs.glaciate.component.GlaciateComponent;
import com.riprod.hexcode.builtin.glyphs.greater.GreaterGlyph;
import com.riprod.hexcode.builtin.glyphs.growth.GrowthGlyph;
import com.riprod.hexcode.builtin.glyphs.gust.GustGlyph;
import com.riprod.hexcode.builtin.glyphs.halt.HaltGlyph;
import com.riprod.hexcode.builtin.glyphs.halt.component.HaltProjectileComponent;
import com.riprod.hexcode.builtin.glyphs.halt.system.HaltProjectileTickSystem;
import com.riprod.hexcode.builtin.glyphs.ignite.IgniteGlyph;
import com.riprod.hexcode.builtin.glyphs.interfere.InterfereGlyph;
import com.riprod.hexcode.builtin.glyphs.less.LessGlyph;
import com.riprod.hexcode.builtin.glyphs.levitate.LevitateGlyph;
import com.riprod.hexcode.builtin.glyphs.levitate.component.LevitateComponent;
import com.riprod.hexcode.builtin.glyphs.levitate.system.LevitateTickSystem;
import com.riprod.hexcode.builtin.glyphs.multiply.MultiplyGlyph;
import com.riprod.hexcode.builtin.glyphs.number.NumberValue;
import com.riprod.hexcode.builtin.glyphs.output.OutputGlyph;
import com.riprod.hexcode.builtin.glyphs.phase.PhaseComponent;
import com.riprod.hexcode.builtin.glyphs.phase.PhaseConstructHandler;
import com.riprod.hexcode.builtin.glyphs.phase.PhaseGlyph;
import com.riprod.hexcode.builtin.glyphs.position.PositionValue;
import com.riprod.hexcode.builtin.glyphs.projectile.ProjectileConstructHandler;
import com.riprod.hexcode.builtin.glyphs.projectile.ProjectileGlyph;
import com.riprod.hexcode.builtin.glyphs.projectile.component.ProjectileComponent;
import com.riprod.hexcode.builtin.glyphs.resonate.ResonateGlyph;
import com.riprod.hexcode.builtin.glyphs.rotation.RotationValue;
import com.riprod.hexcode.builtin.glyphs.scale.ScaleConstructHandler;
import com.riprod.hexcode.builtin.glyphs.scale.ScaleGlyph;
import com.riprod.hexcode.builtin.glyphs.scale.component.ScaleComponent;
import com.riprod.hexcode.builtin.glyphs.scale.component.ScaleTargetMarker;
import com.riprod.hexcode.builtin.glyphs.self.SelfGlyph;
import com.riprod.hexcode.builtin.glyphs.shatter.ShatterConstructHandler;
import com.riprod.hexcode.builtin.glyphs.shatter.ShatterGlyph;
import com.riprod.hexcode.builtin.glyphs.shatter.component.ShatterComponent;
import com.riprod.hexcode.builtin.glyphs.smelt.SmeltGlyph;
import com.riprod.hexcode.builtin.glyphs.subtract.SubtractGlyph;
import com.riprod.hexcode.builtin.glyphs.swap.SwapGlyph;
import com.riprod.hexcode.builtin.glyphs.terraform.TerraformGlyph;
import com.riprod.hexcode.builtin.glyphs.variable.VariableValue;
import com.riprod.hexcode.builtin.glyphs.warp.WarpGlyph;
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
        GlyphRegistry.register(new SelfGlyph());
        GlyphRegistry.register(new ChaosGlyph());
        GlyphRegistry.register(new ForceGlyph());
        GlyphRegistry.register(new DelayGlyph());
        GlyphRegistry.register(new DrainGlyph());
        GlyphRegistry.register(new HaltGlyph());

        // Tier 2
        GlyphRegistry.register(new BeamGlyph());
        GlyphRegistry.register(new AreaGlyph());
        GlyphRegistry.register(new ProjectileGlyph());
        GlyphRegistry.register(new GustGlyph());
        GlyphRegistry.register(new ConjureGlyph());
        GlyphRegistry.register(new GrowthGlyph());
        GlyphRegistry.register(new FortifyGlyph());
        GlyphRegistry.register(new ErodeGlyph());
        GlyphRegistry.register(new InterfereGlyph());
        GlyphRegistry.register(new ResonateGlyph());
        GlyphRegistry.register(new LevitateGlyph());
        GlyphRegistry.register(new ScaleGlyph());
        GlyphRegistry.register(new DomainGlyph());

        // Tier 3
        GlyphRegistry.register(new IgniteGlyph());
        GlyphRegistry.register(new CombustGlyph());
        GlyphRegistry.register(new SmeltGlyph());
        GlyphRegistry.register(new BoltGlyph());
        GlyphRegistry.register(new ArcGlyph());
        GlyphRegistry.register(new FreezeGlyph());
        GlyphRegistry.register(new ShatterGlyph());
        GlyphRegistry.register(new GlaciateGlyph());
        GlyphRegistry.register(new TerraformGlyph());
        GlyphRegistry.register(new EnsnareGlyph());
        GlyphRegistry.register(new PhaseGlyph());
        GlyphRegistry.register(new WarpGlyph());
        GlyphRegistry.register(new SwapGlyph());

        // math glyphs (canReadValue + execute)
        GlyphRegistry.register(new MultiplyGlyph());
        GlyphRegistry.register(new AddGlyph());
        GlyphRegistry.register(new SubtractGlyph());
        GlyphRegistry.register(new DivideGlyph());
        GlyphRegistry.register(new EqualGlyph());
        GlyphRegistry.register(new GreaterGlyph());
        GlyphRegistry.register(new LessGlyph());

        // constructor glyphs (canReadValue + execute)
        GlyphRegistry.register(new PositionValue());
        GlyphRegistry.register(new RotationValue());

        // numeric values
        for (int i = 1; i <= 16; i++) {
            GlyphRegistry.register(new NumberValue(i));
        }
        GlyphRegistry.register(new VariableValue());

        // debug / introspection
        GlyphRegistry.register(new DebugGlyph());

        // output landmark (Wave 2)
        GlyphRegistry.register(new OutputGlyph());

        // caster state queries
        GlyphRegistry.register(new IsHoldingValue());
        GlyphRegistry.register(new ConcentrationGlyph());
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

        ComponentType<EntityStore, HexEffectsComponent> hexConstructType = entityStoreRegistry
                .registerComponent(HexEffectsComponent.class, HexEffectsComponent::new);
        HexEffectsComponent.setComponentType(hexConstructType);
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
        if (controller == null)
            return;
        int effectIndex = EntityEffect.getAssetMap().getIndex(effectId);
        if (effectIndex != Integer.MIN_VALUE) {
            controller.removeEffect(ref, effectIndex, buffer);
        }
    }

    private void RegisterConstructs() {
        ConstructRegistry.register(ScaleGlyph.ID, new ScaleConstructHandler());
        ConstructRegistry.register(ConcentrationGlyph.ID, new ConcentrationConstructHandler());
        ConstructRegistry.register(DomainGlyph.ID, new DomainConstructHandler());
        ConstructRegistry.register(GlaciateGlyph.ID, new GlaciateConstructHandler());
        ConstructRegistry.register(ArcGlyph.ID, new ArcConstructHandler());
        ConstructRegistry.register(PhaseGlyph.ID, new PhaseConstructHandler());
        ConstructRegistry.register(ShatterGlyph.ID, new ShatterConstructHandler());
        ConstructRegistry.register(ConjureGlyph.ID, new ConjureConstructHandler());
        ConstructRegistry.register(ProjectileGlyph.ID, new ProjectileConstructHandler());
        ConstructRegistry.register(EnsnareGlyph.ID, new EnsnareConstructHandler());
        ConstructRegistry.register(FreezeGlyph.ID, new FreezeConstructHandler());
    }
}
