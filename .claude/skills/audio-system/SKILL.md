# Hytale Audio System

## Overview
The Audio System manages sound effects, music, ambient audio, and audio processing through JSON asset definitions.

## Asset Locations
- Sound events: `Assets/Server/Audio/SoundEvents/`
- Audio categories: `Assets/Server/Audio/AudioCategories/`
- Item sounds: `Assets/Server/Audio/ItemSounds/`
- Block sounds: `Assets/Server/Audio/SoundSets/`
- Reverb: `Assets/Server/Audio/Reverb/`
- EQ: `Assets/Server/Audio/EQ/`
- Ambience: `Assets/Server/Audio/AmbienceFX/`
- Sound files: `Assets/Common/Sounds/`
- Music files: `Assets/Common/Music/`

## Sound File Formats

| Format | Extension | Usage |
|--------|-----------|-------|
| Ogg Vorbis | `.ogg` | Sound effects and music |
| Hytale Audio | `.lpf` | Hytale-specific format |

## Sound Events

### Basic Sound Event
```json
{
  "Layers": [
    {
      "Files": [
        "Sounds/Items/Chest/Chest_Open.ogg"
      ]
    }
  ],
  "Volume": 2.0,
  "Parent": "SFX_Attn_Quiet"
}
```

### Looping Sound
```json
{
  "Layers": [
    {
      "Files": [
        "Sounds/Ambient/Fire_Loop.ogg"
      ],
      "Looping": true
    }
  ],
  "Volume": -4.0,
  "Parent": "SFX_Attn_Moderate"
}
```

### Randomized Sound
```json
{
  "Layers": [
    {
      "Files": [
        "Sounds/Footsteps/Step_01.ogg",
        "Sounds/Footsteps/Step_02.ogg",
        "Sounds/Footsteps/Step_03.ogg"
      ],
      "RandomSettings": {
        "MinVolume": -2.0,
        "MinPitch": -0.1,
        "MaxPitch": 0.1
      },
      "Volume": -5.0
    }
  ],
  "Volume": -10.0,
  "PreventSoundInterruption": true,
  "MaxInstance": 15,
  "Parent": "SFX_Attn_Moderate"
}
```

### Sound Event Properties

| Property | Type | Description |
|----------|------|-------------|
| `Parent` | string | Attenuation template |
| `Layers` | array | Sound layers |
| `Volume` | number | Volume in dB |
| `Looping` | bool | Whether sound loops |
| `PreventSoundInterruption` | bool | Prevent overlap |
| `MaxInstance` | int | Max concurrent instances |

### Layer Properties

| Property | Type | Description |
|----------|------|-------------|
| `Files` | string[] | Audio file paths |
| `Volume` | number | Layer volume in dB |
| `Looping` | bool | Layer-specific looping |
| `RandomSettings` | object | Randomization |

### RandomSettings

| Property | Type | Description |
|----------|------|-------------|
| `MinVolume` | number | Minimum volume variation |
| `MinPitch` | number | Minimum pitch variation |
| `MaxPitch` | number | Maximum pitch variation |

## Attenuation Templates

| Template | Description |
|----------|-------------|
| `SFX_Attn_VeryQuiet` | Very short range |
| `SFX_Attn_Quiet` | Short range |
| `SFX_Attn_Moderate` | Medium range |
| `SFX_Attn_Loud` | Long range |
| `SFX_Attn_VeryLoud` | Very long range |

## Audio Categories

Volume groups for sound types:

```json
{
  "Volume": -14.0
}
```

### Standard Categories

| Category | Base Volume | Purpose |
|----------|-------------|---------|
| `AudioCat_Music` | -14 dB | Background music |
| `AudioCat_Weapons` | 0 dB | Weapon sounds |
| `AudioCat_Footsteps` | 0 dB | Footsteps |
| `AudioCat_Inventory` | 0 dB | Inventory UI |
| `AudioCat_NPC` | 0 dB | NPC sounds |
| `AudioCat_Discovery` | 0 dB | Discovery sounds |

## Item Sound Sets

```json
{
  "SoundEvents": {
    "Drop": "SFX_Drop_Weapons_Blade_Large",
    "Drag": "SFX_Drag_Weapons_Blade_Large"
  }
}
```

### Available Item Sound Sets

**Armor:**
- `ISS_Armor_Cloth`, `ISS_Armor_Leather`, `ISS_Armor_Heavy`

**Weapons:**
- `ISS_Weapons_Blade_Large`, `ISS_Weapon_Blade_Small`
- `ISS_Weapon_Blunt_Large`, `ISS_Weapons_Blunt_Small`
- `ISS_Weapons_Stone_Large`, `ISS_Weapons_Stone_Small`
- `ISS_Weapons_Wood`, `ISS_Weapons_Wand`
- `ISS_Weapons_Shield_Metal`, `ISS_Weapons_Shield_Wood`
- `ISS_Weapons_Arrows`, `ISS_Weapons_Books`

**Blocks:**
- `ISS_Blocks_Stone`, `ISS_Blocks_Wood`
- `ISS_Blocks_Gravel`, `ISS_Blocks_Soft`, `ISS_Blocks_Splatty`

**Items:**
- `ISS_Items_Metal`, `ISS_Items_Leather`, `ISS_Items_Cloth`
- `ISS_Items_Ingots`, `ISS_Items_Gems`, `ISS_Items_Bones`
- `ISS_Items_Potion`, `ISS_Items_Paper`, `ISS_Items_Foliage`
- `ISS_Items_Seeds`, `ISS_Items_Clay`, `ISS_Items_Shells`

## Reverb Presets

```json
{
  "DryGain": 0,
  "ModalDensity": 1,
  "Diffusion": 1,
  "Gain": -15,
  "HighFrequencyGain": -39,
  "DecayTime": 11,
  "HighFrequencyDecayRatio": 1.3,
  "ReflectionGain": -10.4,
  "ReflectionDelay": 0.2,
  "LateReverbGain": 0,
  "LateReverbDelay": 0.02,
  "RoomRolloffFactor": 0,
  "AirAbsorbptionHighFrequencyGain": -0.05,
  "LimitDecayHighFrequency": false
}
```

### Available Reverb Presets

| Preset | Description |
|--------|-------------|
| `Rev_Default` | Default outdoor |
| `Rev_Cave` | Cave acoustics |
| `Rev_Forest` | Forest |
| `Rev_Plains` | Open plains |
| `Rev_Mountain` | Mountain |
| `Rev_Swamp` | Swamp |
| `Rev_Temple` | Temple interior |
| `Rev_Village` | Village area |
| `Rev_Mineshaft` | Mineshaft |

## EQ Presets

```json
{
  "LowGain": 0,
  "LowCutOff": 300,
  "LowMidGain": -17.19,
  "LowMidCenter": 1000,
  "LowMidWidth": 1,
  "HighMidGain": -17.9,
  "HighMidCenter": 1500,
  "HighMidWidth": 1,
  "HighGain": -17.9,
  "HighCutOff": 4000
}
```

### Available EQ Presets

| Preset | Description |
|--------|-------------|
| `EQ_Default` | No modification |
| `EQ_Underwater` | Muffled underwater |

## Sound Directory Structure

| Directory | Description |
|-----------|-------------|
| `Blocks/` | Block sounds |
| `Crafting/` | Crafting sounds |
| `Effects/` | Special effects |
| `Environments/` | Ambient sounds |
| `Events/` | Event sounds |
| `Items/` | Item sounds |
| `Magic/` | Magic effects |
| `Movement/` | Movement sounds |
| `NPC/` | NPC sounds |
| `PlayerActions/` | Player actions |
| `Projectiles/` | Projectile sounds |
| `Tools/` | Tool sounds |
| `UI/` | Interface sounds |
| `Weapons/` | Weapon sounds |

## Using Audio in Items

```json
{
  "Id": "Weapon_Sword_Iron",
  "ItemSoundSetId": "ISS_Weapons_Blade_Large",
  "InteractionVars": {
    "Swing_Effect": {
      "Interactions": [{
        "Effects": {
          "WorldSoundEventId": "SFX_Sword_T2_Swing",
          "LocalSoundEventId": "SFX_Sword_T2_Swing_Local"
        }
      }]
    }
  }
}
```

### Sound Types in Interactions

| Field | Description |
|-------|-------------|
| `WorldSoundEventId` | Sound heard by all |
| `LocalSoundEventId` | Sound heard by player only |
| `SoundEventId` | General sound reference |

## Creating Custom Audio

### Step 1: Create Audio File
Save as `.ogg` in `Assets/Common/Sounds/`:
```
MyPlugin/Sounds/
├── Weapons/
│   └── Custom_Swing.ogg
└── Items/
    └── Custom_Pickup.ogg
```

### Step 2: Create Sound Event
```json
{
  "Layers": [
    {
      "Files": [
        "MyPlugin/Sounds/Weapons/Custom_Swing.ogg"
      ],
      "RandomSettings": {
        "MinVolume": -2,
        "MinPitch": -0.1,
        "MaxPitch": 0.1
      }
    }
  ],
  "Volume": -3.0,
  "Parent": "SFX_Attn_Moderate"
}
```

### Step 3: Create ItemSoundSet (Optional)
```json
{
  "SoundEvents": {
    "Drop": "SFX_MyPlugin_Weapon_Drop",
    "Drag": "SFX_MyPlugin_Weapon_Drag"
  }
}
```

### Step 4: Reference in Item
```json
{
  "Id": "MyPlugin_Weapon_Custom",
  "ItemSoundSetId": "ISS_MyPlugin_Weapon"
}
```

## Volume Guidelines

| Sound Type | Typical Volume | Distance |
|------------|----------------|----------|
| UI sounds | 0 to -5 dB | Local only |
| Footsteps | -10 to -15 dB | Short |
| Weapons | -3 to -8 dB | Medium |
| Ambience | -15 to -25 dB | Long |
| Music | -12 to -18 dB | Global |
| NPC voices | -5 to -10 dB | Medium |

## Best Practices

1. **Use appropriate attenuation**: Match `Parent` to sound type
2. **Normalize audio**: Keep peaks at -3 to -6 dB
3. **Add randomization**: Use `RandomSettings` for variety
4. **Limit instances**: Set `MaxInstance` to prevent overload
5. **Use categories**: Assign to appropriate categories
6. **Test in-game**: Verify volumes and distances
7. **Optimize quality**: 44.1kHz, mono for most SFX
8. **Prefix IDs**: Use plugin name prefix

## Documentation Reference
- Audio overview: `/docs/src/content/docs/api-reference/assets/audio/overview.mdx`
- Sound events: `/docs/src/content/docs/api-reference/assets/audio/sound-events.mdx`
- Sound sets: `/docs/src/content/docs/api-reference/assets/audio/sound-sets.mdx`
- Effects: `/docs/src/content/docs/api-reference/assets/audio/effects.mdx`
