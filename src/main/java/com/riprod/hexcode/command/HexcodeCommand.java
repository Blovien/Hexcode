package com.riprod.hexcode.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.DefaultArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.plugin.PluginBase;
import com.riprod.hexcode.glyph.Glyph;
import com.riprod.hexcode.glyph.GlyphRegistry;
import com.riprod.hexcode.glyph.GlyphRole;
import com.riprod.hexcode.hex.Hex;
import com.riprod.hexcode.hex.HexBuilder;
import com.riprod.hexcode.hex.HexValidator;
import com.riprod.hexcode.loadout.Loadout;
import com.riprod.hexcode.mode.GlyphMode;
import com.riprod.hexcode.mode.GlyphModeManager;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.UUID;

/**
 * Debug and admin commands for Hexcode.
 *
 * Usage:
 * /hexcode debug              - Toggle debug visualization
 * /hexcode glyph <id>         - Spawn a glyph in crafting space
 * /hexcode loadout <glyphs>   - Set loadout (comma-separated glyph IDs)
 * /hexcode cast               - Force cast current composition
 * /hexcode clear              - Discard current composition
 * /hexcode tree               - Print current hex tree structure
 * /hexcode list               - List all registered glyphs
 * /hexcode test               - Run test hex builds
 */
public class HexcodeCommand extends AbstractPlayerCommand {
    private final DefaultArg<String> subCommand;

    public HexcodeCommand() {
        super("hexcode", "Hexcode spell-crafting mod commands");
        this.setPermissionGroup(GameMode.Adventure);

        this.subCommand = withDefaultArg("command", "The hexcode command. Send nothing or help for options", ArgTypes.STRING, "help", "Lists valid commands");   
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef playerRef, World world) {
        String args = ctx.get(this.subCommand);

        if (args.toLowerCase().equals("help")) {
            showHelp(ctx);
            return;
        }

        String subcommand = args.toLowerCase();

        switch (subcommand) {
            case "debug":
                toggleDebug(ctx);
                break;
            case "list":
                listGlyphs(ctx);
                break;
            case "tree":
                showTree(ctx, store, ref, playerRef, world);
                break;
            case "clear":
                clearComposition(ctx, store, ref, playerRef, world);
                break;
            case "test":
                runTests(ctx);
                break;
            case "mode":
                toggleMode(ctx, store, ref, playerRef, world);
                break;
            default:
                ctx.sendMessage(Message.raw("Unknown subcommand: " + subcommand));
                showHelp(ctx);
        }
    }

    private void showHelp(CommandContext ctx) {
        ctx.sendMessage(Message.raw("Hexcode Commands:"));
        ctx.sendMessage(Message.raw("  /hexcode debug    - Toggle debug mode"));
        ctx.sendMessage(Message.raw("  /hexcode list     - List all glyphs"));
        ctx.sendMessage(Message.raw("  /hexcode tree     - Show current hex tree"));
        ctx.sendMessage(Message.raw("  /hexcode clear    - Clear composition"));
        ctx.sendMessage(Message.raw("  /hexcode test     - Run test hex builds"));
        ctx.sendMessage(Message.raw("  /hexcode mode     - Toggle glyph mode"));
    }

    private void toggleDebug(CommandContext ctx) {
        // TODO: Implement debug visualization toggle
        ctx.sendMessage(Message.raw("Debug mode toggled (not yet implemented)"));
    }

    private void listGlyphs(CommandContext ctx) {
        GlyphRegistry registry = GlyphRegistry.getInstance();

        ctx.sendMessage(Message.raw("=== EFFECT Glyphs ==="));
        for (Glyph g : registry.getEffectGlyphs()) {
            ctx.sendMessage(Message.raw("  " + g.getId() + " - " + g.getDisplayName() + " (cost: " + g.getBaseCost() + ")"));
        }

        ctx.sendMessage(Message.raw("=== MODIFIER Glyphs ==="));
        for (Glyph g : registry.getModifierGlyphs()) {
            ctx.sendMessage(Message.raw("  " + g.getId() + " - " + g.getDisplayName() + " (x" + g.getModifierMultiplier() + ")"));
        }

        ctx.sendMessage(Message.raw("=== SELECT Glyphs ==="));
        for (Glyph g : registry.getSelectGlyphs()) {
            String delayed = g.isDelayed() ? " [delayed]" : "";
            ctx.sendMessage(Message.raw("  " + g.getId() + " - " + g.getDisplayName() + delayed));
        }

        ctx.sendMessage(Message.raw("Total: " + registry.getGlyphCount() + " glyphs"));
    }

    private void showTree(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef playerRef, World world) {
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
        ctx.sendMessage(Message.raw("Base cost: " + hex.getBaseCost()));
    }

    private void clearComposition(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef playerRef, World world) {
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

    private void runTests(CommandContext ctx) {
        ctx.sendMessage(Message.raw("Running hex build tests..."));

        HexValidator validator = new HexValidator();

        // Test 1: Simple self heal
        Hex hex1 = HexBuilder.exampleSelfHeal();
        HexValidator.ValidationResult result1 = validator.validate(hex1);
        ctx.sendMessage(Message.raw("Test 1 - SELF[HEAL[]]: " + result1));
        ctx.sendMessage(Message.raw("  Structure: " + hex1.toString()));

        // Test 2: Beam with power and ice
        Hex hex2 = HexBuilder.exampleBeamFireIce();
        HexValidator.ValidationResult result2 = validator.validate(hex2);
        ctx.sendMessage(Message.raw("Test 2 - BEAM[POWER[FIRE[]], ICE[]]: " + result2));
        ctx.sendMessage(Message.raw("  Structure: " + hex2.toString()));
        ctx.sendMessage(Message.raw("  Base cost: " + hex2.getBaseCost()));

        // Test 3: Burst lightning
        Hex hex3 = HexBuilder.exampleBurstLightning();
        HexValidator.ValidationResult result3 = validator.validate(hex3);
        ctx.sendMessage(Message.raw("Test 3 - BURST[POWER[LIGHTNING[]]]: " + result3));

        // Test 4: Projectile with duration fire
        Hex hex4 = HexBuilder.exampleProjectileFireEarth();
        HexValidator.ValidationResult result4 = validator.validate(hex4);
        ctx.sendMessage(Message.raw("Test 4 - PROJECTILE[DURATION[FIRE[]], EARTH[]]: " + result4));

        ctx.sendMessage(Message.raw("Tests complete!"));
    }

    private void toggleMode(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef playerRef, World world) {
        UUID playerId = playerRef.getUuid();


        if (playerId == null || ref == null) {
            ctx.sendMessage(Message.raw("Could not get player info"));
            return;
        }

        GlyphModeManager manager = GlyphModeManager.getInstance();
        boolean entered = manager.toggleGlyphMode(playerId, ref, null);

        if (entered) {
            ctx.sendMessage(Message.raw("Entered glyph mode"));
        } else {
            ctx.sendMessage(Message.raw("Exited glyph mode"));
        }
    }
}
