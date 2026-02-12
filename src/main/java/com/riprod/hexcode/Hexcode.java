package com.riprod.hexcode;

import com.riprod.hexcode.builtin.BuiltinPlugin;
import com.riprod.hexcode.command.HexcodeCommand;
import com.riprod.hexcode.core.casting.registery.CastingStyleRegistry;
import com.riprod.hexcode.core.drawing.registry.ShapeAsset;
import com.riprod.hexcode.core.glyphs.component.GlyphComponent;
import com.riprod.hexcode.core.glyphs.registry.GlyphAsset;
import com.riprod.hexcode.core.glyphs.registry.OperatorAsset;
import com.riprod.hexcode.core.glyphs.variables.BlockVar;
import com.riprod.hexcode.core.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.glyphs.variables.PositionVar;
import com.riprod.hexcode.core.glyphs.variables.SpellVar;
import com.riprod.hexcode.core.hexbook.component.HexBookComponent;
import com.riprod.hexcode.core.hexbook.registry.HexBookAsset;
import com.riprod.hexcode.core.hexstaff.component.HexStaffComponent;
import com.riprod.hexcode.core.hexstaff.registry.HexStaffAsset;
import com.riprod.hexcode.interaction.StaffSecondaryEnter;
import com.riprod.hexcode.interaction.StaffSecondaryExit;
import com.riprod.hexcode.interaction.StaffPrimaryExit;
import com.riprod.hexcode.interaction.StaffPrimaryEnter;
import com.riprod.hexcode.player.component.HexcasterComponent;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
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

  private ComponentType<EntityStore, HexBookComponent> hexBookComponentType;
  private ComponentType<EntityStore, GlyphComponent> glyphComponentType;
  private ComponentType<EntityStore, HexStaffComponent> hexStaffComponentType;
  private ComponentType<EntityStore, HexcasterComponent> hexcasterComponentType;

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
            .builder(OperatorAsset.class,
                new DefaultAssetMap<String, OperatorAsset>())
            .setPath("Hexcode/Operators")
            .setCodec(OperatorAsset.CODEC)
            .setKeyFunction(OperatorAsset::getId)
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
            .build());
    AssetRegistry.register(
        HytaleAssetStore
            .builder(HexStaffAsset.class,
                new DefaultAssetMap<String, HexStaffAsset>())
            .setPath("Hexcode/HexStaffs")
            .setCodec(HexStaffAsset.CODEC)
            .setKeyFunction(HexStaffAsset::getId)
            .build());

    // Entity Component Registries
    ComponentRegistryProxy<EntityStore> entityStoreRegistry = this.getEntityStoreRegistry();

    this.glyphComponentType = entityStoreRegistry.registerComponent(GlyphComponent.class, "Glyph",
        GlyphComponent.CODEC);
    GlyphComponent.setComponentType(glyphComponentType);

    this.hexBookComponentType = entityStoreRegistry.registerComponent(HexBookComponent.class, "HexBook",
        HexBookComponent.CODEC);
    HexBookComponent.setComponentType(hexBookComponentType);

    this.hexStaffComponentType = entityStoreRegistry.registerComponent(HexStaffComponent.class, "HexStaff",
        HexStaffComponent.CODEC);
    HexStaffComponent.setComponentType(hexStaffComponentType);

    this.hexcasterComponentType = entityStoreRegistry.registerComponent(HexcasterComponent.class,
        HexcasterComponent::new);
    HexcasterComponent.setComponentType(hexcasterComponentType);

    // Glyph Var Variables
    SpellVar.CODEC.register("Entity", EntityVar.class, EntityVar.CODEC);
    SpellVar.CODEC.register("Block", BlockVar.class, BlockVar.CODEC);
    SpellVar.CODEC.register("Position", PositionVar.class, PositionVar.CODEC);

    // Interaction Registries
    Interaction.CODEC.register("CastingModeEnter", StaffSecondaryEnter.class,
        StaffSecondaryEnter.CODEC);
    Interaction.CODEC.register("CastingModeExit", StaffSecondaryExit.class,
        StaffSecondaryExit.CODEC);
    Interaction.CODEC.register("GlyphSelect", StaffPrimaryEnter.class,
        StaffPrimaryEnter.CODEC);
    Interaction.CODEC.register("GlyphDrop", StaffPrimaryExit.class,
        StaffPrimaryExit.CODEC);

    // Commands
    this.getCommandRegistry().registerCommand(new HexcodeCommand());

    // Startups
    CastingStyleRegistry.init();
    BuiltinPlugin.startup();

    LOGGER.atInfo().log("Hexcode setup complete!");
  }

  public ComponentType<EntityStore, HexBookComponent> getHexBookComponentType() {
    return this.hexBookComponentType;
  }

  public ComponentType<EntityStore, GlyphComponent> getGlyphComponentType() {
    return this.glyphComponentType;
  }

  public ComponentType<EntityStore, HexStaffComponent> getHexStaffComponentType() {
    return this.hexStaffComponentType;
  }

  public ComponentType<EntityStore, HexcasterComponent> getHexcasterComponentType() {
    return this.hexcasterComponentType;
  }
}
