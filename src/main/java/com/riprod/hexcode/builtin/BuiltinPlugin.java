package com.riprod.hexcode.builtin;

import java.util.function.Consumer;

import com.hypixel.hytale.builtin.asseteditor.AssetEditorPlugin;
import com.hypixel.hytale.builtin.asseteditor.event.AssetEditorRequestDataSetEvent;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.builtin.glyphs.isHolding.IsHoldingValue;
import com.riprod.hexcode.builtin.triggers.InteractionTriggerSource;
import com.riprod.hexcode.builtin.triggers.cast.CastTriggerSource;
import com.riprod.hexcode.builtin.triggers.death.DeathTriggerSource;
import com.riprod.hexcode.builtin.triggers.primary.PrimaryImbuementBinder;
import com.riprod.hexcode.builtin.triggers.secondary.SecondaryImbuementBinder;
import com.riprod.hexcode.builtin.triggers.use.UseImbuementBinder;
import com.riprod.hexcode.core.common.triggers.component.TriggerListenerComponent;
import com.riprod.hexcode.core.common.triggers.registry.TriggerListenerRegistry;
import com.riprod.hexcode.core.common.triggers.handler.TriggerConstructHandler;
import com.riprod.hexcode.builtin.glyphs.add.AddGlyph;
import com.riprod.hexcode.builtin.glyphs.cos.CosGlyph;
import com.riprod.hexcode.builtin.glyphs.pi.PiValue;
import com.riprod.hexcode.builtin.glyphs.power.PowerGlyph;
import com.riprod.hexcode.builtin.glyphs.root.RootGlyph;
import com.riprod.hexcode.builtin.glyphs.sin.SinGlyph;
import com.riprod.hexcode.builtin.glyphs.style.StyleGlyph;
import com.riprod.hexcode.builtin.glyphs.tan.TanGlyph;
import com.riprod.hexcode.builtin.glyphs.arc.ArcConstructHandler;
import com.riprod.hexcode.builtin.glyphs.arc.ArcGlyph;
import com.riprod.hexcode.builtin.glyphs.area.AreaGlyph;
import com.riprod.hexcode.builtin.glyphs.beam.BeamGlyph;
import com.riprod.hexcode.builtin.glyphs.bolt.BoltGlyph;
import com.riprod.hexcode.builtin.glyphs.burning.BurningGlyph;
import com.riprod.hexcode.builtin.glyphs.chaos.ChaosGlyph;
import com.riprod.hexcode.builtin.glyphs.concentration.ConcentrationConstructHandler;
import com.riprod.hexcode.builtin.glyphs.concentration.ConcentrationGlyph;
import com.riprod.hexcode.builtin.glyphs.conjure.ConjureGlyph;
import com.riprod.hexcode.builtin.glyphs.conjure.component.ConjureZoneComponent;
import com.riprod.hexcode.builtin.glyphs.conjure.system.ConjureConstructHandler;
import com.riprod.hexcode.builtin.glyphs.debug.DebugGlyph;
import com.riprod.hexcode.builtin.glyphs.delay.DelayConstructHandler;
import com.riprod.hexcode.builtin.glyphs.delay.DelayGlyph;
import com.riprod.hexcode.builtin.glyphs.divide.DivideGlyph;
import com.riprod.hexcode.builtin.glyphs.domain.DomainConstructHandler;
import com.riprod.hexcode.builtin.glyphs.domain.DomainGlyph;
import com.riprod.hexcode.builtin.glyphs.domain.DomainAuraConstructHandler;
import com.riprod.hexcode.builtin.glyphs.domain.component.DomainZoneComponent;
import com.riprod.hexcode.builtin.glyphs.drain.DrainGlyph;
import com.riprod.hexcode.builtin.glyphs.drain.DrainConstructHandler;
import com.riprod.hexcode.builtin.glyphs.drain.DrainState;
import com.riprod.hexcode.builtin.glyphs.ensnare.EnsnareConstructHandler;
import com.riprod.hexcode.builtin.glyphs.ensnare.EnsnareGlyph;
import com.riprod.hexcode.builtin.glyphs.ensnare.component.EnsnareComponent;
import com.riprod.hexcode.builtin.glyphs.equal.EqualGlyph;
import com.riprod.hexcode.builtin.glyphs.erode.ErodeConstructHandler;
import com.riprod.hexcode.builtin.glyphs.erode.ErodeGlyph;
import com.riprod.hexcode.builtin.glyphs.erode.ErodeState;
import com.riprod.hexcode.builtin.glyphs.erode.system.ErodeDamageSystem;
import com.riprod.hexcode.builtin.glyphs.force.ForceGlyph;
import com.riprod.hexcode.builtin.glyphs.fortify.FortifyGlyph;
import com.riprod.hexcode.builtin.glyphs.fortify.FortifyConstructHandler;
import com.riprod.hexcode.builtin.glyphs.fortify.FortifyState;
import com.riprod.hexcode.builtin.glyphs.fortify.system.FortifyDamageSystem;
import com.riprod.hexcode.builtin.glyphs.freeze.FreezeConstructHandler;
import com.riprod.hexcode.builtin.glyphs.freeze.FreezeGlyph;
import com.riprod.hexcode.builtin.glyphs.glaciate.GlaciateConstructHandler;
import com.riprod.hexcode.builtin.glyphs.glaciate.GlaciateGlyph;
import com.riprod.hexcode.builtin.glyphs.glaciate.component.GlaciateComponent;
import com.riprod.hexcode.builtin.glyphs.greater.GreaterGlyph;
import com.riprod.hexcode.builtin.glyphs.growth.GrowthConstructHandler;
import com.riprod.hexcode.builtin.glyphs.growth.GrowthGlyph;
import com.riprod.hexcode.builtin.glyphs.gust.GustGlyph;
import com.riprod.hexcode.builtin.glyphs.halt.HaltGlyph;
import com.riprod.hexcode.builtin.glyphs.halt.HaltConstructHandler;
import com.riprod.hexcode.builtin.glyphs.halt.HaltState;
import com.riprod.hexcode.builtin.glyphs.ignite.IgniteConstructHandler;
import com.riprod.hexcode.builtin.glyphs.ignite.IgniteGlyph;
import com.riprod.hexcode.builtin.glyphs.interfere.InterfereGlyph;
import com.riprod.hexcode.builtin.glyphs.less.LessGlyph;
import com.riprod.hexcode.builtin.glyphs.levitate.LevitateConstructHandler;
import com.riprod.hexcode.builtin.glyphs.levitate.LevitateGlyph;
import com.riprod.hexcode.builtin.glyphs.levitate.LevitateState;
import com.riprod.hexcode.builtin.glyphs.multiply.MultiplyGlyph;
import com.riprod.hexcode.builtin.glyphs.number.NumberValue;
import com.riprod.hexcode.builtin.glyphs.onCast.OnCastGlyph;
import com.riprod.hexcode.builtin.glyphs.onDeath.OnDeathGlyph;
import com.riprod.hexcode.builtin.glyphs.onPrimary.OnPrimaryGlyph;
import com.riprod.hexcode.builtin.glyphs.onSecondary.OnSecondaryGlyph;
import com.riprod.hexcode.builtin.glyphs.onUse.OnUseGlyph;
import com.riprod.hexcode.builtin.glyphs.output.OutputGlyph;
import com.riprod.hexcode.builtin.glyphs.phase.PhaseComponent;
import com.riprod.hexcode.builtin.glyphs.phase.PhaseConstructHandler;
import com.riprod.hexcode.builtin.glyphs.phase.PhaseGlyph;
import com.riprod.hexcode.builtin.glyphs.position.PositionValue;
import com.riprod.hexcode.builtin.glyphs.projectile.ProjectileGlyph;
import com.riprod.hexcode.builtin.glyphs.projectile.component.ProjectileState;
import com.riprod.hexcode.builtin.glyphs.projectile.system.ProjectileConstructHandler;
import com.riprod.hexcode.builtin.glyphs.resonate.ResonateGlyph;
import com.riprod.hexcode.builtin.glyphs.rotation.RotationValue;
import com.riprod.hexcode.builtin.glyphs.scale.ScaleGlyph;
import com.riprod.hexcode.builtin.glyphs.scale.components.ScaleState;
import com.riprod.hexcode.builtin.glyphs.scale.handler.ScaleConstructHandler;
import com.riprod.hexcode.builtin.glyphs.self.SelfGlyph;
import com.riprod.hexcode.builtin.glyphs.shatter.ShatterGlyph;
import com.riprod.hexcode.builtin.glyphs.shatter.component.ShatterState;
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
import com.riprod.hexcode.core.common.construct.component.HexEffectsComponent;
import com.riprod.hexcode.core.common.construct.registry.ConstructRegistry;
import com.riprod.hexcode.core.common.glyphs.registry.GlyphRegistry;
import com.riprod.hexcode.core.common.obelisk.registry.ObeliskHandlerRegistry;
import com.riprod.hexcode.core.state.casting.registery.CastingStyleRegistry;
import com.riprod.hexcode.core.state.crafting.handlers.node.NodeRouter;

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
        GlyphRegistry.register(new BoltGlyph());
        GlyphRegistry.register(new ArcGlyph());
        GlyphRegistry.register(new FreezeGlyph());
        GlyphRegistry.register(new ShatterGlyph());
        GlyphRegistry.register(new GlaciateGlyph());
        GlyphRegistry.register(new TerraformGlyph());
        GlyphRegistry.register(new BurningGlyph());
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
        GlyphRegistry.register(new SinGlyph());
        GlyphRegistry.register(new CosGlyph());
        GlyphRegistry.register(new TanGlyph());
        GlyphRegistry.register(new PowerGlyph());
        GlyphRegistry.register(new RootGlyph());
        GlyphRegistry.register(new StyleGlyph());

        // constructor glyphs (canReadValue + execute)
        GlyphRegistry.register(new PositionValue());
        GlyphRegistry.register(new RotationValue());

        // numeric values
        for (int i = 1; i <= 16; i++) {
            GlyphRegistry.register(new NumberValue(i));
        }
        GlyphRegistry.register(new VariableValue());
        GlyphRegistry.register(new PiValue());

        // debug / introspection
        GlyphRegistry.register(new DebugGlyph());

        // output landmark (Wave 2)
        GlyphRegistry.register(new OutputGlyph());

        // caster state queries
        GlyphRegistry.register(new IsHoldingValue());
        GlyphRegistry.register(new ConcentrationGlyph());

        // trigger glyphs
        GlyphRegistry.register(new OnPrimaryGlyph());
        GlyphRegistry.register(new OnSecondaryGlyph());
        GlyphRegistry.register(new OnUseGlyph());
        GlyphRegistry.register(new OnDeathGlyph());
        GlyphRegistry.register(new OnCastGlyph());
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

        ComponentType<EntityStore, ProjectileState> hexProjectileStateType = entityStoreRegistry
                .registerComponent(ProjectileState.class, ProjectileState::new);
        ProjectileState.setComponentType(hexProjectileStateType);

        // Conjure Zone Component
        ComponentType<EntityStore, ConjureZoneComponent> conjureZoneType = entityStoreRegistry
                .registerComponent(ConjureZoneComponent.class, ConjureZoneComponent::new);
        ConjureZoneComponent.setComponentType(conjureZoneType);

        ComponentType<EntityStore, PhaseComponent> phaseComponentType = entityStoreRegistry
                .registerComponent(PhaseComponent.class, PhaseComponent::new);
        PhaseComponent.setComponentType(phaseComponentType);

        ComponentType<EntityStore, EnsnareComponent> ensnareComponentType = entityStoreRegistry
                .registerComponent(EnsnareComponent.class, EnsnareComponent::new);
        EnsnareComponent.setComponentType(ensnareComponentType);

        ComponentType<EntityStore, GlaciateComponent> glaciateComponentType = entityStoreRegistry
                .registerComponent(GlaciateComponent.class, GlaciateComponent::new);
        GlaciateComponent.setComponentType(glaciateComponentType);

        ComponentType<EntityStore, ShatterState> shatterStateType = entityStoreRegistry
                .registerComponent(ShatterState.class, ShatterState::new);
        ShatterState.setComponentType(shatterStateType);

        ComponentType<EntityStore, DomainZoneComponent> domainZoneComponentType = entityStoreRegistry
                .registerComponent(DomainZoneComponent.class, DomainZoneComponent::new);
        DomainZoneComponent.setComponentType(domainZoneComponentType);

        ComponentType<EntityStore, HexEffectsComponent> hexConstructType = entityStoreRegistry
                .registerComponent(HexEffectsComponent.class, HexEffectsComponent::new);
        HexEffectsComponent.setComponentType(hexConstructType);

        ComponentType<EntityStore, TriggerListenerComponent> triggerListenerType = entityStoreRegistry
                .registerComponent(TriggerListenerComponent.class, TriggerListenerComponent::new);
        TriggerListenerComponent.setComponentType(triggerListenerType);
    }

    private void RegisterSystems() {
        ComponentRegistryProxy<EntityStore> entityStoreRegistry = this.getEntityStoreRegistry();

        entityStoreRegistry.registerSystem(new ErodeDamageSystem());
        entityStoreRegistry.registerSystem(new FortifyDamageSystem());

        // trigger sources
        entityStoreRegistry.registerSystem(new DeathTriggerSource());
        entityStoreRegistry.registerSystem(new CastTriggerSource());
        // interaction source uses a packet adapter (not an EntityEventSystem),
        // so it self-registers via PacketAdapters.registerInbound
        InteractionTriggerSource.register();

        // trigger bootstraps installed on every per-store registry instance
        TriggerListenerRegistry.registerBootstrap(PrimaryImbuementBinder::register);
        TriggerListenerRegistry.registerBootstrap(SecondaryImbuementBinder::register);
        TriggerListenerRegistry.registerBootstrap(UseImbuementBinder::register);
    }

    private void RegisterConstructs() {
        ConstructRegistry.register(ScaleGlyph.ID, new ScaleConstructHandler());
        ConstructRegistry.register(ConcentrationGlyph.ID, new ConcentrationConstructHandler());
        ConstructRegistry.register(DomainGlyph.ID, new DomainConstructHandler());
        ConstructRegistry.register(DomainGlyph.AURA_ID, new DomainAuraConstructHandler());
        ConstructRegistry.register(GlaciateGlyph.ID, new GlaciateConstructHandler());
        ConstructRegistry.register(ArcGlyph.ID, new ArcConstructHandler());
        ConstructRegistry.register(PhaseGlyph.ID, new PhaseConstructHandler());
        ConstructRegistry.register(ConjureGlyph.ID, new ConjureConstructHandler());
        ConstructRegistry.register(ErodeGlyph.ID, new ErodeConstructHandler());
        ConstructRegistry.register(LevitateGlyph.ID, new LevitateConstructHandler());
        ConstructRegistry.register(HaltGlyph.ID, new HaltConstructHandler());
        ConstructRegistry.register(FortifyGlyph.ID, new FortifyConstructHandler());
        ConstructRegistry.register(DrainGlyph.ID, new DrainConstructHandler());
        ConstructRegistry.register(DelayGlyph.ID, new DelayConstructHandler());
        ConstructRegistry.register(EnsnareGlyph.ID, new EnsnareConstructHandler());
        ConstructRegistry.register(FreezeGlyph.ID, new FreezeConstructHandler());
        ConstructRegistry.register(ProjectileGlyph.ID, new ProjectileConstructHandler());
        ConstructRegistry.register(IgniteGlyph.ID, new IgniteConstructHandler());
        ConstructRegistry.register(GrowthGlyph.ID, new GrowthConstructHandler());

        // shared trigger sustain construct (sips volatility while waiting for an event)
        ConstructRegistry.register(TriggerConstructHandler.HANDLER_ID, new TriggerConstructHandler());
    }
}
