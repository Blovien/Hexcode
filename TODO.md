# Hexcode Future Implementation Plan

This document outlines future enhancements for the Hexcode spell-crafting system.
These items are documented here for future implementation after the core system
is stable and tested.

---

## Current Scope - Completed

The following features have been implemented:

### Per-World Book Persistence (Hybrid Storage)
- [x] UUID-based book identification stored in ItemStack metadata
- [x] Per-world storage at `<world>/hexcode/books/<uuid>.json` for book data
- [x] Per-world storage at `<world>/hexcode/books/<uuid>/queued.json` for queued hex
- [x] Caching with dirty flag for efficient saves (`WorldHexDataStore`)
- [x] Migration support for legacy formats

### ItemStack Immutability Fix
- [x] Rewrote `HexBookMetadata` for Hytale's immutable ItemStack API
- [x] Uses `withMetadata()` and `getFromMetadataOrNull()` with Codec
- [x] Added `BookUUIDResult` record for proper return value handling
- [x] Book UUID stored in ItemStack (rarely changes)
- [x] Queued hex stored in world files via `WorldHexDataStore` (frequently changes)

### Per-Book Queued Spell Storage
- [x] `WorldHexDataStore.getQueuedHex()` and `setQueuedHex()` methods
- [x] Queued hex stored per-book (swapping books changes active spell)
- [x] Composition loads from `WorldHexDataStore` on glyph mode entry
- [x] Composition saves to `WorldHexDataStore` on glyph mode exit
- [x] Created `InventoryUtil` helper for updating inventory slots

### Tag-Based Book Detection
- [x] `Template_HexBook.json` with `Tags.Family = "HexBook"`
- [x] `HexStaffUtil.isHexBook()` uses tag-based detection
- [x] Fallback to ID matching for backwards compatibility
- [x] Third-party books inherit from `Template_HexBook` for auto-detection

### Interaction System Updates
- [x] `GlyphMode.enter()` takes World and Inventory for proper data loading
- [x] `GlyphMode.exit()` takes World for proper data saving
- [x] `GlyphModeManager` updated to pass World and Inventory
- [x] `HexcodeGlyphModeToggle` uses new enter/exit signatures
- [x] `HexcodeGlyphAction` casts from `WorldHexDataStore` when outside glyph mode

### Bug Fixes
- [x] Context isolation in `HexExecutor` (nested vs chain handling)
- [x] `DelayedExecutionManager` for BEAM/PROJECTILE delayed execution
- [x] Mana cost calculation traverses hex tree (not hardcoded)
- [x] `GlyphInstance` null safety with `isValid()` and `createInvalid()`

---

## 1. Asset Integration

### Goal
Convert glyphs from programmatic registration to proper Hytale asset definitions
with auto-detection and hot-reloading support.

### Implementation

#### 1.1 Create GlyphType Asset Class
```java
public class GlyphType extends Asset {
    public static final AssetCodec<GlyphType> CODEC = ...;

    private String id;
    private String displayName;
    private GlyphRole role;
    private float basePower;
    private float baseManaCost;
    private float baseVariability;
    private String modelPath;
    private String drawingTemplatePath;
    private Map<String, Object> properties;
    private List<String> compatibleModifiers;
}
```

#### 1.2 Asset File Location
- **Path**: `assets/glyphtype/` directory
- **Format**: JSON files following Hytale asset conventions
- **Naming**: `{namespace}_{glyph_name}.json` (e.g., `hexcode_fire.json`)

#### 1.3 Asset Map Integration
```java
// In GlyphRegistry
private AssetMap<GlyphType> glyphAssets;

public void initializeFromAssets(AssetMap<GlyphType> assets) {
    this.glyphAssets = assets;
    for (GlyphType asset : assets.getAll()) {
        registerGlyphFromAsset(asset);
    }
}
```

#### 1.4 Migration Strategy
1. Keep existing `GlyphAssetDefinition` for backward compatibility
2. Add adapter to convert `GlyphType` assets to `GlyphAssetDefinition`
3. Deprecate programmatic registration with warnings
4. Remove deprecated API in future major version

---

## 2. Third-Party Hex Book Extensions

### Goal
Allow third-party plugins to define new book types via JSON assets without
requiring code dependencies on Hexcode.

### Implementation

#### 2.1 Create HexBookType Asset
```json
{
  "Parent": "hexcode:hex_book",
  "DisplayName": "Fire Hex Book",
  "MaxGlyphs": 50,
  "MaxHexes": 30,
  "CastingModifiers": {
    "fire": 1.5,
    "ice": 0.5
  },
  "CustomData": {
    "fireResistance": true
  }
}
```

#### 2.2 Asset Structure
```java
public class HexBookType extends Asset {
    private String parent;              // Inheritance from base book
    private String displayName;
    private int maxGlyphs;
    private int maxHexes;
    private Map<String, Float> castingModifiers;  // Element -> multiplier
    private Map<String, Object> customData;
}
```

#### 2.3 Auto-Detection
- Third parties add `assets/hexbooktype/Fire_Hex_Book.json`
- System auto-detects and registers on load
- No code dependency required
- Book type determined by item ID matching

#### 2.4 Integration Points
- `WorldHexDataStore`: Support custom book types in storage path
- `HexBookDataManager`: Auto-detect book type from item
- `GlyphModeManager`: Apply book-specific casting modifiers

---

## 3. Third-Party Hex Staff Extensions

### Goal
Allow third-party plugins to define custom staff types with unique casting
behaviors and visual effects.

### Implementation

#### 3.1 Create HexStaffType Asset
```json
{
  "DisplayName": "Inferno Staff",
  "CastingStyle": "channeled",
  "CastingModifiers": {
    "fire": 2.0,
    "water": 0.25
  },
  "VisualColors": {
    "primary": "#FF4500",
    "secondary": "#FFD700",
    "particle": "#FF6347"
  },
  "ChargeTime": 1.5,
  "CooldownMultiplier": 0.8
}
```

#### 3.2 Casting Styles
| Style | Description |
|-------|-------------|
| `instant` | Cast immediately on use |
| `channeled` | Hold to charge, release to cast |
| `charged` | Click to start charge, click again to cast |
| `toggle` | Toggle on/off sustained effect |

#### 3.3 Staff Properties
- **CastingModifiers**: Element multipliers (stacks with book)
- **VisualColors**: Primary, secondary, and particle colors
- **ChargeTime**: Time to fully charge (channeled/charged styles)
- **CooldownMultiplier**: Modifies base spell cooldown
- **ManaEfficiency**: Modifies mana cost

---

## 4. Casting Modifier System

### Goal
Implement a comprehensive modifier system where book and staff modifiers
are applied during spell execution.

### Implementation

#### 4.1 Element Tags
Add element tags to glyphs in their asset definitions:
```json
{
  "id": "hexcode:fire",
  "elements": ["fire", "damage", "dot"]
}
```

#### 4.2 Modifier Application
```java
public float applyModifiers(float baseDamage, SpellContext context) {
    float bookMod = getBookModifier(context.getGlyph().getElements());
    float staffMod = getStaffModifier(context.getGlyph().getElements());

    // Stack multiplicatively
    return baseDamage * bookMod * staffMod;
}
```

#### 4.3 Modifier Stacking Rules
- **Same source**: Highest value wins (e.g., two fire books)
- **Different sources**: Multiply together (book × staff)
- **Caps**: Maximum 3.0x boost, minimum 0.1x reduction

#### 4.4 Integration Points
- `SpellContext`: Add `getBookModifiers()` and `getStaffModifiers()`
- `EffectGlyph.applyEffect()`: Apply modifiers before damage calculation
- `GlyphModeManager`: Cache active book/staff modifiers per player

---

## 5. Additional Future Work

### 5.1 Glyph Discovery System
- Players discover new glyphs through exploration/quests
- Undiscovered glyphs appear as "???" in UI
- Discovery unlocks glyph for loadout selection

### 5.2 Spell Combo System
- Certain glyph combinations create special effects
- Fire + Ice = Steam (AoE vision reduction)
- Lightning + Water = Electrocution (chain damage)

### 5.3 Visual Customization
- Player-chosen glyph colors/styles
- Unlockable visual effects
- Staff/book skins

### 5.4 Spell Saving/Favorites
- Save frequently used hex configurations
- Quick-cast from saved spells
- Share spell configurations with other players

### 5.5 PvP Balance
- Spell interrupt mechanics
- Defensive counter-spells
- Anti-magic zones

---

## Implementation Priority

### Phase 1 (Next)
1. Asset Integration for GlyphType
2. Element Tags on existing glyphs

### Phase 2
1. HexBookType assets
2. Casting modifier system

### Phase 3
1. HexStaffType assets
2. Visual customization

### Phase 4
1. Glyph discovery
2. Spell combos
3. PvP balance

---

## Testing Requirements

### Unit Tests
- [ ] GlyphType asset loading and validation
- [ ] HexBookType inheritance resolution
- [ ] Casting modifier calculations
- [ ] Element tag matching

### Integration Tests
- [ ] Third-party book type registration
- [ ] Staff type auto-detection
- [ ] Modifier stacking across book+staff
- [ ] Hot-reload of glyph assets

### Manual Testing
1. Create custom book type via JSON, verify auto-detection
2. Create custom staff type, verify casting modifiers apply
3. Test element tag matching with various glyph combinations
4. Verify visual colors propagate to particles/effects
