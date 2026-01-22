package com.riprod.hexcode.visual;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.spatial.SpatialResource;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.Color;
import com.hypixel.hytale.protocol.Direction;
import com.hypixel.hytale.protocol.Position;
import com.hypixel.hytale.protocol.packets.world.SpawnParticleSystem;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.glyph.GlyphShape;
import it.unimi.dsi.fastutil.objects.ObjectList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * Handles spawning and updating particle-based glyph visuals.
 *
 * Instead of using 3D models (which require .blockymodel files), this renderer
 * displays glyphs as colored particle sprites that can be positioned, scaled,
 * rotated, and tinted in real-time.
 */
public class GlyphParticleRenderer {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /**
     * Default particle system ID for glyph sprites.
     * Defined in Assets/Server/Particles/Hexcode/Glyphs/Glyph_Orbit.particlesystem
     */
    public static final String GLYPH_PARTICLE_SYSTEM = "Hexcode/Glyphs/Glyph_Orbit";

    /**
     * Ambient particle system ID for hover/highlight effects.
     * Defined in Assets/Server/Particles/Hexcode/Glyphs/Glyph_Ambient.particlesystem
     */
    public static final String GLYPH_AMBIENT_SYSTEM = "Hexcode/Glyphs/Glyph_Ambient";

    /**
     * Distance within which players will see glyph particles.
     */
    public static final double PARTICLE_VIEW_DISTANCE = 50.0;

    private static GlyphParticleRenderer instance;

    private GlyphParticleRenderer() {
    }

    /**
     * Get the singleton instance.
     */
    public static GlyphParticleRenderer getInstance() {
        if (instance == null) {
            instance = new GlyphParticleRenderer();
        }
        return instance;
    }

    /**
     * Spawn a glyph particle at the given position.
     *
     * @param componentAccessor The component accessor for entity queries
     * @param position          World position to spawn at
     * @param shape             Glyph shape data (texture, color, scale)
     */
    public void spawnGlyphParticle(
            @Nonnull ComponentAccessor<EntityStore> componentAccessor,
            @Nonnull Vector3d position,
            @Nonnull GlyphShape shape
    ) {
        spawnGlyphParticle(componentAccessor, position, shape, 0.0f, null);
    }

    /**
     * Spawn a glyph particle at the given position with rotation.
     *
     * @param componentAccessor The component accessor for entity queries
     * @param position          World position to spawn at
     * @param shape             Glyph shape data (texture, color, scale)
     * @param rotation          Rotation angle in radians (around Y axis)
     * @param excludePlayer     Optional player ref to exclude from receiving the packet
     */
    public void spawnGlyphParticle(
            @Nonnull ComponentAccessor<EntityStore> componentAccessor,
            @Nonnull Vector3d position,
            @Nonnull GlyphShape shape,
            float rotation,
            @Nullable Ref<EntityStore> excludePlayer
    ) {
        // Get nearby players to send the particle to
        List<Ref<EntityStore>> nearbyPlayers = getNearbyPlayers(componentAccessor, position, PARTICLE_VIEW_DISTANCE);

        if (nearbyPlayers.isEmpty()) {
            return;
        }

        // Create the particle spawn packet
        SpawnParticleSystem packet = createGlyphPacket(position, shape, rotation);

        // Send to all nearby players (except excluded)
        sendToPlayers(componentAccessor, packet, nearbyPlayers, excludePlayer);

        LOGGER.atFine().log("Spawned glyph particle at (%.1f, %.1f, %.1f) with color #%06X",
                position.x, position.y, position.z, shape.getColor());
    }

    /**
     * Spawn a glyph particle visible only to specific players.
     *
     * @param componentAccessor The component accessor for entity queries
     * @param position          World position to spawn at
     * @param shape             Glyph shape data
     * @param rotation          Rotation angle in radians
     * @param playerRefs        List of players to send the particle to
     */
    public void spawnGlyphParticleForPlayers(
            @Nonnull ComponentAccessor<EntityStore> componentAccessor,
            @Nonnull Vector3d position,
            @Nonnull GlyphShape shape,
            float rotation,
            @Nonnull List<Ref<EntityStore>> playerRefs
    ) {
        if (playerRefs.isEmpty()) {
            return;
        }

        SpawnParticleSystem packet = createGlyphPacket(position, shape, rotation);
        sendToPlayers(componentAccessor, packet, playerRefs, null);
    }

    /**
     * Update a glyph particle's position (re-spawn at new location).
     * Since particles are one-shot effects, we spawn a new one at the new position.
     *
     * @param componentAccessor The component accessor
     * @param newPosition       New world position
     * @param shape             Glyph shape data
     * @param rotation          Current rotation angle
     */
    public void updateGlyphParticlePosition(
            @Nonnull ComponentAccessor<EntityStore> componentAccessor,
            @Nonnull Vector3d newPosition,
            @Nonnull GlyphShape shape,
            float rotation
    ) {
        // For continuous display, we re-spawn the particle at the new position
        // This should be called periodically (e.g., every few ticks) to maintain visibility
        spawnGlyphParticle(componentAccessor, newPosition, shape, rotation, null);
    }

    /**
     * Create a SpawnParticleSystem packet for a glyph.
     *
     * @param position World position
     * @param shape    Glyph shape data
     * @param rotation Rotation angle in radians (Y axis)
     * @return Configured packet ready to send
     */
    private SpawnParticleSystem createGlyphPacket(Vector3d position, GlyphShape shape, float rotation) {
        // Create position
        Position pos = new Position(position.x, position.y, position.z);

        // Create rotation (yaw, pitch, roll)
        Direction dir = null;
        if (rotation != 0.0f) {
            dir = new Direction(rotation, 0.0f, 0.0f);
        }

        // Create color from shape
        Color color = new Color(
                (byte) shape.getRed(),
                (byte) shape.getGreen(),
                (byte) shape.getBlue()
        );

        // Use the glyph's texture-specific particle system if available,
        // otherwise fall back to default glyph sprite
        String particleSystemId = shape.getTextureId() != null
                ? shape.getTextureId()
                : GLYPH_PARTICLE_SYSTEM;

        return new SpawnParticleSystem(
                particleSystemId,
                pos,
                dir,
                shape.getScale(),
                color
        );
    }

    /**
     * Get nearby players within a certain distance of a position.
     *
     * @param componentAccessor Component accessor for spatial queries
     * @param position          Center position
     * @param distance          Maximum distance
     * @return List of player entity refs
     */
    private List<Ref<EntityStore>> getNearbyPlayers(
            ComponentAccessor<EntityStore> componentAccessor,
            Vector3d position,
            double distance
    ) {
        SpatialResource<Ref<EntityStore>, EntityStore> playerSpatialResource =
                componentAccessor.getResource(EntityModule.get().getPlayerSpatialResourceType());

        ObjectList<Ref<EntityStore>> playerRefs = SpatialResource.getThreadLocalReferenceList();
        playerSpatialResource.getSpatialStructure().collect(position, distance, playerRefs);

        return playerRefs;
    }

    /**
     * Send a packet to a list of players.
     *
     * @param componentAccessor Component accessor
     * @param packet            Packet to send
     * @param playerRefs        Players to send to
     * @param excludePlayer     Optional player to exclude
     */
    private void sendToPlayers(
            ComponentAccessor<EntityStore> componentAccessor,
            SpawnParticleSystem packet,
            List<Ref<EntityStore>> playerRefs,
            @Nullable Ref<EntityStore> excludePlayer
    ) {
        for (Ref<EntityStore> playerRef : playerRefs) {
            if (!playerRef.isValid()) {
                continue;
            }

            if (excludePlayer != null && playerRef.equals(excludePlayer)) {
                continue;
            }

            PlayerRef playerRefComponent = componentAccessor.getComponent(playerRef, PlayerRef.getComponentType());
            if (playerRefComponent != null) {
                playerRefComponent.getPacketHandler().writeNoCache(packet);
            }
        }
    }

    /**
     * Spawn an ambient particle effect around a glyph (e.g., for hover highlight).
     *
     * @param componentAccessor Component accessor
     * @param position          Glyph position
     * @param color             Effect color (RGB int)
     * @param intensity         Effect intensity/scale
     */
    public void spawnAmbientEffect(
            @Nonnull ComponentAccessor<EntityStore> componentAccessor,
            @Nonnull Vector3d position,
            int color,
            float intensity
    ) {
        List<Ref<EntityStore>> nearbyPlayers = getNearbyPlayers(componentAccessor, position, PARTICLE_VIEW_DISTANCE);

        if (nearbyPlayers.isEmpty()) {
            return;
        }

        Color particleColor = new Color(
                (byte) ((color >> 16) & 0xFF),
                (byte) ((color >> 8) & 0xFF),
                (byte) (color & 0xFF)
        );

        SpawnParticleSystem packet = new SpawnParticleSystem(
                GLYPH_AMBIENT_SYSTEM,
                new Position(position.x, position.y, position.z),
                null,
                intensity,
                particleColor
        );

        sendToPlayers(componentAccessor, packet, nearbyPlayers, null);
    }

    /**
     * Spawn a hover highlight effect.
     *
     * @param componentAccessor Component accessor
     * @param position          Glyph position
     * @param color             Glyph color for highlight tinting
     */
    public void spawnHoverHighlight(
            @Nonnull ComponentAccessor<EntityStore> componentAccessor,
            @Nonnull Vector3d position,
            int color
    ) {
        spawnAmbientEffect(componentAccessor, position, color, 1.5f);
    }

    /**
     * Reset the singleton instance (for testing).
     */
    public static void reset() {
        instance = null;
    }
}
