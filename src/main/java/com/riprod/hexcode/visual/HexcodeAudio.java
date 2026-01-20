package com.riprod.hexcode.visual;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Audio manager for Hexcode mod sounds.
 * Handles all sound effects for glyph mode, composition, and casting.
 */
public class HexcodeAudio {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    // Sound event IDs - these would be registered in asset files
    public static final String SOUND_MODE_ENTER = "hexcode:glyph_mode_enter";
    public static final String SOUND_MODE_EXIT = "hexcode:glyph_mode_exit";
    public static final String SOUND_GLYPH_HOVER = "hexcode:glyph_hover";
    public static final String SOUND_DRAG_START = "hexcode:drag_start";
    public static final String SOUND_GLYPH_PLACE = "hexcode:glyph_place";
    public static final String SOUND_INVALID_PLACEMENT = "hexcode:invalid_placement";
    public static final String SOUND_GLYPH_LINK = "hexcode:glyph_link";
    public static final String SOUND_UNDO = "hexcode:undo";
    public static final String SOUND_HEX_CAST = "hexcode:hex_cast";
    public static final String SOUND_CAST_FAIL_MANA = "hexcode:cast_fail_mana";

    private static HexcodeAudio instance;

    private HexcodeAudio() {}

    public static synchronized HexcodeAudio getInstance() {
        if (instance == null) {
            instance = new HexcodeAudio();
        }
        return instance;
    }

    /**
     * Play glyph mode enter sound.
     */
    public void playModeEnter(Store<EntityStore> store, Ref<EntityStore> playerRef) {
        playSound2D(store, playerRef, SOUND_MODE_ENTER);
        LOGGER.atInfo().log("Playing glyph mode enter sound");
    }

    /**
     * Play glyph mode exit sound.
     */
    public void playModeExit(Store<EntityStore> store, Ref<EntityStore> playerRef) {
        playSound2D(store, playerRef, SOUND_MODE_EXIT);
        LOGGER.atInfo().log("Playing glyph mode exit sound");
    }

    /**
     * Play glyph hover sound.
     */
    public void playGlyphHover(Store<EntityStore> store, Ref<EntityStore> playerRef) {
        playSound2D(store, playerRef, SOUND_GLYPH_HOVER, 0.5f, 1.0f);
        LOGGER.atInfo().log("Playing glyph hover sound");
    }

    /**
     * Play drag start sound.
     */
    public void playDragStart(Store<EntityStore> store, Ref<EntityStore> playerRef) {
        playSound2D(store, playerRef, SOUND_DRAG_START);
        LOGGER.atInfo().log("Playing drag start sound");
    }

    /**
     * Play glyph placement sound.
     */
    public void playGlyphPlace(Store<EntityStore> store, Ref<EntityStore> playerRef, Vector3d position) {
        playSound3D(store, SOUND_GLYPH_PLACE, position);
        LOGGER.atInfo().log("Playing glyph place sound");
    }

    /**
     * Play invalid placement sound.
     */
    public void playInvalidPlacement(Store<EntityStore> store, Ref<EntityStore> playerRef) {
        playSound2D(store, playerRef, SOUND_INVALID_PLACEMENT);
        LOGGER.atInfo().log("Playing invalid placement sound");
    }

    /**
     * Play glyph link sound.
     */
    public void playGlyphLink(Store<EntityStore> store, Ref<EntityStore> playerRef, Vector3d position) {
        playSound3D(store, SOUND_GLYPH_LINK, position);
        LOGGER.atInfo().log("Playing glyph link sound");
    }

    /**
     * Play undo sound.
     */
    public void playUndo(Store<EntityStore> store, Ref<EntityStore> playerRef) {
        playSound2D(store, playerRef, SOUND_UNDO);
        LOGGER.atInfo().log("Playing undo sound");
    }

    /**
     * Play hex cast sound.
     */
    public void playHexCast(Store<EntityStore> store, Ref<EntityStore> playerRef, Vector3d position) {
        playSound3D(store, SOUND_HEX_CAST, position);
        LOGGER.atInfo().log("Playing hex cast sound");
    }

    /**
     * Play cast failure (insufficient mana) sound.
     */
    public void playCastFailMana(Store<EntityStore> store, Ref<EntityStore> playerRef) {
        playSound2D(store, playerRef, SOUND_CAST_FAIL_MANA);
        LOGGER.atInfo().log("Playing cast fail (mana) sound");
    }

    /**
     * Play a 2D sound to a specific player.
     */
    private void playSound2D(Store<EntityStore> store, Ref<EntityStore> playerRef, String soundId) {
        playSound2D(store, playerRef, soundId, 1.0f, 1.0f);
    }

    /**
     * Play a 2D sound to a specific player with volume/pitch modifiers.
     */
    private void playSound2D(Store<EntityStore> store, Ref<EntityStore> playerRef, String soundId,
                              float volume, float pitch) {
        int soundIndex = getSoundEventIndex(soundId);
        if (soundIndex != 0) {
            SoundUtil.playSoundEvent2d(playerRef, soundIndex, SoundCategory.SFX, volume, pitch, store);
        }
    }

    /**
     * Play a 3D positional sound.
     */
    private void playSound3D(Store<EntityStore> store, String soundId, Vector3d position) {
        playSound3D(store, soundId, position, 1.0f, 1.0f);
    }

    /**
     * Play a 3D positional sound with volume/pitch modifiers.
     */
    private void playSound3D(Store<EntityStore> store, String soundId, Vector3d position,
                              float volume, float pitch) {
        int soundIndex = getSoundEventIndex(soundId);
        if (soundIndex != 0) {
            SoundUtil.playSoundEvent3d(soundIndex, SoundCategory.SFX, position.x, position.y, position.z,
                    volume, pitch, store);
        }
    }

    /**
     * Get sound event index from ID.
     * Falls back to a generic sound if the custom sound doesn't exist.
     */
    private int getSoundEventIndex(String soundId) {
        SoundEvent soundEvent = SoundEvent.getAssetMap().getAsset(soundId);
        if (soundEvent != null) {
            return SoundEvent.getAssetMap().getIndex(soundId);
        }
        // Fallback to generic UI sound
        return SoundEvent.getAssetMap().getIndex("SFX_UI_Click");
    }
}
