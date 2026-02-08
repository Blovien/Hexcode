package com.riprod.hexcode;

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
import com.riprod.hexcode.builtin.glyphs.GlyphRegister;
import com.riprod.hexcode.command.HexcodeCommand;
import com.riprod.hexcode.core.casting.CastingStyleRegistry;
import com.riprod.hexcode.core.glyphs.component.GlyphComponent;
import com.riprod.hexcode.core.hexbook.HexBookComponent;
import com.riprod.hexcode.core.hexbook.registry.HexBookAsset;
import com.riprod.hexcode.core.hexstaff.HexStaffComponent;
import com.riprod.hexcode.core.hexstaff.registry.HexStaffAsset;
import com.riprod.hexcode.core.glyphs.registry.GlyphAsset;
import com.riprod.hexcode.interaction.CastingModeEnterInteraction;
import com.riprod.hexcode.interaction.CastingModeExitInteraction;
import com.riprod.hexcode.interaction.GlyphDropInteraction;
import com.riprod.hexcode.interaction.GlyphSelectInteraction;
import com.riprod.hexcode.player.component.HexcasterComponent;

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
                // Custom glyph registery
                AssetRegistry.register(
                                HytaleAssetStore
                                                .builder(GlyphAsset.class, new DefaultAssetMap<String, GlyphAsset>())
                                                .setPath("Hexcode/Glyphs")
                                                .setCodec(GlyphAsset.CODEC)
                                                .setKeyFunction(GlyphAsset::getId)
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

                // Interaction Registries
                Interaction.CODEC.register("CastingModeEnter", CastingModeEnterInteraction.class,
                                CastingModeEnterInteraction.CODEC);
                Interaction.CODEC.register("CastingModeExit", CastingModeExitInteraction.class,
                                CastingModeExitInteraction.CODEC);
                Interaction.CODEC.register("GlyphSelect", GlyphSelectInteraction.class,
                                GlyphSelectInteraction.CODEC);
                Interaction.CODEC.register("GlyphDrop", GlyphDropInteraction.class,
                                GlyphDropInteraction.CODEC);

                // Commands
                this.getCommandRegistry().registerCommand(new HexcodeCommand());

                // Startups
                CastingStyleRegistry.init();
                GlyphRegister.registerAll();

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
