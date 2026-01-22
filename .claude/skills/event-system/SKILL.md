# Hytale Event System

## Overview
The Event System provides a publish-subscribe mechanism for responding to game events. Plugins register handlers for specific event types and can cancel or modify events.

## Package Location
- Events: `com.hypixel.hytale.server.core.event.events`
- Registry: `com.hypixel.hytale.server.core.event.EventRegistry`

## Basic Event Handling

```java
@Override
protected void setup() {
    // Register event handler
    getEventRegistry().register(PlayerConnectEvent.class, event -> {
        PlayerRef player = event.getPlayerRef();
        getLogger().info("Player connected: " + player.getUsername());
    });
}
```

## Event Priorities

Events are processed in priority order:

```java
getEventRegistry().register(PlayerConnectEvent.class, EventPriority.HIGHEST, event -> {
    // Runs first
});

getEventRegistry().register(PlayerConnectEvent.class, EventPriority.LOWEST, event -> {
    // Runs last
});
```

Priority order (first to last):
1. `HIGHEST`
2. `HIGH`
3. `NORMAL` (default)
4. `LOW`
5. `LOWEST`

## Cancellable Events

Some events can be cancelled:

```java
getEventRegistry().register(BreakBlockEvent.class, event -> {
    if (shouldPreventBreaking(event)) {
        event.setCancelled(true);
        return;
    }
});
```

## Common Event Categories

### Player Events
| Event | Description |
|-------|-------------|
| `PlayerConnectEvent` | Player connects to server |
| `PlayerDisconnectEvent` | Player disconnects |
| `AddPlayerToWorldEvent` | Player added to world |
| `DrainPlayerFromWorldEvent` | Player removed from world |
| `PlayerDeathEvent` | Player dies |
| `PlayerRespawnEvent` | Player respawns |

### Block Events
| Event | Description |
|-------|-------------|
| `BreakBlockEvent` | Block being broken |
| `PlaceBlockEvent` | Block being placed |
| `UseBlockEvent` | Block being used/interacted |

### Entity Events
| Event | Description |
|-------|-------------|
| `EntityRemoveEvent` | Entity removed from world |
| `KillFeedEvent` | Entity killed |
| `DamageEvent` | Entity taking damage |

### Chat Events
| Event | Description |
|-------|-------------|
| `ChatEvent` | Chat message sent |

### Permission Events
| Event | Description |
|-------|-------------|
| `PlayerPermissionChangeEvent.PermissionsAdded` | Permissions added |
| `PlayerPermissionChangeEvent.PermissionsRemoved` | Permissions removed |
| `PlayerGroupEvent.Added` | Player added to group |
| `PlayerGroupEvent.Removed` | Player removed from group |

## Event Examples

### Player Connect/Disconnect

```java
getEventRegistry().register(PlayerConnectEvent.class, event -> {
    PlayerRef player = event.getPlayerRef();
    String username = player.getUsername();
    UUID uuid = player.getUuid();
    String language = player.getLanguage();

    getLogger().info("Player connected: " + username + " (" + uuid + ")");
});

getEventRegistry().register(PlayerDisconnectEvent.class, event -> {
    PlayerRef player = event.getPlayerRef();
    // Clean up player data
});
```

### Block Break/Place

```java
getEventRegistry().register(BreakBlockEvent.class, event -> {
    BlockType blockType = event.getBlockType();
    Vector3i position = event.getPosition();
    PlayerRef player = event.getPlayerRef();
    World world = event.getWorld();

    // Prevent breaking specific blocks
    if (blockType.getKey().equals("Protected_Block")) {
        event.setCancelled(true);
        player.sendMessage(Message.raw("You cannot break this block!"));
    }
});

getEventRegistry().register(PlaceBlockEvent.class, event -> {
    BlockType blockType = event.getBlockType();
    Vector3i position = event.getPosition();
    // Handle block placement
});
```

### Chat

```java
getEventRegistry().register(ChatEvent.class, event -> {
    PlayerRef sender = event.getPlayerRef();
    String message = event.getMessage();

    // Filter bad words
    if (containsBadWords(message)) {
        event.setCancelled(true);
        sender.sendMessage(Message.raw("Message blocked!"));
        return;
    }

    // Modify message
    event.setMessage("[" + getRank(sender) + "] " + message);
});
```

### Entity Death

```java
getEventRegistry().register(KillFeedEvent.class, event -> {
    // Killed entity info
    Ref<EntityStore> killed = event.getKilledRef();

    // Killer info (if applicable)
    Ref<EntityStore> killer = event.getKillerRef();

    // Death details
    DamageCause cause = event.getDamageCause();
});
```

### Permission Changes

```java
getEventRegistry().register(PlayerPermissionChangeEvent.PermissionsAdded.class, event -> {
    UUID playerUuid = event.getPlayerUUID();
    Set<String> addedPerms = event.getPermissions();

    getLogger().info("Permissions added to " + playerUuid + ": " + addedPerms);
});
```

## Event Properties

### Getting Event Information

```java
getEventRegistry().register(SomeEvent.class, event -> {
    // Common properties vary by event type
    // Check specific event class for available methods

    // For player events:
    PlayerRef player = event.getPlayerRef();

    // For world events:
    World world = event.getWorld();

    // For position events:
    Vector3i position = event.getPosition();

    // For entity events:
    Ref<EntityStore> entityRef = event.getEntityRef();
});
```

## Unregistering Handlers

```java
private EventRegistration chatHandler;

@Override
protected void setup() {
    chatHandler = getEventRegistry().register(ChatEvent.class, this::handleChat);
}

@Override
protected void shutdown() {
    // Unregister specific handler
    getEventRegistry().unregister(chatHandler);
}
```

## Custom Events

While the API primarily uses built-in events, plugins can create custom event flows using the event system patterns.

## Event Threading

- Most events run on the main server thread
- World-specific events may run on world threads
- Check event documentation for threading requirements
- Use `world.runOnThread()` if needed for thread safety

## Best Practices

1. **Check cancellation**: At higher priorities, check if event is already cancelled
2. **Early return**: Exit early if event doesn't apply to your plugin
3. **Avoid heavy processing**: Keep handlers fast to avoid lag
4. **Clean up**: Unregister handlers in `shutdown()` if storing references
5. **Priority selection**: Use NORMAL unless you need specific ordering
6. **Null checks**: Always validate event data before use

```java
getEventRegistry().register(SomeEvent.class, EventPriority.NORMAL, event -> {
    // Check if already cancelled by higher priority handler
    if (event.isCancelled()) {
        return;
    }

    // Validate data
    PlayerRef player = event.getPlayerRef();
    if (player == null || !player.isValid()) {
        return;
    }

    // Process event
    processEvent(event);
});
```

## Documentation Reference
- Event system: `/docs/src/content/docs/core-concepts/event-system.mdx`
- Event catalog: `/docs/src/content/docs/api-reference/events/event-catalog.mdx`
