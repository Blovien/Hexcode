package com.riprod.hexcode.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.DefaultArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.plugin.PluginBase;
import com.riprod.hexcode.asset.GlyphAssetDefinition;
import com.riprod.hexcode.asset.GlyphAssetLoader;
import com.riprod.hexcode.data.GlyphInstance;
import com.riprod.hexcode.data.HexBookData;
import com.riprod.hexcode.data.HexBookDataManager;
import com.riprod.hexcode.execution.SpellContext;
import com.riprod.hexcode.glyph.Glyph;
import com.riprod.hexcode.glyph.GlyphRegistry;
import com.riprod.hexcode.glyph.GlyphRole;
import com.riprod.hexcode.hex.Hex;
import com.riprod.hexcode.hex.HexBuilder;
import com.riprod.hexcode.hex.HexInstance;
import com.riprod.hexcode.hex.HexSerializer;
import com.riprod.hexcode.hex.HexValidator;
import com.riprod.hexcode.loadout.Loadout;
import com.riprod.hexcode.mode.GlyphMode;
import com.riprod.hexcode.mode.GlyphModeManager;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Debug and admin commands for Hexcode.
 *
 * Usage:
 * /hexcode help - Show available commands
 * /hexcode debug - Toggle debug visualization
 * /hexcode glyph <id> - Spawn a glyph in crafting space
 * /hexcode loadout <glyphs> - Set loadout (comma-separated glyph IDs)
 * /hexcode cast - Force cast current composition
 * /hexcode clear - Discard current composition
 * /hexcode tree - Print current hex tree structure
 * /hexcode list - List all registered glyphs
 * /hexcode test - Run test hex builds
 */
public class HexcodeCommand extends AbstractPlayerCommand {
    private static boolean debugEnabled = false;

    public HexcodeCommand() {
        super("hexcode", "Hexcode spell-crafting mod commands");
        this.setPermissionGroup(GameMode.Adventure);

        // Register subcommands
        addSubCommand(new HelpSubCommand());
        addSubCommand(new DebugSubCommand());
        addSubCommand(new ListSubCommand());
        addSubCommand(new TreeSubCommand());
        addSubCommand(new ClearSubCommand());
        addSubCommand(new ModeSubCommand());
        addSubCommand(new CastSubCommand());
        addSubCommand(new ManaSubCommand());
        addSubCommand(new StaminaSubCommand());
        addSubCommand(new GlyphSubCommand());
        addSubCommand(new ReloadSubCommand());
        addSubCommand(new ContextSubCommand());
        addSubCommand(new DecaySubCommand());
        addSubCommand(new AssetsSubCommand());
        addSubCommand(new LearnSubCommand());
        addSubCommand(new ForgetSubCommand());
        addSubCommand(new SaveSubCommand());
        addSubCommand(new HexesSubCommand());
        addSubCommand(new DeleteHexSubCommand());
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
            PlayerRef playerRef, World world) {
        // Called when no subcommand provided - show help
        showHelp(ctx);
    }

    private void showHelp(CommandContext ctx) {
        ctx.sendMessage(Message.raw("Hexcode Commands:"));
        ctx.sendMessage(Message.raw("  /hexcode help            - Show this help message"));
        ctx.sendMessage(Message.raw("  /hexcode debug           - Toggle debug visualization"));
        ctx.sendMessage(Message.raw("  /hexcode glyph <id>      - Spawn glyph in crafting space"));
        ctx.sendMessage(Message.raw("  /hexcode cast            - Force cast current composition"));
        ctx.sendMessage(Message.raw("  /hexcode clear           - Discard current composition"));
        ctx.sendMessage(Message.raw("  /hexcode mana <amount>   - Set mana for testing"));
        ctx.sendMessage(Message.raw("  /hexcode stamina <amount>- Set stamina for testing"));
        ctx.sendMessage(Message.raw("  /hexcode tree            - Print hex tree structure"));
        ctx.sendMessage(Message.raw("  /hexcode list            - List all glyphs"));
        ctx.sendMessage(Message.raw("  /hexcode mode            - Toggle glyph mode"));
        ctx.sendMessage(Message.raw("  /hexcode reload          - Reload glyph assets"));
        ctx.sendMessage(Message.raw("  /hexcode context         - Show current spell context"));
        ctx.sendMessage(Message.raw("  /hexcode decay <casts>   - Show decay for N casts"));
        ctx.sendMessage(Message.raw("  /hexcode assets          - List loaded asset definitions"));
        ctx.sendMessage(Message.raw("  /hexcode learn <id>      - Learn a glyph"));
        ctx.sendMessage(Message.raw("  /hexcode forget <id>     - Forget a glyph"));
        ctx.sendMessage(Message.raw("  /hexcode save <name>     - Save current hex to book"));
        ctx.sendMessage(Message.raw("  /hexcode hexes           - List saved hexes in book"));
        ctx.sendMessage(Message.raw("  /hexcode deletehex <name>- Delete saved hex from book"));
    }

    public static boolean isDebugEnabled() {
        return debugEnabled;
    }

    public static void setDebugEnabled(boolean enabled) {
        debugEnabled = enabled;
    }

    // ========================
    // Subcommand Classes
    // ========================

    private class HelpSubCommand extends AbstractPlayerCommand {
        public HelpSubCommand() {
            super("help", "Show available hexcode commands");
        }

        @Override
        protected void execute(@Nonnull CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                PlayerRef playerRef, World world) {
            showHelp(ctx);
        }
    }

    private class DebugSubCommand extends AbstractPlayerCommand {
        public DebugSubCommand() {
            super("debug", "Toggle debug visualization");
        }

        @Override
        protected void execute(@Nonnull CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                PlayerRef playerRef, World world) {
            debugEnabled = !debugEnabled;
            ctx.sendMessage(Message.raw("Debug visualization: " + (debugEnabled ? "ENABLED" : "DISABLED")));
            if (debugEnabled) {
                ctx.sendMessage(Message.raw("Debug mode shows hex tree structure, mana costs, and target info"));
            }
        }
    }

    private class ListSubCommand extends AbstractPlayerCommand {
        public ListSubCommand() {
            super("list", "List all registered glyphs");
        }

        @Override
        protected void execute(@Nonnull CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                PlayerRef playerRef, World world) {
            GlyphRegistry registry = GlyphRegistry.getInstance();

            ctx.sendMessage(Message.raw("=== ALL Glyphs ==="));
            for (Glyph g : registry.getAllGlyphs()) {
                ctx.sendMessage(Message.raw("  " + g.getId() + " - " + g.getDisplayName()));
            }

            ctx.sendMessage(Message.raw("Total: " + registry.getGlyphCount() + " glyphs"));
        }
    }

    private class TreeSubCommand extends AbstractPlayerCommand {
        public TreeSubCommand() {
            super("tree", "Print current hex tree structure");
        }

        @Override
        protected void execute(@Nonnull CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                PlayerRef playerRef, World world) {
            UUID playerId = playerRef.getUuid();
            if (playerId == null) {
                ctx.sendMessage(Message.raw("Could not get player ID"));
                return;
            }

            GlyphModeManager manager = GlyphModeManager.getInstance();
            GlyphMode mode = manager.getSession(playerId);

            if (mode == null || mode.getComposition().isEmpty()) {
                ctx.sendMessage(Message.raw("No hex composition in progress"));
                return;
            }

            Hex hex = mode.getComposition().getHex();
            ctx.sendMessage(Message.raw("Current Hex Tree:"));
            ctx.sendMessage(Message.raw(hex.toTreeString()));
            ctx.sendMessage(Message.raw("Total Nodes: " + hex.getNodeCount()));
        }
    }

    private class ClearSubCommand extends AbstractPlayerCommand {
        public ClearSubCommand() {
            super("clear", "Discard current composition");
        }

        @Override
        protected void execute(@Nonnull CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                PlayerRef playerRef, World world) {
            UUID playerId = playerRef.getUuid();

            if (playerId == null) {
                ctx.sendMessage(Message.raw("Could not get player ID"));
                return;
            }

            GlyphModeManager manager = GlyphModeManager.getInstance();
            GlyphMode mode = manager.getSession(playerId);

            if (mode != null) {
                mode.clearComposition();
                ctx.sendMessage(Message.raw("Composition cleared"));
            } else {
                ctx.sendMessage(Message.raw("Not in glyph mode"));
            }
        }
    }

    private class ModeSubCommand extends AbstractPlayerCommand {
        public ModeSubCommand() {
            super("mode", "Toggle glyph mode");
        }

        @Override
        protected void execute(@Nonnull CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                PlayerRef playerRef, World world) {
            UUID playerId = playerRef.getUuid();

            if (playerId == null || ref == null) {
                ctx.sendMessage(Message.raw("Could not get player info"));
                return;
            }

            GlyphModeManager manager = GlyphModeManager.getInstance();
            boolean entered = manager.toggleGlyphMode(playerId, ref, null, null);

            if (entered) {
                ctx.sendMessage(Message.raw("Entered glyph mode"));
            } else {
                ctx.sendMessage(Message.raw("Exited glyph mode"));
            }
        }
    }

    private class CastSubCommand extends AbstractPlayerCommand {
        public CastSubCommand() {
            super("cast", "Force cast current composition");
        }

        @Override
        protected void execute(@Nonnull CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                PlayerRef playerRef, World world) {
            UUID playerId = playerRef.getUuid();
            if (playerId == null) {
                ctx.sendMessage(Message.raw("Could not get player ID"));
                return;
            }

            GlyphModeManager manager = GlyphModeManager.getInstance();
            GlyphMode mode = manager.getSession(playerId);

            if (mode == null || mode.getComposition().isEmpty()) {
                ctx.sendMessage(Message.raw("No hex composition to cast"));
                return;
            }

            // Force cast ignoring mana cost
            mode.forceCast(store, ref);
            ctx.sendMessage(Message.raw("Force cast executed!"));
        }
    }

    private class ManaSubCommand extends AbstractPlayerCommand {
        private final DefaultArg<Float> amountArg;

        public ManaSubCommand() {
            super("mana", "Set mana for testing");
            this.amountArg = withDefaultArg("amount", "Mana amount to set", ArgTypes.FLOAT, 100f, "Amount of mana");
        }

        @Override
        protected void execute(@Nonnull CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                PlayerRef playerRef, World world) {
            float amount = ctx.get(amountArg);
            EntityStatMap statMap = store.getComponent(ref, EntityStatMap.getComponentType());
            if (statMap != null) {
                int manaIndex = DefaultEntityStatTypes.getMana();
                statMap.setStatValue(manaIndex, amount);
                ctx.sendMessage(Message.raw("Set mana to " + amount));
            } else {
                ctx.sendMessage(Message.raw("Could not find player stats"));
            }
        }
    }

    private class StaminaSubCommand extends AbstractPlayerCommand {
        private final DefaultArg<Float> amountArg;

        public StaminaSubCommand() {
            super("stamina", "Set stamina for testing");
            this.amountArg = withDefaultArg("amount", "Stamina amount to set", ArgTypes.FLOAT, 100f,
                    "Amount of stamina");
        }

        @Override
        protected void execute(@Nonnull CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                PlayerRef playerRef, World world) {
            float amount = ctx.get(amountArg);
            EntityStatMap statMap = store.getComponent(ref, EntityStatMap.getComponentType());
            if (statMap != null) {
                int staminaIndex = DefaultEntityStatTypes.getStamina();
                statMap.setStatValue(staminaIndex, amount);
                ctx.sendMessage(Message.raw("Set stamina to " + amount));
            } else {
                ctx.sendMessage(Message.raw("Could not find player stats"));
            }
        }
    }

    private class GlyphSubCommand extends AbstractPlayerCommand {
        private final DefaultArg<String> glyphIdArg;

        public GlyphSubCommand() {
            super("glyph", "Spawn a glyph in crafting space");
            this.glyphIdArg = withDefaultArg("glyph_id", "The glyph ID to spawn", ArgTypes.STRING, "",
                    "Glyph ID (e.g., fire, hexcode:fire)");
        }

        @Override
        protected void execute(@Nonnull CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                PlayerRef playerRef, World world) {
            String glyphId = ctx.get(glyphIdArg);
            if (glyphId.isEmpty()) {
                ctx.sendMessage(Message.raw("Usage: /hexcode glyph <glyph_id>"));
                return;
            }

            if (!glyphId.contains(":")) {
                glyphId = "hexcode:" + glyphId;
            }

            GlyphRegistry registry = GlyphRegistry.getInstance();
            GlyphInstance glyph = GlyphInstance.initial(registry.getGlyph(glyphId));

            if (glyph == null) {
                ctx.sendMessage(Message.raw("Unknown glyph: " + glyphId));
                return;
            }

            UUID playerId = playerRef.getUuid();
            GlyphModeManager manager = GlyphModeManager.getInstance();
            GlyphMode mode = manager.getSession(playerId);

            if (mode == null) {
                ctx.sendMessage(Message.raw("Enter glyph mode first (/hexcode mode)"));
                return;
            }

            // Add glyph to composition
            boolean added = mode.getComposition().addGlyph(glyph);
            if (added) {
                ctx.sendMessage(Message.raw("Added " + glyph.getGlyph().getDisplayName() + " to composition"));
            } else {
                ctx.sendMessage(Message.raw("Could not add glyph - check composition rules"));
            }
        }
    }

    private class ReloadSubCommand extends AbstractPlayerCommand {
        public ReloadSubCommand() {
            super("reload", "Reload glyph assets");
        }

        @Override
        protected void execute(@Nonnull CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                PlayerRef playerRef, World world) {
            ctx.sendMessage(Message.raw("Reloading glyph assets..."));
            try {
                java.util.Map<String, GlyphAssetDefinition> reloaded = GlyphAssetLoader.reload();
                ctx.sendMessage(Message.raw("Reloaded " + reloaded.size() + " glyph asset definitions"));
            } catch (Exception e) {
                ctx.sendMessage(Message.raw("Error reloading assets: " + e.getMessage()));
            }
        }
    }

    private class ContextSubCommand extends AbstractPlayerCommand {
        public ContextSubCommand() {
            super("context", "Show current spell context");
        }

        @Override
        protected void execute(@Nonnull CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                PlayerRef playerRef, World world) {
            UUID playerId = playerRef.getUuid();
            if (playerId == null) {
                ctx.sendMessage(Message.raw("Could not get player ID"));
                return;
            }

            GlyphModeManager manager = GlyphModeManager.getInstance();
            GlyphMode mode = manager.getSession(playerId);

            if (mode == null) {
                ctx.sendMessage(Message.raw("Not in glyph mode"));
                return;
            }

            ctx.sendMessage(Message.raw("=== Glyph Mode Context ==="));
            ctx.sendMessage(Message.raw("  Active: " + mode.isActive()));
            ctx.sendMessage(Message.raw("  Time in mode: " + String.format("%.1f", mode.getTimeInModeSeconds()) + "s"));
            ctx.sendMessage(Message.raw("  Stamina drain rate: " + mode.getStaminaDrainRate() + "/s"));
            ctx.sendMessage(Message.raw("  Movement speed: " + (mode.getMovementSpeedMultiplier() * 100) + "%"));
            ctx.sendMessage(Message.raw("  Orbital radius: " + mode.getOrbitalRadius()));
            ctx.sendMessage(Message.raw("  Orbit speed: " + mode.getOrbitSpeed()));
            ctx.sendMessage(Message.raw("  Crafting distance: " + mode.getCraftingSpaceDistance()));

            ctx.sendMessage(Message.raw("=== Loadout ==="));
            ctx.sendMessage(Message.raw("  Glyphs: " + mode.getAvailableGlyphs().size()));
            for (Glyph g : mode.getAvailableGlyphs()) {
                ctx.sendMessage(Message.raw("    - " + g.getId()));
            }

            ctx.sendMessage(Message.raw("=== Composition ==="));
            ctx.sendMessage(Message.raw("  Empty: " + mode.getComposition().isEmpty()));
            if (!mode.getComposition().isEmpty()) {
                ctx.sendMessage(Message.raw("  Hex: " + mode.getComposition().getHex().toString()));
            }

            ctx.sendMessage(Message.raw("=== Interaction ==="));
            ctx.sendMessage(Message.raw(
                    "  Hovered glyph: " + (mode.getHoveredGlyph() != null ? mode.getHoveredGlyph().getId() : "none")));
            ctx.sendMessage(Message.raw("  Dragging: " + mode.isDragging()));
        }
    }

    private class DecaySubCommand extends AbstractPlayerCommand {
        private final DefaultArg<Integer> castsArg;

        public DecaySubCommand() {
            super("decay", "Show decay for N casts");
            this.castsArg = withDefaultArg("casts", "Number of casts to show", ArgTypes.INTEGER, 5, "Number of casts");
        }

        @Override
        protected void execute(@Nonnull CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                PlayerRef playerRef, World world) {
            int maxCasts = ctx.get(castsArg);

            ctx.sendMessage(Message.raw("=== Power Decay by Cast Number ==="));
            ctx.sendMessage(Message.raw("Formula: effectivePower = basePower * (1.0 / castNumber)"));
            for (int i = 1; i <= maxCasts; i++) {
                float decay = 1.0f / i;
                int percentage = Math.round(decay * 100);
                ctx.sendMessage(Message.raw("  Cast " + i + ": " + percentage + "% power"));
            }

            ctx.sendMessage(Message.raw("=== Glyph Repetition Decay ==="));
            ctx.sendMessage(Message.raw("Formula: effectivePower = basePower * (1.0 / executionCount)"));
            for (int i = 1; i <= maxCasts; i++) {
                float decay = 1.0f / i;
                int percentage = Math.round(decay * 100);
                ctx.sendMessage(Message.raw("  Execution " + i + ": " + percentage + "% power"));
            }
        }
    }

    private class AssetsSubCommand extends AbstractPlayerCommand {
        public AssetsSubCommand() {
            super("assets", "List loaded asset definitions");
        }

        @Override
        protected void execute(@Nonnull CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                PlayerRef playerRef, World world) {
            java.util.Map<String, GlyphAssetDefinition> assets = GlyphAssetLoader.getLoadedAssets();

            if (assets.isEmpty()) {
                ctx.sendMessage(Message.raw("No glyph assets loaded"));
                return;
            }

            ctx.sendMessage(Message.raw("=== Loaded Glyph Assets (" + assets.size() + ") ==="));
            for (GlyphAssetDefinition asset : assets.values()) {
                String info = String.format("  %s [%s] - power:%.1f cost:%.0f var:%.1f",
                        asset.getId(),
                        asset.getGlyphId(),
                        asset.getBasePower(),
                        asset.getBaseManaCost(),
                        asset.getBaseVariability());
                ctx.sendMessage(Message.raw(info));
            }
        }
    }

    private class LearnSubCommand extends AbstractPlayerCommand {
        private final DefaultArg<String> glyphIdArg;

        public LearnSubCommand() {
            super("learn", "Learn a glyph");
            this.glyphIdArg = withDefaultArg("glyph_id", "The glyph ID to learn", ArgTypes.STRING, "", "Glyph ID");
        }

        @Override
        protected void execute(@Nonnull CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                PlayerRef playerRef, World world) {
            String glyphId = ctx.get(glyphIdArg);
            if (glyphId.isEmpty()) {
                ctx.sendMessage(Message.raw("Usage: /hexcode learn <glyph_id>"));
                return;
            }

            if (!glyphId.contains(":")) {
                glyphId = "hexcode:" + glyphId;
            }

            GlyphRegistry registry = GlyphRegistry.getInstance();
            Glyph glyph = registry.getGlyph(glyphId);

            if (glyph == null) {
                ctx.sendMessage(Message.raw("Unknown glyph: " + glyphId));
                return;
            }

            UUID playerId = playerRef.getUuid();
            if (playerId == null) {
                ctx.sendMessage(Message.raw("Could not get player ID"));
                return;
            }
            ItemStack book = HexBookDataManager.findHeldHexBook(store, ref);

            HexBookData bookData = HexBookDataManager.getData(book);

            if (bookData.hasGlyph(glyphId)) {
                ctx.sendMessage(Message.raw("You already know " + glyph.getDisplayName()));
                return;
            }

            bookData.addGlyphWithDefault(glyphId);
            HexBookDataManager.updateHeldBookData(store, ref, bookData);
            ctx.sendMessage(Message.raw("Added glyph: " + glyph.getDisplayName()));
        }
    }

    private class ForgetSubCommand extends AbstractPlayerCommand {
        private final DefaultArg<String> glyphIdArg;

        public ForgetSubCommand() {
            super("forget", "Forget a glyph");
            this.glyphIdArg = withDefaultArg("glyph_id", "The glyph ID to forget", ArgTypes.STRING, "", "Glyph ID");
        }

        @Override
        protected void execute(@Nonnull CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                PlayerRef playerRef, World world) {
            String glyphId = ctx.get(glyphIdArg);
            if (glyphId.isEmpty()) {
                ctx.sendMessage(Message.raw("Usage: /hexcode forget <glyph_id>"));
                return;
            }

            if (!glyphId.contains(":")) {
                glyphId = "hexcode:" + glyphId;
            }

            GlyphRegistry registry = GlyphRegistry.getInstance();
            Glyph glyph = registry.getGlyph(glyphId);

            if (glyph == null) {
                ctx.sendMessage(Message.raw("Unknown glyph: " + glyphId));
                return;
            }

            UUID playerId = playerRef.getUuid();
            if (playerId == null) {
                ctx.sendMessage(Message.raw("Could not get player ID"));
                return;
            }

            HexBookData playerData = HexBookDataManager.getData(playerId);

            if (!playerData.knowsGlyph(glyphId)) {
                ctx.sendMessage(Message.raw("You don't know " + glyph.getDisplayName()));
                return;
            }

            playerData.forgetGlyph(glyphId);
            PlayerGlyphDataManager.savePlayerDataSync(playerId);
            ctx.sendMessage(Message.raw("Forgot glyph: " + glyph.getDisplayName()));
        }
    }

    private class SaveSubCommand extends AbstractPlayerCommand {
        private final DefaultArg<String> nameArg;

        public SaveSubCommand() {
            super("save", "Save current hex to book");
            this.nameArg = withDefaultArg("name", "Name for the saved hex", ArgTypes.STRING, "", "Hex name");
        }

        @Override
        protected void execute(@Nonnull CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                PlayerRef playerRef, World world) {
            String hexName = ctx.get(nameArg);
            if (hexName.isEmpty()) {
                ctx.sendMessage(Message.raw("Usage: /hexcode save <name>"));
                return;
            }

            UUID playerId = playerRef.getUuid();
            if (playerId == null) {
                ctx.sendMessage(Message.raw("Could not get player ID"));
                return;
            }

            GlyphModeManager manager = GlyphModeManager.getInstance();
            GlyphMode mode = manager.getSession(playerId);

            if (mode == null || mode.getComposition().isEmpty()) {
                ctx.sendMessage(Message.raw("No hex composition to save. Enter glyph mode and compose a hex first."));
                return;
            }

            Hex hex = mode.getComposition().getHex();

            HexBookDataManager.saveHex(store, ref, "hex", hex);

            boolean saved = HexBookDataManager.saveHex(store, ref, hexName, hexString);
            if (saved) {
                ctx.sendMessage(Message.raw("Saved hex '" + hexName + "' to your Hex Book"));
                ctx.sendMessage(Message.raw("  Structure: " + hexString));
            } else {
                // Could be no book, or at max capacity
                HexBookData data = HexBookDataManager.getHeldBookData(store, ref);
                if (data == null) {
                    ctx.sendMessage(Message.raw("You must have a Hex Book in your offhand to save hexes"));
                } else if (data.isAtMaxHexCapacity()) {
                    ctx.sendMessage(Message
                            .raw("Your Hex Book is at maximum capacity (" + HexBookData.MAX_SAVED_HEXES + " hexes)"));
                } else {
                    ctx.sendMessage(Message.raw("Failed to save hex"));
                }
            }
        }
    }

    private class HexesSubCommand extends AbstractPlayerCommand {
        public HexesSubCommand() {
            super("hexes", "List saved hexes in book");
        }

        @Override
        protected void execute(@Nonnull CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                PlayerRef playerRef, World world) {
            HexBookData data = HexBookDataManager.getHeldBookData(store, ref);
            if (data == null) {
                ctx.sendMessage(Message.raw("You must have a Hex Book in your offhand to view saved hexes"));
                return;
            }

            if (data.getSavedHexCount() == 0) {
                ctx.sendMessage(Message.raw("Your Hex Book has no saved hexes"));
                ctx.sendMessage(Message.raw("Use /hexcode save <name> to save a hex"));
                return;
            }

            ctx.sendMessage(Message
                    .raw("=== Saved Hexes (" + data.getSavedHexCount() + "/" + HexBookData.MAX_SAVED_HEXES + ") ==="));
            int index = 1;
            for (HexInstance hex : data.getSavedHexes()) {
                String usageInfo = hex.getTimesUsed() > 0 ? " (used " + hex.getTimesUsed() + "x)" : "";
                ctx.sendMessage(Message.raw("  " + index + ". " + hex.getName() + usageInfo));
                ctx.sendMessage(Message.raw("     " + hex.getHexString()));
                index++;
            }
        }
    }

    private class DeleteHexSubCommand extends AbstractPlayerCommand {
        private final DefaultArg<String> nameArg;

        public DeleteHexSubCommand() {
            super("deletehex", "Delete saved hex from book");
            this.nameArg = withDefaultArg("name", "Name of hex to delete", ArgTypes.STRING, "", "Hex name");
        }

        @Override
        protected void execute(@Nonnull CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                PlayerRef playerRef, World world) {
            String hexName = ctx.get(nameArg);
            if (hexName.isEmpty()) {
                ctx.sendMessage(Message.raw("Usage: /hexcode deletehex <name>"));
                return;
            }

            boolean deleted = HexBookDataManager.deleteSavedHex(store, ref, hexName);
            if (deleted) {
                ctx.sendMessage(Message.raw("Deleted hex '" + hexName + "' from your Hex Book"));
            } else {
                HexBookData data = HexBookDataManager.getHeldBookData(store, ref);
                if (data == null) {
                    ctx.sendMessage(Message.raw("You must have a Hex Book in your offhand"));
                } else if (data.getSavedHex(hexName) == null) {
                    ctx.sendMessage(Message.raw("No saved hex named '" + hexName + "' found"));
                    ctx.sendMessage(Message.raw("Use /hexcode hexes to list saved hexes"));
                } else {
                    ctx.sendMessage(Message.raw("Failed to delete hex"));
                }
            }
        }
    }
}
