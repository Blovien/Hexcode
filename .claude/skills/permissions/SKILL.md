# Hytale Permission System

## Overview
The Permission System controls access to commands and features using a hierarchical node-based structure with support for groups, wildcards, and custom providers.

## Package Locations
- Core: `com.hypixel.hytale.server.core.permissions`
- Providers: `com.hypixel.hytale.server.core.permissions.provider`
- Commands: `com.hypixel.hytale.server.core.permissions.commands`
- Events: `com.hypixel.hytale.server.core.event.events.permissions`

## PermissionHolder Interface

```java
public interface PermissionHolder {
    // Check permission (default: false)
    boolean hasPermission(String permission);

    // Check with custom default
    boolean hasPermission(String permission, boolean defaultValue);
}
```

Implemented by: `Player`, `CommandSender`, `ConsoleSender` (always true)

## PermissionsModule

```java
public class PermissionsModule extends JavaPlugin {
    // Singleton access
    public static PermissionsModule get();

    // Check permissions
    public boolean hasPermission(UUID uuid, String permission);
    public boolean hasPermission(UUID uuid, String permission, boolean defaultValue);

    // User permissions
    public void addUserPermission(UUID uuid, Set<String> permissions);
    public void removeUserPermission(UUID uuid, Set<String> permissions);

    // Group management
    public void addUserToGroup(UUID uuid, String group);
    public void removeUserFromGroup(UUID uuid, String group);
    public Set<String> getGroupsForUser(UUID uuid);

    // Group permissions
    public void addGroupPermission(String group, Set<String> permissions);
    public void removeGroupPermission(String group, Set<String> permissions);

    // Provider management
    public void addProvider(PermissionProvider provider);
    public void removeProvider(PermissionProvider provider);
    public List<PermissionProvider> getProviders();
}
```

## Permission Node Syntax

### Basic Syntax

| Pattern | Meaning |
|---------|---------|
| `permission.node` | Grant specific permission |
| `-permission.node` | Deny specific permission |
| `permission.*` | Grant all under namespace |
| `-permission.*` | Deny all under namespace |
| `*` | Grant all permissions |
| `-*` | Deny all permissions |

### Matching Algorithm

For check `hytale.command.teleport`:

1. Check exact: `hytale.command.teleport`
2. Check parent wildcard: `hytale.command.*`
3. Check grandparent: `hytale.*`
4. Check root: `*`

Negation checked at each level.

### Examples

```
# Grant specific command
hytale.command.teleport

# Deny specific command
-hytale.command.kick

# Grant all commands
hytale.command.*

# Grant all permissions
*

# Grant all but deny specific
*
-hytale.command.ban
```

## Built-in Permission Nodes

| Node | Description |
|------|-------------|
| `hytale.command.*` | All commands |
| `hytale.editor.asset` | Asset editor |
| `hytale.editor.packs.create` | Create packs |
| `hytale.editor.packs.edit` | Edit packs |
| `hytale.editor.packs.delete` | Delete packs |
| `hytale.editor.builderTools` | Builder tools |
| `hytale.editor.brush.*` | Brush editor |
| `hytale.camera.flycam` | Fly camera |

## Default Groups

| Group | Permissions |
|-------|-------------|
| `OP` | `*` (all) |
| `Default` | (none) |

## Permission Checking

### Basic Check

```java
if (sender.hasPermission("myplugin.feature")) {
    // Allowed
}

// With default
if (sender.hasPermission("myplugin.feature", true)) {
    // Allowed if not explicitly denied
}
```

### Using CommandUtil

```java
public void execute(CommandContext context) {
    // Throws NoPermissionException if denied
    CommandUtil.requirePermission(context.getSender(), "myplugin.admin");
    // Continue...
}
```

### In Commands

```java
public class MyCommand extends AbstractCommand {
    public MyCommand() {
        super("mycommand", "Description");
        this.requirePermission("myplugin.command.mycommand");
    }

    @Override
    public boolean hasPermission(CommandSender sender) {
        return sender.hasPermission(getPermission());
    }
}
```

## Command Permissions

### Auto-Generated Format

- Plugin commands: `{plugin.base}.command.{command.name}`
- System commands: `hytale.system.command.{command.name}`
- Sub-commands: `{parent}.{subcommand.name}`

### Argument Permissions

```java
arg.setPermission("myplugin.command.special.option");
```

## Permission Events

| Event | Description |
|-------|-------------|
| `PlayerPermissionChangeEvent.PermissionsAdded` | Permissions added |
| `PlayerPermissionChangeEvent.PermissionsRemoved` | Permissions removed |
| `PlayerGroupEvent.Added` | Player added to group |
| `PlayerGroupEvent.Removed` | Player removed from group |
| `GroupPermissionChangeEvent.Added` | Group permissions added |
| `GroupPermissionChangeEvent.Removed` | Group permissions removed |

### Listening to Changes

```java
@Override
protected void setup() {
    getEventRegistry().register(PlayerPermissionChangeEvent.PermissionsAdded.class,
        event -> {
            UUID player = event.getPlayerUUID();
            Set<String> added = event.getPermissions();
            getLogger().info("Permissions added to " + player + ": " + added);
        }
    );
}
```

## Built-in Commands

| Command | Description |
|---------|-------------|
| `/perm user add <uuid> <permissions...>` | Add user permissions |
| `/perm user remove <uuid> <permissions...>` | Remove user permissions |
| `/perm user group add <uuid> <group>` | Add user to group |
| `/perm user group remove <uuid> <group>` | Remove from group |
| `/perm group add <group> <permissions...>` | Add group permissions |
| `/perm group remove <group> <permissions...>` | Remove group permissions |
| `/perm test <permissions...>` | Test sender permissions |

## Custom Provider

### Provider Interface

```java
public interface PermissionProvider {
    String getName();

    // User permissions
    void addUserPermissions(UUID uuid, Set<String> permissions);
    void removeUserPermissions(UUID uuid, Set<String> permissions);
    Set<String> getUserPermissions(UUID uuid);

    // Group permissions
    void addGroupPermissions(String group, Set<String> permissions);
    void removeGroupPermissions(String group, Set<String> permissions);
    Set<String> getGroupPermissions(String group);

    // Group membership
    void addUserToGroup(UUID uuid, String group);
    void removeUserFromGroup(UUID uuid, String group);
    Set<String> getGroupsForUser(UUID uuid);
}
```

### Implementation Example

```java
public class MyPermissionProvider implements PermissionProvider {
    private final Map<UUID, Set<String>> userPermissions = new HashMap<>();
    private final Map<String, Set<String>> groupPermissions = new HashMap<>();
    private final Map<UUID, Set<String>> userGroups = new HashMap<>();

    @Override
    public String getName() {
        return "MyProvider";
    }

    @Override
    public void addUserPermissions(UUID uuid, Set<String> permissions) {
        userPermissions.computeIfAbsent(uuid, k -> new HashSet<>())
            .addAll(permissions);
    }

    @Override
    public void removeUserPermissions(UUID uuid, Set<String> permissions) {
        Set<String> perms = userPermissions.get(uuid);
        if (perms != null) {
            perms.removeAll(permissions);
        }
    }

    @Override
    public Set<String> getUserPermissions(UUID uuid) {
        return userPermissions.getOrDefault(uuid, Collections.emptySet());
    }

    // ... implement remaining methods ...
}
```

### Registering Provider

```java
@Override
protected void setup() {
    PermissionsModule.get().addProvider(new MyPermissionProvider());
}

@Override
protected void shutdown() {
    PermissionsModule.get().removeProvider(myProvider);
}
```

## Storage

Default provider stores in `permissions.json` in universe folder.

## Best Practices

1. **Use hierarchical nodes**: `myplugin.feature.subfeature`
2. **Document permissions**: List all permissions your plugin uses
3. **Provide defaults**: Use `hasPermission(perm, true)` for non-critical
4. **Use groups**: Assign via groups, not individual users
5. **Test permissions**: Use `/perm test` to verify
6. **Prefix nodes**: Use plugin name as prefix

## Documentation Reference
- Permissions overview: `/docs/src/content/docs/api-reference/permissions/overview.mdx`
- Commands: `/docs/src/content/docs/core-concepts/commands.mdx`
