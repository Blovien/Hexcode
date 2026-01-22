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
            case "cast":
                forceCast(ctx, store, ref, playerRef, world);
                break;
            case "mana":
                setMana(ctx, store, ref, args);
                break;
            case "stamina":
                setStamina(ctx, store, ref, args);
                break;
            case "glyph":
                spawnGlyph(ctx, store, ref, playerRef, world, args);
                break;
            case "loadout":
                setLoadout(ctx, store, ref, playerRef, world, args);
                break;
            default:
                ctx.sendMessage(Message.raw("Unknown subcommand: " + subcommand));
                showHelp(ctx);
        }
    }

    private void showHelp(CommandContext ctx) {
        ctx.sendMessage(Message.raw("Hexcode Commands:"));
        ctx.sendMessage(Message.raw("  /hexcode debug           - Toggle debug visualization"));
        ctx.sendMessage(Message.raw("  /hexcode glyph <id>      - Spawn glyph in crafting space"));
        ctx.sendMessage(Message.raw("  /hexcode loadout <ids>   - Set loadout (comma-separated)"));
        ctx.sendMessage(Message.raw("  /hexcode cast            - Force cast current composition"));
        ctx.sendMessage(Message.raw("  /hexcode clear           - Discard current composition"));
        ctx.sendMessage(Message.raw("  /hexcode mana <amount>   - Set mana for testing"));
        ctx.sendMessage(Message.raw("  /hexcode stamina <amount>- Set stamina for testing"));
        ctx.sendMessage(Message.raw("  /hexcode tree            - Print hex tree structure"));
        ctx.sendMessage(Message.raw("  /hexcode list            - List all glyphs"));
        ctx.sendMessage(Message.raw("  /hexcode test            - Run test hex builds"));
        ctx.sendMessage(Message.raw("  /hexcode mode            - Toggle glyph mode"));
    }

    private static boolean debugEnabled = false;

    private void toggleDebug(CommandContext ctx) {
        debugEnabled = !debugEnabled;
        ctx.sendMessage(Message.raw("Debug visualization: " + (debugEnabled ? "ENABLED" : "DISABLED")));
        if (debugEnabled) {
            ctx.sendMessage(Message.raw("Debug mode shows hex tree structure, mana costs, and target info"));
        }
    }

    public static boolean isDebugEnabled() {
        return debugEnabled;
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
        boolean entered = manager.toggleGlyphMode(playerId, ref, null, null);

        if (entered) {
            ctx.sendMessage(Message.raw("Entered glyph mode"));
        } else {
            ctx.sendMessage(Message.raw("Exited glyph mode"));
        }
    }

    private void forceCast(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef playerRef, World world) {
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

    private void setMana(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, String args) {
        String[] parts = args.split(" ");
        if (parts.length < 2) {
            ctx.sendMessage(Message.raw("Usage: /hexcode mana <amount>"));
            return;
        }

        try {
            float amount = Float.parseFloat(parts[1]);
            EntityStatMap statMap = store.getComponent(ref, EntityStatMap.getComponentType());
            if (statMap != null) {
                int manaIndex = DefaultEntityStatTypes.getMana();
                statMap.setStatValue(manaIndex, amount);
                ctx.sendMessage(Message.raw("Set mana to " + amount));
            } else {
                ctx.sendMessage(Message.raw("Could not find player stats"));
            }
        } catch (NumberFormatException e) {
            ctx.sendMessage(Message.raw("Invalid amount: " + parts[1]));
        }
    }

    private void setStamina(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, String args) {
        String[] parts = args.split(" ");
        if (parts.length < 2) {
            ctx.sendMessage(Message.raw("Usage: /hexcode stamina <amount>"));
            return;
        }

        try {
            float amount = Float.parseFloat(parts[1]);
            EntityStatMap statMap = store.getComponent(ref, EntityStatMap.getComponentType());
            if (statMap != null) {
                int staminaIndex = DefaultEntityStatTypes.getStamina();
                statMap.setStatValue(staminaIndex, amount);
                ctx.sendMessage(Message.raw("Set stamina to " + amount));
            } else {
                ctx.sendMessage(Message.raw("Could not find player stats"));
            }
        } catch (NumberFormatException e) {
            ctx.sendMessage(Message.raw("Invalid amount: " + parts[1]));
        }
    }

    private void spawnGlyph(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef playerRef, World world, String args) {
        String[] parts = args.split(" ");
        if (parts.length < 2) {
            ctx.sendMessage(Message.raw("Usage: /hexcode glyph <glyph_id>"));
            return;
        }

        String glyphId = parts[1];
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
        GlyphModeManager manager = GlyphModeManager.getInstance();
        GlyphMode mode = manager.getSession(playerId);

        if (mode == null) {
            ctx.sendMessage(Message.raw("Enter glyph mode first (/hexcode mode)"));
            return;
        }

        // Add glyph to composition
        boolean added = mode.getComposition().addGlyph(glyph);
        if (added) {
            ctx.sendMessage(Message.raw("Added " + glyph.getDisplayName() + " to composition"));
        } else {
            ctx.sendMessage(Message.raw("Could not add glyph - check composition rules"));
        }
    }

    private void setLoadout(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef playerRef, World world, String args) {
        String[] parts = args.split(" ");
        if (parts.length < 2) {
            ctx.sendMessage(Message.raw("Usage: /hexcode loadout <glyph_id1,glyph_id2,...>"));
            return;
        }

        String glyphIds = parts[1];
        String[] ids = glyphIds.split(",");

        GlyphRegistry registry = GlyphRegistry.getInstance();
        List<Glyph> glyphs = new java.util.ArrayList<>();

        for (String id : ids) {
            String fullId = id.contains(":") ? id : "hexcode:" + id;
            Glyph glyph = registry.getGlyph(fullId);
            if (glyph != null) {
                glyphs.add(glyph);
            } else {
                ctx.sendMessage(Message.raw("Warning: Unknown glyph: " + fullId));
            }
        }

        if (glyphs.isEmpty()) {
            ctx.sendMessage(Message.raw("No valid glyphs specified"));
            return;
        }

        UUID playerId = playerRef.getUuid();
        GlyphModeManager manager = GlyphModeManager.getInstance();
        GlyphMode mode = manager.getSession(playerId);

        if (mode != null) {
            Loadout newLoadout = Loadout.fromGlyphs(glyphs);
            mode.updateLoadout(newLoadout, null);
            ctx.sendMessage(Message.raw("Loadout set with " + glyphs.size() + " glyphs"));
        } else {
            ctx.sendMessage(Message.raw("Enter glyph mode first to set loadout"));
        }
    }
}
