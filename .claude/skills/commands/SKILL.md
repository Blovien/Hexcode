# Hytale Command System

## Overview
The Command System allows plugins to create server commands with arguments, permissions, tab completion, and subcommands.

## Package Location
- Commands: `com.hypixel.hytale.server.core.command`
- System: `com.hypixel.hytale.server.core.command.system`
- Arguments: `com.hypixel.hytale.server.core.command.arguments`

## Basic Command

```java
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;

public class HelloCommand extends AbstractCommand {
    public HelloCommand() {
        super("hello", "Says hello to a player");
    }

    @Override
    public void execute(CommandContext context) {
        CommandSender sender = context.getSender();
        sender.sendMessage(Message.raw("Hello, World!"));
    }
}
```

### Registering Commands

```java
@Override
protected void setup() {
    getCommandRegistry().register(new HelloCommand());
}
```

## Command with Arguments

```java
import com.hypixel.hytale.server.core.command.arguments.*;

public class TeleportCommand extends AbstractCommand {
    public TeleportCommand() {
        super("teleport", "Teleport to coordinates");

        // Add arguments
        addArgument(new CoordinateArgument("x"));
        addArgument(new CoordinateArgument("y"));
        addArgument(new CoordinateArgument("z"));
    }

    @Override
    public void execute(CommandContext context) {
        double x = context.getArgument("x", Double.class);
        double y = context.getArgument("y", Double.class);
        double z = context.getArgument("z", Double.class);

        PlayerRef player = context.getPlayerRef();
        // Teleport player to x, y, z
    }
}
```

## Argument Types

| Type | Class | Description |
|------|-------|-------------|
| String | `StringArgument` | Text value |
| Integer | `IntegerArgument` | Whole number |
| Float | `FloatArgument` | Decimal number |
| Boolean | `BooleanArgument` | true/false |
| Player | `PlayerArgument` | Online player |
| Coordinate | `CoordinateArgument` | World coordinate |
| Enum | `EnumArgument` | Enum value |
| BlockType | `BlockTypeArgument` | Block type |
| Item | `ItemArgument` | Item type |

### Argument Configuration

```java
// Required argument
addArgument(new StringArgument("name"));

// Optional argument with default
StringArgument arg = new StringArgument("name");
arg.setOptional(true);
arg.setDefaultValue("default");
addArgument(arg);

// Argument with suggestions
PlayerArgument playerArg = new PlayerArgument("target");
// Tab completion automatically shows online players
addArgument(playerArg);

// Argument with permission
StringArgument adminArg = new StringArgument("secret");
adminArg.setPermission("myplugin.admin.secret");
addArgument(adminArg);
```

## Subcommands

```java
public class MainCommand extends AbstractCommand {
    public MainCommand() {
        super("mycommand", "Main plugin command");

        // Add subcommands
        addSubCommand(new ReloadSubCommand());
        addSubCommand(new HelpSubCommand());
        addSubCommand(new SetSubCommand());
    }

    @Override
    public void execute(CommandContext context) {
        // Called when no subcommand matched
        context.getSender().sendMessage(Message.raw("Use /mycommand help"));
    }
}

public class ReloadSubCommand extends AbstractCommand {
    public ReloadSubCommand() {
        super("reload", "Reload plugin configuration");
    }

    @Override
    public void execute(CommandContext context) {
        // Reload config
        context.getSender().sendMessage(Message.raw("Config reloaded!"));
    }
}
```

Usage: `/mycommand reload`

## Command Context

```java
@Override
public void execute(CommandContext context) {
    // Get sender
    CommandSender sender = context.getSender();

    // Check if sender is player
    if (sender.isPlayer()) {
        PlayerRef player = context.getPlayerRef();
        // Player-specific logic
    }

    // Get arguments
    String name = context.getArgument("name", String.class);
    int count = context.getArgument("count", Integer.class);

    // Optional argument (may be null)
    String optional = context.getArgumentOrNull("optional", String.class);

    // Check permissions
    if (!sender.hasPermission("myplugin.admin")) {
        sender.sendMessage(Message.raw("No permission!"));
        return;
    }

    // Send response
    sender.sendMessage(Message.raw("Command executed!"));
}
```

## CommandSender

```java
CommandSender sender = context.getSender();

// Check type
boolean isPlayer = sender.isPlayer();
boolean isConsole = sender.isConsole();

// Get name
String name = sender.getName();

// Send messages
sender.sendMessage(Message.raw("Plain text"));
sender.sendMessage(Message.text("Formatted text"));

// Check permissions
boolean hasPerm = sender.hasPermission("permission.node");
boolean hasPermDefault = sender.hasPermission("perm", true); // default if not set
```

## Permissions

### Auto-Generated Permissions

Commands automatically generate permission nodes:
- `{plugin.base}.command.{command.name}`
- `{plugin.base}.command.{command.name}.{subcommand}`

### Manual Permission

```java
public class AdminCommand extends AbstractCommand {
    public AdminCommand() {
        super("admin", "Admin command");
        setPermission("myplugin.command.admin");
    }

    @Override
    public boolean hasPermission(CommandSender sender) {
        return sender.hasPermission(getPermission());
    }
}
```

### Require Permission in Execute

```java
@Override
public void execute(CommandContext context) {
    CommandUtil.requirePermission(context.getSender(), "myplugin.admin");
    // Throws NoPermissionException if denied

    // Continue with command...
}
```

## Tab Completion

Custom tab completion:

```java
public class CustomCommand extends AbstractCommand {
    public CustomCommand() {
        super("custom", "Custom command");

        StringArgument modeArg = new StringArgument("mode");
        modeArg.setSuggestions(() -> Arrays.asList("easy", "medium", "hard"));
        addArgument(modeArg);
    }
}
```

### Dynamic Suggestions

```java
StringArgument worldArg = new StringArgument("world");
worldArg.setSuggestions(() -> {
    return Universe.get().getWorlds().stream()
        .map(World::getName)
        .collect(Collectors.toList());
});
addArgument(worldArg);
```

## Error Handling

```java
@Override
public void execute(CommandContext context) {
    try {
        PlayerRef target = context.getArgument("target", PlayerRef.class);
        if (target == null) {
            throw new CommandException("Player not found!");
        }
        // Process command
    } catch (CommandException e) {
        context.getSender().sendMessage(Message.raw("Error: " + e.getMessage()));
    }
}
```

## Command Aliases

```java
public class TpCommand extends AbstractCommand {
    public TpCommand() {
        super("teleport", "Teleport command");
        setAliases("tp", "goto", "warp");
    }
}
```

## Player-Only Commands

```java
public class PlayerOnlyCommand extends AbstractCommand {
    public PlayerOnlyCommand() {
        super("fly", "Toggle flight mode");
        setPlayerOnly(true); // Only players can use
    }

    @Override
    public void execute(CommandContext context) {
        PlayerRef player = context.getPlayerRef(); // Guaranteed non-null
        // Toggle flight
    }
}
```

## Console Commands

```java
@Override
public void execute(CommandContext context) {
    if (context.getSender().isConsole()) {
        // Console-specific handling
        getLogger().info("Executed from console");
    }
}
```

## Best Practices

1. **Clear descriptions**: Provide helpful command descriptions
2. **Permission checks**: Always validate permissions
3. **Input validation**: Validate arguments before processing
4. **Error messages**: Give clear feedback on errors
5. **Tab completion**: Provide helpful suggestions
6. **Subcommands**: Organize complex commands with subcommands
7. **Help text**: Include usage information

```java
public class WellDesignedCommand extends AbstractCommand {
    public WellDesignedCommand() {
        super("mycommand", "Does something useful");
        setPermission("myplugin.command.mycommand");
        setUsage("/mycommand <player> [amount]");

        addArgument(new PlayerArgument("player"));

        IntegerArgument amountArg = new IntegerArgument("amount");
        amountArg.setOptional(true);
        amountArg.setDefaultValue(1);
        amountArg.setMin(1);
        amountArg.setMax(100);
        addArgument(amountArg);
    }

    @Override
    public void execute(CommandContext context) {
        // Implementation
    }
}
```

## Documentation Reference
- Commands: `/docs/src/content/docs/core-concepts/commands.mdx`
- Permissions: `/docs/src/content/docs/api-reference/permissions/overview.mdx`
