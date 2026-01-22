# Hytale VFX System

## Overview
The VFX (Visual Effects) System manages particles, trails, and model effects for visual feedback in the game.

## Asset Locations
- Particle configs: `Assets/Server/Particles/`
- Particle visuals: `Assets/Common/Particles/`
- Trail configs: `Assets/Server/Trail/`
- Model VFX: `Assets/Server/ModelVFX/`
- VFX visuals: `Assets/Common/VFX/`

## Particle System

### Basic Particle Configuration
```json
{
  "Id": "MyPlugin_CustomParticle",
  "Emitters": [
    {
      "Type": "Point",
      "Rate": 10,
      "Lifetime": 1.0,
      "Color": "#FF0000",
      "Size": 0.1
    }
  ]
}
```

### Emitter Types

| Type | Description |
|------|-------------|
| `Point` | Single point emission |
| `Sphere` | Sphere surface emission |
| `Box` | Box volume emission |
| `Cone` | Cone direction emission |
| `Line` | Line emission |

### Emitter Properties

| Property | Type | Description |
|----------|------|-------------|
| `Type` | string | Emitter type |
| `Rate` | number | Particles per second |
| `Lifetime` | number | Particle lifetime |
| `Color` | string | Particle color (hex) |
| `Size` | number | Particle size |
| `Velocity` | object | Initial velocity |
| `Gravity` | number | Gravity effect |
| `Drag` | number | Air resistance |
| `Sprite` | string | Particle texture |

### Complex Particle System
```json
{
  "Id": "MyPlugin_FireParticle",
  "Emitters": [
    {
      "Type": "Cone",
      "Rate": 50,
      "Lifetime": 0.8,
      "Sprite": "Particles/Fire_01.png",
      "Size": {
        "Start": 0.3,
        "End": 0.1
      },
      "Color": {
        "Start": "#FFAA00",
        "End": "#FF0000"
      },
      "Velocity": {
        "Direction": [0, 1, 0],
        "Speed": 2.0,
        "SpeedVariation": 0.5
      },
      "Gravity": -0.5,
      "Drag": 0.1,
      "Rotation": {
        "Speed": 45,
        "Variation": 30
      },
      "Opacity": {
        "Start": 1.0,
        "End": 0.0
      }
    }
  ]
}
```

### Particle in Item Interaction
```json
{
  "InteractionVars": {
    "Attack_Effect": {
      "Interactions": [{
        "Effects": {
          "WorldParticles": [
            { "SystemId": "Impact_Blade_01" }
          ],
          "LocalParticles": [
            { "SystemId": "Swing_Trail_01" }
          ]
        }
      }]
    }
  }
}
```

## Trail System

### Basic Trail
```json
{
  "Id": "MyPlugin_SwordTrail",
  "Width": 0.5,
  "Lifetime": 0.3,
  "Color": "#FFFFFF",
  "Texture": "Trails/Sword_Trail.png",
  "FadeOut": true
}
```

### Trail Properties

| Property | Type | Description |
|----------|------|-------------|
| `Width` | number | Trail width |
| `Lifetime` | number | Trail duration |
| `Color` | string | Trail color |
| `Texture` | string | Trail texture |
| `FadeOut` | bool | Fade at end |
| `UVMode` | string | Texture UV mode |
| `BlendMode` | string | Blending mode |

### Trail with Gradient
```json
{
  "Id": "MyPlugin_MagicTrail",
  "Width": {
    "Start": 0.8,
    "End": 0.1
  },
  "Lifetime": 0.5,
  "ColorGradient": [
    { "Position": 0.0, "Color": "#00FFFF" },
    { "Position": 0.5, "Color": "#0080FF" },
    { "Position": 1.0, "Color": "#000080" }
  ],
  "Texture": "Trails/Magic_Trail.png",
  "BlendMode": "Additive"
}
```

### Trail in Weapon
```json
{
  "InteractionVars": {
    "Swing_Effect": {
      "Trails": [
        {
          "TrailId": "MyPlugin_SwordTrail",
          "AttachPoint": "weapon_tip"
        }
      ]
    }
  }
}
```

## Model VFX

Effects attached to models:

```json
{
  "Id": "MyPlugin_EnchantedGlow",
  "Type": "Glow",
  "Color": "#8800FF",
  "Intensity": 1.5,
  "Pulse": {
    "Enabled": true,
    "Speed": 2.0,
    "MinIntensity": 0.8
  }
}
```

### Model VFX Types

| Type | Description |
|------|-------------|
| `Glow` | Emissive glow effect |
| `Outline` | Outline highlight |
| `Distortion` | Heat distortion |
| `Dissolve` | Dissolve effect |

### Model VFX in Items
```json
{
  "ModelVFX": [
    {
      "VFXId": "MyPlugin_EnchantedGlow",
      "Condition": "enchanted"
    }
  ]
}
```

## Using VFX in Blocks

### Block Particles
```json
{
  "Id": "MyPlugin_MagicOre",
  "Particles": [
    {
      "SystemId": "Sparkle_Magic",
      "Offset": [0.5, 0.5, 0.5]
    }
  ],
  "BlockParticleSetId": "MyPlugin_MagicBlockParticles"
}
```

### Block Light Effect
```json
{
  "Light": {
    "Color": "#8800FF",
    "Intensity": 10
  }
}
```

## Using VFX in NPCs

### NPC Particle Effects
```json
{
  "Particles": [
    {
      "SystemId": "Aura_Fire",
      "AttachPoint": "body"
    }
  ]
}
```

### Death Effect
```json
{
  "DeathEffects": {
    "Particles": [
      { "SystemId": "Death_Smoke" }
    ],
    "Sound": "SFX_NPC_Death"
  }
}
```

## Weather Particles

```json
{
  "Id": "MyPlugin_CustomWeather",
  "Particles": {
    "Rain": {
      "SystemId": "Weather_Rain",
      "Density": 100,
      "Wind": [1, 0, 0.5]
    }
  }
}
```

## Spawning Particles in Code

Particles are typically spawned through interaction effects defined in JSON, but can also be triggered through the event system and packet handlers.

## Blend Modes

| Mode | Description |
|------|-------------|
| `Normal` | Standard blending |
| `Additive` | Adds to background |
| `Multiply` | Multiplies colors |
| `SoftLight` | Soft overlay |

## Particle Performance

### Optimization Tips

1. **Limit rate**: Use lowest rate that looks good
2. **Short lifetime**: Keep particles short-lived
3. **Simple sprites**: Use simple textures
4. **Pool particles**: System pools automatically
5. **LOD**: Reduce at distance

### Performance Properties
```json
{
  "MaxParticles": 100,
  "LOD": {
    "Distance": 50,
    "RateMultiplier": 0.5
  },
  "CullDistance": 100
}
```

## Common Particle Systems

### Impact Particles
```json
{
  "Id": "Impact_Blade_01",
  "Emitters": [{
    "Type": "Point",
    "Burst": 10,
    "Lifetime": 0.3,
    "Sprite": "Particles/Spark.png",
    "Velocity": {
      "RandomDirection": true,
      "Speed": 5.0
    }
  }]
}
```

### Ambient Particles
```json
{
  "Id": "Ambient_Fireflies",
  "Emitters": [{
    "Type": "Box",
    "Rate": 2,
    "Lifetime": 5.0,
    "Sprite": "Particles/Glow.png",
    "Size": 0.1,
    "Movement": {
      "Type": "Wander",
      "Speed": 0.5
    }
  }]
}
```

## File Structure

```
my-plugin/
├── assets/
│   ├── server/
│   │   ├── particles/
│   │   │   └── MyPlugin_CustomParticle.json
│   │   └── trail/
│   │       └── MyPlugin_CustomTrail.json
│   └── common/
│       └── particles/
│           └── myplugin/
│               └── Custom_Sprite.png
```

## Best Practices

1. **Prefix IDs**: Use plugin name prefix
2. **Optimize performance**: Balance quality vs. performance
3. **Test on hardware**: Test on various hardware
4. **Use LOD**: Reduce particles at distance
5. **Match style**: Match Hytale's visual style
6. **Provide fallbacks**: Have simpler versions
7. **Consider gameplay**: Don't obscure important visuals

## Documentation Reference
- Particles: `/docs/src/content/docs/api-reference/assets/vfx/particles.mdx`
- Trails: `/docs/src/content/docs/api-reference/assets/vfx/trails.mdx`
- Model effects: `/docs/src/content/docs/api-reference/assets/vfx/model-effects.mdx`
