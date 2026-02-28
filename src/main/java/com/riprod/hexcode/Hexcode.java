package com.riprod.hexcode;

import com.riprod.hexcode.builtin.BuiltinPlugin;
import com.riprod.hexcode.builtin.glyphs.effect.propel.PropelComponent;
import com.riprod.hexcode.builtin.glyphs.effect.propel.PropelTickSystem;
import com.riprod.hexcode.command.HexcodeCommand;
import com.riprod.hexcode.core.casting.registery.CastingStyleRegistry;
import com.riprod.hexcode.core.drawing.registry.ShapeAsset;
import com.riprod.hexcode.core.glyphs.component.GlyphComponent;
import com.riprod.hexcode.core.glyphs.component.SlotComponent;
import com.riprod.hexcode.core.glyphs.registry.GlyphAsset;
import com.riprod.hexcode.core.glyphs.variables.BlockVar;
import com.riprod.hexcode.core.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.glyphs.variables.NumberVar;
import com.riprod.hexcode.core.glyphs.variables.PositionVar;
import com.riprod.hexcode.core.glyphs.variables.RotationVar;
import com.riprod.hexcode.core.glyphs.variables.HexVar;
import com.riprod.hexcode.core.hexbook.component.HexBookAsset;
import com.riprod.hexcode.core.hexbook.component.HexBookComponent;
import com.riprod.hexcode.core.hexcaster.component.HexcasterComponent;
import com.riprod.hexcode.core.hexes.component.HexComponent;
import com.riprod.hexcode.core.hexstaff.component.HexStaffAsset;
import com.riprod.hexcode.core.hexstaff.component.HexStaffComponent;
import com.riprod.hexcode.interaction.HexStateChange;
import com.riprod.hexcode.interaction.HexHold;
import com.riprod.hexcode.interaction.HexMode;
import com.riprod.hexcode.interaction.HexModeExit;
import com.riprod.hexcode.state.HexState;
import com.riprod.hexcode.state.HexTick;
import com.riprod.hexcode.state.HexcodeManager;
import com.riprod.hexcode.state.StateRouter;
import com.riprod.hexcode.core.idle.IdleSystem;
import com.riprod.hexcode.core.casting.CastingSystem;
import com.riprod.hexcode.core.casting.component.HexcasterCastingComponent;
import com.riprod.hexcode.core.drawing.DrawingSystem;
import com.riprod.hexcode.core.drawing.component.HexcasterDrawingComponent;
import com.riprod.hexcode.core.crafting.CraftingSystem;
import com.riprod.hexcode.core.debug.DebugComponent;
import com.riprod.hexcode.core.debug.DebugTickSystem;
import com.riprod.hexcode.core.execution.ExecutionSystem;
import com.riprod.hexcode.core.execution.component.RootGlyph;
import com.riprod.hexcode.core.execution.system.ExecutionTickSystem;
import com.riprod.hexcode.interaction.HexStateBranch;
import com.riprod.hexcode.interaction.HexItemCondition;
import com.riprod.hexcode.interaction.PedestalInteraction;
import com.riprod.hexcode.core.crafting.component.HexcasterCraftingComponent;
import com.riprod.hexcode.core.crafting.component.PedestalAnchorComponent;
import com.riprod.hexcode.core.crafting.registry.ObeliskBlockComponent;
import com.riprod.hexcode.core.crafting.registry.PedestalBlockComponent;
import com.riprod.hexcode.core.crafting.system.ObeliskProtectionSystem;
import com.riprod.hexcode.core.crafting.system.PedestalBlockEventSystem;
import com.riprod.hexcode.core.crafting.system.PedestalTickSystem;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.asset.type.particle.config.ParticleSystem;
import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class Hexcode extends JavaPlugin {
  private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
  private static Hexcode instance;

  public Hexcode(JavaPluginInit init) {
    super(init);
    LOGGER.atInfo().log("Hexcode spell-crafting mod v%s initializing...",
        this.getManifest().getVersion().toString());
    instance = this;
  }

  public static Hexcode get() {
    return instance;
  }

  @Override
  protected void setup() {
    // Custom asset registries
    AssetRegistry.register(
        HytaleAssetStore
            .builder(GlyphAsset.class, new DefaultAssetMap<String, GlyphAsset>())
            .setPath("Hexcode/Glyphs")
            .setCodec(GlyphAsset.CODEC)
            .setKeyFunction(GlyphAsset::getId)
            .build());
    AssetRegistry.register(
        HytaleAssetStore
            .builder(ShapeAsset.class, new DefaultAssetMap<String, ShapeAsset>())
            .setPath("Hexcode/Shapes")
            .setCodec(ShapeAsset.CODEC)
            .setKeyFunction(ShapeAsset::getId)
            .build());
    AssetRegistry.register(
        HytaleAssetStore
            .builder(HexBookAsset.class,
                new DefaultAssetMap<String, HexBookAsset>())
            .setPath("Hexcode/HexBooks")
            .setCodec(HexBookAsset.CODEC)
            .setKeyFunction(HexBookAsset::getId)
            .loadsAfter(ParticleSystem.class)
            .loadsAfter(Item.class)
            .build());
    AssetRegistry.register(
        HytaleAssetStore
            .builder(HexStaffAsset.class,
                new DefaultAssetMap<String, HexStaffAsset>())
            .setPath("Hexcode/HexStaffs")
            .setCodec(HexStaffAsset.CODEC)
            .setKeyFunction(HexStaffAsset::getId)
            .loadsAfter(ParticleSystem.class)
            .loadsAfter(Item.class)
            .build());

    // Entity Component Registries
    ComponentRegistryProxy<EntityStore> entityStoreRegistry = this.getEntityStoreRegistry();

    ComponentType<EntityStore, GlyphComponent> glyphComponentType = entityStoreRegistry.registerComponent(GlyphComponent.class, "Glyph",
        GlyphComponent.CODEC);
    GlyphComponent.setComponentType(glyphComponentType);

    ComponentType<EntityStore, HexComponent> hexComponentType = entityStoreRegistry.registerComponent(HexComponent.class, "Hex",
        HexComponent.CODEC);
    HexComponent.setComponentType(hexComponentType);

    ComponentType<EntityStore, HexBookComponent> hexBookComponentType = entityStoreRegistry.registerComponent(HexBookComponent.class, "HexBook",
        HexBookComponent.CODEC);
    HexBookComponent.setComponentType(hexBookComponentType);

    ComponentType<EntityStore, HexStaffComponent> hexStaffComponentType = entityStoreRegistry.registerComponent(HexStaffComponent.class, "HexStaff",
        HexStaffComponent.CODEC);
    HexStaffComponent.setComponentType(hexStaffComponentType);

    ComponentType<EntityStore, HexcasterComponent> hexcasterComponentType = entityStoreRegistry.registerComponent(HexcasterComponent.class,
        HexcasterComponent::new);
    HexcasterComponent.setComponentType(hexcasterComponentType);

    ComponentType<EntityStore, HexcasterCastingComponent> castingRootComponentType = entityStoreRegistry.registerComponent(HexcasterCastingComponent.class,
        HexcasterCastingComponent::new);
    HexcasterCastingComponent.setComponentType(castingRootComponentType);

    ComponentType<EntityStore, HexcasterCraftingComponent> craftingRootComponentType = entityStoreRegistry.registerComponent(HexcasterCraftingComponent.class,
        HexcasterCraftingComponent::new);
    HexcasterCraftingComponent.setComponentType(craftingRootComponentType);

    ComponentType<EntityStore, HexcasterDrawingComponent> drawingRootComponentType = entityStoreRegistry.registerComponent(HexcasterDrawingComponent.class,
        HexcasterDrawingComponent::new);
    HexcasterDrawingComponent.setComponentType(drawingRootComponentType);

    ComponentType<EntityStore, RootGlyph> executionComponentType = entityStoreRegistry.registerComponent(RootGlyph.class,
        RootGlyph::new);
    RootGlyph.setComponentType(executionComponentType);

    ComponentType<EntityStore, PropelComponent> propelComponentType = entityStoreRegistry.registerComponent(PropelComponent.class, PropelComponent::new);
    PropelComponent.setComponentType(propelComponentType);

    ComponentType<EntityStore, SlotComponent> slotComponentType = entityStoreRegistry.registerComponent(SlotComponent.class, SlotComponent::new);
    SlotComponent.setComponentType(slotComponentType);

    ComponentType<EntityStore, PedestalAnchorComponent> pedestalComponentType = entityStoreRegistry.registerComponent(PedestalAnchorComponent.class, PedestalAnchorComponent::new);
    PedestalAnchorComponent.setComponentType(pedestalComponentType);

    ComponentType<EntityStore, DebugComponent> debugComponentType = entityStoreRegistry.registerComponent(DebugComponent.class, DebugComponent::new);
    DebugComponent.setComponentType(debugComponentType);

    // Block Component Registries
    ComponentRegistryProxy<ChunkStore> chunkStoreRegistry = this.getChunkStoreRegistry();

    ComponentType<ChunkStore, PedestalBlockComponent> pedestalBlockComponentType = chunkStoreRegistry.registerComponent(PedestalBlockComponent.class,
        "Hexcode_PedestalBlock",
        PedestalBlockComponent.CODEC);
    PedestalBlockComponent.setComponentType(pedestalBlockComponentType);

    ComponentType<ChunkStore, ObeliskBlockComponent>   obeliskBlockComponentType = chunkStoreRegistry.registerComponent(ObeliskBlockComponent.class,
        "Hexcode_ObeliskBlock",
        ObeliskBlockComponent.CODEC);
    ObeliskBlockComponent.setComponentType(obeliskBlockComponentType);

    // Glyph Var Variables
    HexVar.CODEC.register("Entity", EntityVar.class, EntityVar.CODEC);
    HexVar.CODEC.register("Block", BlockVar.class, BlockVar.CODEC);
    HexVar.CODEC.register("Rotation", RotationVar.class, RotationVar.CODEC);
    HexVar.CODEC.register("Position", PositionVar.class, PositionVar.CODEC);
    HexVar.CODEC.register("Number", NumberVar.class, NumberVar.CODEC);

    // Interaction Registries
    Interaction.CODEC.register("HexStateBranch", HexStateBranch.class, HexStateBranch.CODEC);
    Interaction.CODEC.register("HexStateChange", HexStateChange.class, HexStateChange.CODEC);
    Interaction.CODEC.register("HexHold", HexHold.class, HexHold.CODEC);
    Interaction.CODEC.register("HexMode", HexMode.class, HexMode.CODEC);
    Interaction.CODEC.register("HexModeExit", HexModeExit.class, HexModeExit.CODEC);
    Interaction.CODEC.register("PedestalInteraction", PedestalInteraction.class, PedestalInteraction.CODEC);
    Interaction.CODEC.register("HexItemCondition", HexItemCondition.class, HexItemCondition.CODEC);

    // State Managers
    StateRouter.registerState(HexState.IDLE, new IdleSystem());
    StateRouter.registerState(HexState.CASTING, new CastingSystem());
    // StateRouter.registerState(HexState.CASTING, HexcasterCastingComponent.getComponentType());
    StateRouter.registerState(HexState.DRAWING, new DrawingSystem());
    StateRouter.registerState(HexState.CRAFTING, new CraftingSystem());
    StateRouter.registerState(HexState.EXECUTION, new ExecutionSystem());

    // Ticking Systems
    entityStoreRegistry.registerSystem(new HexTick());
    entityStoreRegistry.registerSystem(new ExecutionTickSystem());
    entityStoreRegistry.registerSystem(new PropelTickSystem());
    entityStoreRegistry.registerSystem(new PedestalTickSystem());
    entityStoreRegistry.registerSystem(new PedestalBlockEventSystem());
    entityStoreRegistry.registerSystem(new ObeliskProtectionSystem());
    entityStoreRegistry.registerSystem(new DebugTickSystem());

    // Events
    this.getEventRegistry().registerGlobal(PlayerConnectEvent.class, Hexcode::onPlayerConnect);
    this.getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, Hexcode::onPlayerDisconnect);

    // Commands
    this.getCommandRegistry().registerCommand(new HexcodeCommand());

    // Startups
    CastingStyleRegistry.init();
    BuiltinPlugin.startup();

    LOGGER.atInfo().log("Hexcode setup complete!");
  }

  private static void onPlayerConnect(PlayerConnectEvent event) {
    Holder<EntityStore> holder = event.getHolder();
    HexcasterComponent comp = holder.ensureAndGetComponent(HexcasterComponent.getComponentType());

    for (HexcodeManager manager : StateRouter.allManagers()) {
      manager.onPlayerJoin(holder, comp);
    }
  }

  private static void onPlayerDisconnect(PlayerDisconnectEvent event) {
    PlayerRef playerRef = event.getPlayerRef();

    for (HexcodeManager manager : StateRouter.allManagers()) {
      manager.onPlayerLeave(playerRef);
    }
  }
}
