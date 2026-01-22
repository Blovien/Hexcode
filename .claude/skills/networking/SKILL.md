# Hytale Networking System

## Overview
The Networking System handles all client-server communication through a packet-based protocol built on Netty. Plugins can send packets, watch packet flow, and filter packets.

## Package Locations
- Protocol: `com.hypixel.hytale.protocol`
- Packets: `com.hypixel.hytale.protocol.packets`
- Server I/O: `com.hypixel.hytale.server.core.io`
- Packet Adapters: `com.hypixel.hytale.server.core.io.adapter`

## Packet Interface

```java
public interface Packet {
    int getId();
    void serialize(@Nonnull ByteBuf buf);
    int computeSize();
}
```

### Packet Structure

```java
public class ExamplePacket implements Packet {
    public static final int PACKET_ID = 123;
    public static final boolean IS_COMPRESSED = false;
    public static final int MAX_SIZE = 256;

    @Override
    public int getId() {
        return PACKET_ID;
    }

    @Override
    public void serialize(@Nonnull ByteBuf buf) {
        // Write fields to buffer
    }

    public static ExamplePacket deserialize(@Nonnull ByteBuf buf, int offset) {
        // Read fields from buffer
    }

    @Override
    public int computeSize() {
        return MAX_SIZE;
    }
}
```

## Packet Categories

~354 packet types across 18 categories:

| Category | Package | Description |
|----------|---------|-------------|
| `connection` | `packets.connection` | Connect, Disconnect, Ping |
| `auth` | `packets.auth` | Authentication |
| `player` | `packets.player` | Movement, inventory |
| `entities` | `packets.entities` | Spawn, despawn, animation |
| `world` | `packets.world` | Chunks, blocks, sounds |
| `interface_` | `packets.interface_` | Chat, notifications, UI |
| `inventory` | `packets.inventory` | Item operations |
| `assets` | `packets.assets` | Asset updates |
| `interaction` | `packets.interaction` | Mount, triggers |
| `setup` | `packets.setup` | Initial setup |
| `window` | `packets.window` | Dialogs, windows |
| `camera` | `packets.camera` | Camera effects |
| `buildertools` | `packets.buildertools` | World editing |
| `machinima` | `packets.machinima` | Cinematics |

## PacketHandler

```java
public abstract class PacketHandler implements IPacketReceiver {
    // Send single packet
    public void write(@Nonnull Packet packet);

    // Send packet bypassing cache
    public void writeNoCache(@Nonnull Packet packet);

    // Send multiple packets
    public void write(@Nonnull Packet... packets);

    // Ping management
    public void sendPing();
    public void handlePong(@Nonnull Pong packet);

    // Lifecycle hooks
    public void registered(@Nullable PacketHandler oldHandler);
    public void unregistered(@Nullable PacketHandler newHandler);

    // Handle incoming packet
    public abstract void accept(@Nonnull Packet packet);
}
```

### Handler Types

| Handler | Description |
|---------|-------------|
| `GenericPacketHandler` | Generic connection |
| `GenericConnectionPacketHandler` | Connection-specific |
| `InitialPacketHandler` | Initial connection |
| `GamePacketHandler` | In-game player |
| `SubPacketHandler` | Custom sub-handlers |

## Packet Adapters

Intercept packets for watching or filtering:

```java
public final class PacketAdapters {
    // Inbound packets (client → server)
    public static PacketFilter registerInbound(@Nonnull PacketWatcher watcher);
    public static PacketFilter registerInbound(@Nonnull PlayerPacketWatcher watcher);
    public static PacketFilter registerInbound(@Nonnull PlayerPacketFilter filter);

    // Outbound packets (server → client)
    public static PacketFilter registerOutbound(@Nonnull PacketWatcher watcher);
    public static PacketFilter registerOutbound(@Nonnull PlayerPacketWatcher watcher);
    public static PacketFilter registerOutbound(@Nonnull PlayerPacketFilter filter);

    // Deregister
    public static void deregisterInbound(PacketFilter predicate);
    public static void deregisterOutbound(PacketFilter predicate);
}
```

### Adapter Interfaces

```java
// Watch any packet
public interface PacketWatcher {
    void onPacket(Packet packet);
}

// Watch player packets
public interface PlayerPacketWatcher {
    void onPacket(PlayerRef player, Packet packet);
}

// Filter player packets (can block)
public interface PlayerPacketFilter {
    boolean filter(PlayerRef player, Packet packet);
}
```

## Watching Packets

```java
public class MyPlugin extends JavaPlugin {
    private PacketFilter chatWatcher;

    @Override
    protected void setup() {
        chatWatcher = PacketAdapters.registerInbound(
            (PlayerPacketWatcher) (player, packet) -> {
                if (packet instanceof ChatMessage chat) {
                    getLogger().info(player.getUsername() + " sent: " + chat.message);
                }
            }
        );
    }

    @Override
    protected void shutdown() {
        PacketAdapters.deregisterInbound(chatWatcher);
    }
}
```

## Filtering Packets

```java
// Block packets containing banned words
PacketFilter filter = PacketAdapters.registerInbound(
    (PlayerPacketFilter) (player, packet) -> {
        if (packet instanceof ChatMessage chat) {
            if (chat.message.contains("banned_word")) {
                return false; // Block packet
            }
        }
        return true; // Allow packet
    }
);
```

## Sending Packets

```java
public void sendMessage(PlayerRef player, String message) {
    ChatMessage packet = new ChatMessage();
    packet.message = message;
    player.getPacketHandler().write(packet);
}

// Send multiple packets
public void sendMultiple(PlayerRef player, Packet... packets) {
    player.getPacketHandler().write(packets);
}
```

## Wire Protocol

### Packet Frame Format

```
┌─────────────────────────────────────────────┐
│  4 bytes  │  4 bytes  │  N bytes            │
│  Length   │  Packet   │  Payload            │
│  (LE)     │  ID (LE)  │  (compressed/raw)   │
└─────────────────────────────────────────────┘
```

### Compression
- Uses Zstd compression
- Level configurable via `hytale.protocol.compressionLevel`
- Per-packet opt-in via `IS_COMPRESSED`
- Max payload: 1,677,721,600 bytes

## Transport Layer

| Transport | Description |
|-----------|-------------|
| TCP | `TCPTransport` - Standard TCP |
| QUIC | `QUICTransport` - QUIC protocol |

## Ping Metrics

Three types tracked:

| Type | Description |
|------|-------------|
| `Raw` | Raw network latency |
| `Direct` | Direct tick-based |
| `Tick` | Game tick synchronized |

Metrics: min, max, average, 99th percentile over 1s, 1m, 5m windows.

## Common Packets

### Connection

| Packet | Direction | Description |
|--------|-----------|-------------|
| `Connect` | C→S | Initial connection |
| `Disconnect` | Both | Connection termination |
| `Ping` | S→C | Latency request |
| `Pong` | C→S | Latency response |

### Player

| Packet | Direction | Description |
|--------|-----------|-------------|
| `PlayerMovement` | C→S | Position update |
| `PlayerTeleport` | S→C | Server teleport |
| `PlayerInventory` | S→C | Inventory sync |

### World

| Packet | Direction | Description |
|--------|-----------|-------------|
| `SetChunk` | S→C | Chunk data |
| `ServerSetBlock` | S→C | Block change |
| `SoundEvent` | S→C | Sound playback |

### Interface

| Packet | Direction | Description |
|--------|-----------|-------------|
| `ChatMessage` | Both | Chat message |
| `Notification` | S→C | UI notification |

## Sending Messages

```java
// Send message to player
player.sendMessage(Message.raw("Hello!"));
player.sendMessage(Message.text("Formatted text"));

// Broadcast to all
Universe.get().getPlayers().forEach(p ->
    p.sendMessage(Message.raw("Broadcast!"))
);

// Broadcast to world
world.getPlayerRefs().forEach(p ->
    p.sendMessage(Message.raw("World message!"))
);
```

## Server Transfer

```java
// Transfer player to another server
player.referToServer("other-server.example.com", 25565);
player.referToServer("other-server.example.com", 25565, customData);
```

## Best Practices

1. **Clean up adapters**: Always deregister in `shutdown()`
2. **Check packet types**: Use `instanceof` to identify
3. **Minimize filtering**: Keep filters efficient
4. **Use appropriate transport**: TCP for reliability, QUIC for performance
5. **Respect limits**: Don't exceed `MAX_SIZE`
6. **Thread safety**: Packet handling may be multithreaded

## Limitations

- Cannot create new packet types at runtime
- Serialization format is fixed per type
- No direct Netty channel access

## Documentation Reference
- Networking overview: `/docs/src/content/docs/api-reference/networking/overview.mdx`
- Packet types: `/docs/src/content/docs/api-reference/networking/packet-types.mdx`
- Packet handlers: `/docs/src/content/docs/api-reference/networking/packet-handlers.mdx`
- Client sync: `/docs/src/content/docs/api-reference/networking/client-sync.mdx`
