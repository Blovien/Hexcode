package com.riprod.hexcode;

import com.riprod.hexcode.builtin.BuiltinPlugin;
import com.riprod.hexcode.command.HexcodeCommand;
import com.riprod.hexcode.core.common.block.component.UnbreakableBlockComponent;
import com.riprod.hexcode.core.common.block.event.BlockBreakEvent;
import com.riprod.hexcode.core.state.drawing.DrawingSlotLockEvent;
import com.riprod.hexcode.core.common.glyphs.component.GlyphComponent;
import com.riprod.hexcode.core.common.glyphs.registry.GlyphAsset;
import com.riprod.hexcode.core.common.glyphs.variables.BlockVar;
import com.riprod.hexcode.core.common.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.common.glyphs.variables.NumberVar;
import com.riprod.hexcode.core.common.glyphs.variables.PositionVar;
import com.riprod.hexcode.core.common.glyphs.variables.RotationVar;
import com.riprod.hexcode.core.common.hexbook.component.HexBookAsset;
import com.riprod.hexcode.core.common.hexbook.component.HexBookComponent;
import com.riprod.hexcode.core.common.hexcaster.component.HexcasterComponent;
import com.riprod.hexcode.core.common.hexes.component.HexComponent;
import com.riprod.hexcode.core.common.hexstaff.component.HexStaffAsset;
import com.riprod.hexcode.core.common.hexstaff.component.HexStaffComponent;
import com.riprod.hexcode.core.common.hidden.component.HiddenComponent;
import com.riprod.hexcode.core.common.hidden.system.HiddenFilterSystem;
import com.riprod.hexcode.core.common.hover.component.HoverableComponent;
import com.riprod.hexcode.core.common.hover.system.HoverableSpatialSystem;
import com.riprod.hexcode.core.common.obelisk.component.ObeliskBlockComponent;
import com.riprod.hexcode.core.common.pedestal.component.PedestalBlockComponent;
import com.riprod.hexcode.core.common.pedestal.events.PedestalBlockEvent;
import com.riprod.hexcode.core.common.pedestal.events.PedestalPlaceEvent;
import com.riprod.hexcode.core.common.utilities.component.DebugComponent;
import com.riprod.hexcode.core.common.effect.GlyphEffectSystem;
import com.riprod.hexcode.core.common.utilities.system.DebugTickSystem;
import com.riprod.hexcode.core.state.casting.CastingSystem;
import com.riprod.hexcode.core.state.casting.component.HexcasterCastingComponent;
import com.riprod.hexcode.core.state.crafting.CraftingSystem;
import com.riprod.hexcode.core.state.crafting.component.HexcasterCraftingComponent;
import com.riprod.hexcode.core.state.crafting.component.NodeComponent;
import com.riprod.hexcode.core.state.crafting.component.SlotComponent;
import com.riprod.hexcode.core.state.drawing.DrawingSystem;
import com.riprod.hexcode.core.state.drawing.component.HexcasterDrawingComponent;
import com.riprod.hexcode.core.state.drawing.registry.ShapeAsset;
import com.riprod.hexcode.core.state.drawing.registry.TemplateAsset;
import com.riprod.hexcode.core.state.execution.ExecutionSystem;
import com.riprod.hexcode.core.state.execution.component.RootGlyph;
import com.riprod.hexcode.core.state.execution.system.ExecutionTickSystem;
import com.riprod.hexcode.core.state.idle.IdleSystem;
import com.riprod.hexcode.interaction.HexStateChange;
import com.riprod.hexcode.interaction.HexHold;
import com.riprod.hexcode.interaction.HexMode;
import com.riprod.hexcode.interaction.HexModeExit;
import com.riprod.hexcode.interaction.HexStateBranch;
import com.riprod.hexcode.interaction.HexAbility;
import com.riprod.hexcode.interaction.HexItemCondition;
import com.riprod.hexcode.interaction.PedestalInteraction;
import com.riprod.hexcode.state.HexState;
import com.riprod.hexcode.state.HexTick;
import com.riprod.hexcode.state.HexcodeManager;
import com.riprod.hexcode.state.StateRouter;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.asset.type.particle.config.ParticleSystem;
import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.ResourceType;
import com.hypixel.hytale.component.spatial.KDTree;
import com.hypixel.hytale.component.spatial.SpatialResource;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class Hexcode extends JavaPlugin {
  private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
  private BuiltinPlugin builtinPlugin;

  public Hexcode(JavaPluginInit init) {
    super(init);
    LOGGER.atInfo().log("Hexcode spell-crafting mod v%s initializing...",
        this.getManifest().getVersion().toString());

    builtinPlugin = new BuiltinPlugin(init);
  }


  @SuppressWarnings("null")
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
            .builder(TemplateAsset.class,
                new DefaultAssetMap<String, TemplateAsset>())
            .setPath("Hexcode/Templates")
            .setCodec(TemplateAsset.CODEC)
            .setKeyFunction(TemplateAsset::getId)
            .loadsAfter(ShapeAsset.class)
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

    ComponentType<EntityStore, GlyphComponent> glyphComponentType = entityStoreRegistry.registerComponent(
        GlyphComponent.class, "Glyph",
        GlyphComponent.CODEC);
    GlyphComponent.setComponentType(glyphComponentType);

    ComponentType<EntityStore, HexComponent> hexComponentType = entityStoreRegistry.registerComponent(
        HexComponent.class, "Hex",
        HexComponent.CODEC);
    HexComponent.setComponentType(hexComponentType);

    ComponentType<EntityStore, HexBookComponent> hexBookComponentType = entityStoreRegistry
        .registerComponent(
            HexBookComponent.class, "HexBook",
            HexBookComponent.CODEC);
    HexBookComponent.setComponentType(hexBookComponentType);

    ComponentType<EntityStore, HexStaffComponent> hexStaffComponentType = entityStoreRegistry
        .registerComponent(
            HexStaffComponent.class, "HexStaff",
            HexStaffComponent.CODEC);
    HexStaffComponent.setComponentType(hexStaffComponentType);

    ComponentType<EntityStore, HexcasterComponent> hexcasterComponentType = entityStoreRegistry
        .registerComponent(
            HexcasterComponent.class, "HexcasterComponent",
            HexcasterComponent.CODEC);
    HexcasterComponent.setComponentType(hexcasterComponentType);

    ComponentType<EntityStore, HexcasterCastingComponent> castingRootComponentType = entityStoreRegistry
        .registerComponent(HexcasterCastingComponent.class,
            HexcasterCastingComponent::new);
    HexcasterCastingComponent.setComponentType(castingRootComponentType);

    ComponentType<EntityStore, HexcasterCraftingComponent> craftingRootComponentType = entityStoreRegistry
        .registerComponent(HexcasterCraftingComponent.class,
            HexcasterCraftingComponent::new);
    HexcasterCraftingComponent.setComponentType(craftingRootComponentType);

    ComponentType<EntityStore, HexcasterDrawingComponent> drawingRootComponentType = entityStoreRegistry
        .registerComponent(HexcasterDrawingComponent.class,
            HexcasterDrawingComponent::new);
    HexcasterDrawingComponent.setComponentType(drawingRootComponentType);

    ComponentType<EntityStore, NodeComponent> nodeComponentType = entityStoreRegistry
        .registerComponent(NodeComponent.class,
            NodeComponent::new);
    NodeComponent.setComponentType(nodeComponentType);

    ComponentType<EntityStore, RootGlyph> executionComponentType = entityStoreRegistry.registerComponent(
        RootGlyph.class,
        RootGlyph::new);
    RootGlyph.setComponentType(executionComponentType);

    ComponentType<EntityStore, SlotComponent> slotComponentType = entityStoreRegistry
        .registerComponent(SlotComponent.class, SlotComponent::new);
    SlotComponent.setComponentType(slotComponentType);

    ComponentType<EntityStore, HoverableComponent> hoverableComponentType = entityStoreRegistry
        .registerComponent(HoverableComponent.class, HoverableComponent::new);
    HoverableComponent.setComponentType(hoverableComponentType);

    ComponentType<EntityStore, DebugComponent> debugComponentType = entityStoreRegistry
        .registerComponent(DebugComponent.class, DebugComponent::new);
    DebugComponent.setComponentType(debugComponentType);

    ComponentType<EntityStore, HiddenComponent> hiddenComponentType = entityStoreRegistry
        .registerComponent(HiddenComponent.class, HiddenComponent::new);
    HiddenComponent.setComponentType(hiddenComponentType);

    // Block Component Registries
    ComponentRegistryProxy<ChunkStore> chunkStoreRegistry = this.getChunkStoreRegistry();

    ComponentType<ChunkStore, PedestalBlockComponent> pedestalBlockComponentType = chunkStoreRegistry
        .registerComponent(PedestalBlockComponent.class,
            "Hexcode_PedestalBlock",
            PedestalBlockComponent.CODEC);
    PedestalBlockComponent.setComponentType(pedestalBlockComponentType);

    ComponentType<ChunkStore, ObeliskBlockComponent> obeliskBlockComponentType = chunkStoreRegistry
        .registerComponent(ObeliskBlockComponent.class,
            "Hexcode_ObeliskBlock",
            ObeliskBlockComponent.CODEC);
    ObeliskBlockComponent.setComponentType(obeliskBlockComponentType);

    ComponentType<ChunkStore, UnbreakableBlockComponent> unbreakableComponentType = chunkStoreRegistry
        .registerComponent(UnbreakableBlockComponent.class, UnbreakableBlockComponent::new);
    UnbreakableBlockComponent.setComponentType(unbreakableComponentType);

    chunkStoreRegistry.registerSystem(new PedestalPlaceEvent());

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
    Interaction.CODEC.register("HexAbility", HexAbility.class, HexAbility.CODEC);

    // State Managers
    StateRouter.registerState(HexState.IDLE, new IdleSystem());
    StateRouter.registerState(HexState.CASTING, new CastingSystem());
    // StateRouter.registerState(HexState.CASTING,
    // HexcasterCastingComponent.getComponentType());
    StateRouter.registerState(HexState.DRAWING, new DrawingSystem());
    StateRouter.registerState(HexState.CRAFTING, new CraftingSystem());
    StateRouter.registerState(HexState.EXECUTION, new ExecutionSystem());

    // Ticking Systems
    entityStoreRegistry.registerSystem(new HexTick());
    entityStoreRegistry.registerSystem(new ExecutionTickSystem());
    entityStoreRegistry.registerSystem(new PedestalBlockEvent());
    entityStoreRegistry.registerSystem(new BlockBreakEvent());
    entityStoreRegistry.registerSystem(new DrawingSlotLockEvent());
    entityStoreRegistry.registerSystem(new DebugTickSystem());
    entityStoreRegistry.registerSystem(new GlyphEffectSystem());

    // Events
    this.getEventRegistry().registerGlobal(PlayerConnectEvent.class, Hexcode::onPlayerConnect);
    this.getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, Hexcode::onPlayerDisconnect);

    // Commands
    this.getCommandRegistry().registerCommand(new HexcodeCommand());

    // spatial resources
    ResourceType<EntityStore, SpatialResource<Ref<EntityStore>, EntityStore>> hoverableSpatialResourceType = entityStoreRegistry
        .registerSpatialResource(() -> new KDTree<>(Ref::isValid));
    entityStoreRegistry.registerSystem(new HoverableSpatialSystem(hoverableSpatialResourceType));

    // Startups
    this.builtinPlugin.startup();

    LOGGER.atInfo().log("Hexcode setup complete!");
  }

  @Override
  protected void start() {
    EntityStore.REGISTRY.registerSystem(new HiddenFilterSystem());
  }

  private static void onPlayerConnect(PlayerConnectEvent event) {
    try {
      Holder<EntityStore> holder = event.getHolder();
      HexcasterComponent comp = holder.ensureAndGetComponent(HexcasterComponent.getComponentType());

      for (HexcodeManager manager : StateRouter.allManagers()) {
        manager.onPlayerJoin(holder, comp);
      }
    } catch (Exception e) {
      LOGGER.atSevere().log("[hexcode] onPlayerConnect failed: %s", e.getMessage());
    }
  }

  private static void onPlayerDisconnect(PlayerDisconnectEvent event) {
    try {
      PlayerRef playerRef = event.getPlayerRef();
      Ref<EntityStore> ref = playerRef.getReference();
      if (ref != null) {
        Store<EntityStore> store = ref.getStore();
        World world = store.getExternalData().getWorld();
        world.execute(() -> {
          try {
            if (ref.isValid()) {
              HexcasterComponent hexcaster = store.getComponent(ref,
                  HexcasterComponent.getComponentType());
              if (hexcaster != null && hexcaster.getState() != HexState.IDLE) {
                hexcaster.requestStateChange(HexState.IDLE);
              }
            }

            for (HexcodeManager manager : StateRouter.allManagers()) {
              manager.onPlayerLeave(playerRef);
            }
          } catch (Exception e) {
            LOGGER.atSevere().log("[hexcode] onPlayerDisconnect world.execute failed: %s", e.getMessage());
          }
        });
      }
    } catch (Exception e) {
      LOGGER.atSevere().log("[hexcode] onPlayerDisconnect failed: %s", e.getMessage());
    }
  }
}
