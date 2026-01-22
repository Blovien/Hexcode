# Hexcode Glyph System Rework - Implementation Guide

This document provides comprehensive instructions for an agent to implement the complete rework of the Hexcode glyph system. Follow each phase in order. Each task has clear completion criteria.

---

## Overview

The goal is to transform the current hard-coded glyph system into a fully modular, asset-driven architecture where:

1. **All glyphs are equal** - Whether defined by this plugin or external plugins, all glyphs use the same registration and execution path
2. **Asset-driven configuration** - Glyph properties (visuals, costs, power, variability) are defined in asset files, not code
3. **Context-based execution** - Glyphs receive and return a Context object that carries execution state
4. **Per-player glyph data** - Drawing accuracy and speed are stored per-player for persistence
5. **Hex/Chain composition** - Clear separation between nested Hexes (influence each other) and Chains (independent execution)

---

## Key Concepts

### Hex vs Chain

```
HEX (Nested - glyphs influence each other):
  Beam[Charge[Fire[]]]
  - Beam wraps Charge wraps Fire
  - Context flows: Beam → Charge → Fire
  - Each glyph can modify the context for its children

CHAIN (Sequential - glyphs execute independently):
  Fire[]:Ice[]:Heal[]
  - Fire, Ice, Heal execute one after another
  - Each receives the ORIGINAL context (not modified by siblings)
  - Syntax uses ":" to separate chain elements

COMBINED:
  Beam[Charge[Fire[]]]:Beam[Blink[]]
  - Two hex chains: first is Beam[Charge[Fire[]]], second is Beam[Blink[]]
  - Depth-first: complete first hex entirely, then start second hex
  - Second Beam receives original context, NOT the context after Fire executed
```

### Context Object

The Context object is the central data structure passed between glyphs during execution:

```java
SpellContext {
    // Immutable base info
    UUID casterId;
    EntityRef caster;
    Vector3d castOrigin;
    Vector3d castDirection;
    int castNumber;           // How many times this spell has been cast (1, 2, 3...)

    // Mutable execution state
    List<EntityRef> targets;
    List<Vector3d> targetPositions;
    float powerMultiplier;    // Accumulated power (default 1.0)
    float rangeMultiplier;    // Accumulated range (default 1.0)
    float durationMultiplier; // Accumulated duration (default 1.0)

    // Execution history
    List<GlyphExecutionRecord> executedGlyphs;  // In order of execution
    Map<String, Integer> glyphExecutionCounts;  // How many times each glyph ID executed

    // Extensible metadata
    Map<String, Object> metadata;  // Glyphs can add custom data
}
```

### Power Decay Rules

1. **Cast Decay**: Each subsequent cast of the same spell is weaker
   - Formula: `effectivePower = basePower * (1.0 / castNumber)`
   - Cast 1: 100%, Cast 2: 50%, Cast 3: 33%, etc.

2. **Glyph Repetition Decay**: If a glyph executes multiple times in one spell, subsequent executions are weaker
   - Formula: `effectivePower = basePower * (1.0 / executionCount)`
   - First Fire[]: 100%, Second Fire[]: 50%, etc.

---

## Phase 1: Asset System Foundation

**Goal**: Create the asset file format and loading infrastructure for glyph definitions.

### 1.1 - Define Glyph Asset Schema

**File to create**: `src/main/java/com/riprod/hexcode/asset/GlyphAssetDefinition.java`

**Completion Criteria**:
- [ ] Class represents a single glyph's asset-defined properties
- [ ] Fields include:
  ```java
  String id;                    // e.g., "hexcode:fire"
  String displayName;           // e.g., "Fire"
  String role;                  // "EFFECT", "MODIFIER", or "SELECT"

  // Visual assets
  String modelPath;             // Path to Blockbench .blockymodel file (3D shape)
  String drawingTemplatePath;   // Path to PNG used for drawing comparison

  // Base stats (all default to 1.0 if not specified)
  float basePower;              // Multiplier for effect strength (default 1.0)
  float baseManaCost;           // Mana cost before modifiers (default 1.0)
  float baseVariability;        // Drawing tolerance 0.0-1.0 (default 0.5)

  // Role-specific properties
  Map<String, Object> properties;  // Custom properties per glyph type
  ```
- [ ] Includes builder pattern for construction
- [ ] Has `validate()` method that checks required fields based on role and returns the expected mana cost
- [ ] Static factory method: `fromJson(JsonObject json)` for parsing

**Asset File Location**: `Assets/Server/Hexcode/Glyphs/{glyphId}.json`

**Example Asset File** (`Assets/Server/Hexcode/Glyphs/fire.json`):
```json
{
  "id": "hexcode:fire",
  "displayName": "Fire",
  "role": "EFFECT",
  "modelPath": "Hexcode/Models/Glyphs/fire.blockymodel",
  "drawingTemplatePath": "Hexcode/Drawings/fire.png",
  "basePower": 0.25,
  "baseManaCost": 15,
  "baseVariability": 0.6,
  "properties": {
    "damageType": "fire",
    "baseDamage": 10.0,
    "burnDuration": 3.0,
    "burnEffectId": "hexcode:burn"
  }
}
```

### 1.2 - Create Glyph Asset Loader

**File to create**: `src/main/java/com/riprod/hexcode/asset/GlyphAssetLoader.java`

**Completion Criteria**:
- [ ] Singleton pattern with `getInstance()`
- [ ] Method: `loadGlyphAsset(String glyphId)` - loads single glyph asset file
- [ ] Method: `loadAllGlyphAssets()` - scans `Assets/Server/Hexcode/Glyphs/` directory
- [ ] Method: `reloadAssets()` - hot-reload for development
- [ ] Uses Hytale's `AssetRegistry` for file access (see `com.hypixel.hytale.server.api.assets.AssetRegistry`)
- [ ] Parses JSON using Hytale's codec system
- [ ] Logs warnings for invalid/missing assets but continues loading others
- [ ] Returns `Map<String, GlyphAssetDefinition>` of loaded assets

**Error Handling**:
- Missing required field → log error, skip glyph
- Invalid role → log error, skip glyph
- Missing referenced files (model, drawing) → log warning, continue with defaults

### 1.3 - Create Drawing Template System

**File to create**: `src/main/java/com/riprod/hexcode/drawing/DrawingTemplate.java`

**Completion Criteria**:
- [ ] Stores the PNG data for drawing comparison
- [ ] Fields:
  ```java
  String glyphId;
  int width;
  int height;
  boolean[][] shapeData;  // true = part of shape, false = background
  float variability;      // Tolerance for matching
  ```
- [ ] Method: `loadFromPng(String path)` - loads PNG and converts to boolean array
- [ ] Method: `getShapePoints()` - returns list of (x,y) coordinates that are part of shape
- [ ] Method: `getNormalizedPoints()` - returns points normalized to 0.0-1.0 range
- [ ] Static cache: `Map<String, DrawingTemplate>` to avoid reloading

**PNG Format Requirements**:
- Black & white PNG (white = shape, black/transparent = background)
- Recommended size: 128x128 pixels
- Shape should be centered and fill ~80% of canvas

---

## Phase 2: Core Glyph Interface Rework

**Goal**: Redesign the Glyph interface to support the new modular architecture.

### 2.1 - Create New Glyph Interface

**File to modify**: `src/main/java/com/riprod/hexcode/glyph/Glyph.java`

**Completion Criteria**:
- [ ] Remove all default implementations (make truly interface-based)
- [ ] New method signatures:
  ```java
  public interface Glyph {
      // Identity
      String getId();
      String getDisplayName();
      GlyphRole getRole();

      // Asset-driven properties (loaded from asset file)
      GlyphAssetDefinition getAssetDefinition();

      // Registration (called once when glyph is registered, should return metadata about being registered)
      RegisterObject onRegister(GlyphRegistry registry);

      // Execution (called each time glyph executes)
      SpellContext cast(SpellContext context);

      // Per-execution data
      float getAccuracy();        // 0.0-1.0, set by drawing system
      float getDrawSpeed();       // Seconds taken to draw
      void setExecutionData(float accuracy, float drawSpeed);

      // Mana calculation
      float calculateManaCost(SpellContext context);
  }
  ```
- [ ] Document each method with Javadoc explaining when it's called and what it should do

### 2.2 - Create SpellContext Class

**File to create**: `src/main/java/com/riprod/hexcode/execution/SpellContext.java`

**Completion Criteria**:
- [ ] Immutable builder pattern for base properties
- [ ] Mutable state with copy-on-modify for chain isolation
- [ ] Fields as defined in Key Concepts above
- [ ] Methods:
  ```java
  // Factory methods
  static SpellContext create(EntityRef caster, Vector3d origin, Vector3d direction, int castNumber);
  SpellContext copy();  // Deep copy for chain isolation

  // Target management
  void addTarget(EntityRef entity);
  void addTargetPosition(Vector3d position);
  void setTargets(List<EntityRef> entities);
  void clearTargets();
  List<EntityRef> getTargets();
  List<Vector3d> getTargetPositions();
  boolean hasTargets();

  // Multiplier management
  void multiplyPower(float factor);
  void multiplyRange(float factor);
  void multiplyDuration(float factor);
  float getEffectivePower();
  float getEffectiveRange();
  float getEffectiveDuration();

  // Execution tracking
  void recordGlyphExecution(Glyph glyph);
  int getGlyphExecutionCount(String glyphId);
  List<GlyphExecutionRecord> getExecutionHistory();

  // Metadata
  void setMetadata(String key, Object value);
  <T> T getMetadata(String key, Class<T> type);
  boolean hasMetadata(String key);

  // Power decay calculation
  float calculateDecayedPower(Glyph glyph);
  ```
- [ ] `GlyphExecutionRecord` inner class with: glyphId, timestamp, accuracy, resultingTargetCount

### 2.3 - Create Abstract Base Classes

**Files to modify**:
- `src/main/java/com/riprod/hexcode/glyph/effects/EffectGlyph.java`
- `src/main/java/com/riprod/hexcode/glyph/modifiers/ModifierGlyph.java`
- `src/main/java/com/riprod/hexcode/glyph/selects/SelectGlyph.java`

**Completion Criteria for EffectGlyph**:
- [ ] Constructor takes only `GlyphAssetDefinition`
- [ ] Implements `cast()` method template:
  ```java
  // Implementation may vary depending on the glyph
  @Override
  public final SpellContext cast(SpellContext context) { // actual implementation may vary depending on the spell
      // Record this execution
      context.recordGlyphExecution(this);

      // Calculate effective power with decay
      float effectivePower = context.calculateDecayedPower(this);

      // Apply effect to all targets (if applicable)
      for (EntityRef target : context.getTargets()) {
          applyEffect(context, target, effectivePower);
      }
      // Apple an efffect at all locations (if applicable)
      for (Vector3d position : context.getTargetPositions()) {
          applyEffectAtPosition(context, position, effectivePower);
      }

      return context;
  }
  ```
- [ ] Abstract methods for subclasses:
  ```java
  protected abstract void applyEffect(SpellContext context, EntityRef target, float power);
  protected abstract void applyEffectAtPosition(SpellContext context, Vector3d position, float power);
  ```
- [ ] Helper methods to read properties from asset definition:
  ```java
  protected float getProperty(String key, float defaultValue);
  protected String getProperty(String key, String defaultValue);
  protected int getProperty(String key, int defaultValue);
  ```

**Completion Criteria for ModifierGlyph types**:
- [ ] Constructor takes only `GlyphAssetDefinition`
- [ ] Implements `cast()` to apply multiplier and return context:
  ```java
  @Override
  public SpellContext cast(SpellContext context) {
      context.recordGlyphExecution(this);
      applyModifier(context);
      return context;
  }

  protected abstract void applyModifier(SpellContext context);
  ```
- [ ] Standard implementations read multiplier from asset:
  ```java
  // In PowerModifierGlyph:
  protected void applyModifier(SpellContext context) {
      float multiplier = getAssetDefinition().getBasePower();
      context.multiplyPower(multiplier);
  }
  ```

**Completion Criteria for SelectGlyph types**:
- [ ] Constructor takes only `GlyphAssetDefinition`
- [ ] New field: `boolean isDelayed` (read from asset properties)
- [ ] Implements `cast()` for target selection:
  ```java
  @Override
  public SpellContext cast(SpellContext context) {
      context.recordGlyphExecution(this);
      selectTargets(context);
      return context;
  }

  protected abstract void selectTargets(SpellContext context);
  ```
- [ ] For delayed selects (BEAM, PROJECTILE), `selectTargets()` spawns projectile and wait until hit before returning

---

## Phase 3: Glyph Registry Rework

**Goal**: Create a robust registry that serves as the single source of truth for all glyphs.

### 3.1 - Redesign GlyphRegistry

**File to modify**: `src/main/java/com/riprod/hexcode/glyph/GlyphRegistry.java`

**Completion Criteria**:
- [ ] Remove hard-coded `registerDefaultGlyphs()` method
- [ ] New architecture:
  ```java
  public class GlyphRegistry {
      private static GlyphRegistry instance;

      // Storage
      private final Map<String, Glyph> glyphsById;
      private final Map<GlyphRole, List<Glyph>> glyphsByRole;
      private final Map<String, GlyphAssetDefinition> assetDefinitions;

      // Registration state
      private boolean frozen = false;  // Prevents registration after initialization

      // Methods
      public static GlyphRegistry getInstance();

      // Registration (only before frozen)
      public void registerGlyph(Glyph glyph);
      public void registerGlyphFromAsset(GlyphAssetDefinition asset, GlyphFactory factory);
      public void freeze();  // Called after all glyphs registered

      // Lookup (thread-safe)
      public Glyph getGlyph(String id);
      public Optional<Glyph> findGlyph(String id);
      public List<Glyph> getGlyphsByRole(GlyphRole role);
      public List<Glyph> getAllGlyphs();
      public boolean hasGlyph(String id);

      // Asset definitions
      public GlyphAssetDefinition getAssetDefinition(String id);

      // Validation
      public boolean areCompatible(Glyph modifier, Glyph target);
      public List<String> validateHex(HexNode root);  // Returns list of errors
  }
  ```
- [ ] `GlyphFactory` functional interface:
  ```java
  @FunctionalInterface
  public interface GlyphFactory {
      Glyph create(GlyphAssetDefinition asset);
  }
  ```
- [ ] Registration validates:
  - ID is unique
  - ID follows namespace format (e.g., "hexcode:fire", "myplugin:custom")
  - Asset definition exists and is valid
  - Glyph implements correct interface for its role

### 3.2 - Create Glyph Registration Event

**File to create**: `src/main/java/com/riprod/hexcode/event/GlyphRegistrationEvent.java`

**Completion Criteria**:
- [ ] Fired during Hexcode initialization before registry is frozen
- [ ] Provides access to registry for external plugins:
  ```java
  public class GlyphRegistrationEvent {
      private final GlyphRegistry registry;
      private final GlyphAssetLoader assetLoader;

      public void registerGlyph(Glyph glyph);
      public void registerGlyphWithAsset(String assetPath, GlyphFactory factory);
      public GlyphAssetDefinition loadAsset(String path);
  }
  ```
- [ ] External plugins subscribe and register their glyphs during this event

### 3.3 - Create Built-in Glyph Factories

**File to create**: `src/main/java/com/riprod/hexcode/glyph/GlyphFactories.java`

**Completion Criteria**:
- [ ] Factory methods for each built-in glyph type:
  ```java
  public class GlyphFactories {
      // Effects
      public static final GlyphFactory FIRE = asset -> new FireGlyph(asset);
      public static final GlyphFactory ICE = asset -> new IceGlyph(asset);
      public static final GlyphFactory LIGHTNING = asset -> new LightningGlyph(asset);
      // ... etc for all 10 effects

      // Modifiers
      public static final GlyphFactory POWER = asset -> new PowerGlyph(asset);
      public static final GlyphFactory RANGE = asset -> new RangeGlyph(asset);
      // ... etc for all 5 modifiers

      // Selects
      public static final GlyphFactory SELF = asset -> new SelfGlyph(asset);
      public static final GlyphFactory BEAM = asset -> new BeamGlyph(asset);
      // ... etc for all 7 selects

      // Lookup by ID
      public static GlyphFactory getFactory(String glyphId);
  }
  ```
- [ ] Registration maps glyph IDs to their factories

### 3.4 - Update Hexcode Initialization

**File to modify**: `src/main/java/com/riprod/hexcode/Hexcode.java`

**Completion Criteria**:
- [ ] In `onEnable()`:
  1. Load all glyph asset definitions via `GlyphAssetLoader`
  2. Fire `GlyphRegistrationEvent` (allows external plugins to register)
  3. Register built-in glyphs using factories + assets
  4. Freeze registry
  5. Validate all compatibility references
- [ ] Log summary: "Registered {N} glyphs ({X} effects, {Y} modifiers, {Z} selects)"

---

## Phase 4: Hex/Chain Execution System

**Goal**: Implement the new execution model with proper context passing for Hexes vs Chains.

### 4.1 - Redesign Hex Structure

**File to modify**: `src/main/java/com/riprod/hexcode/hex/Spell.java`

**Completion Criteria**:
- [ ] New structure to support both nesting and chaining (vary as needed depending on actual implementation):
  ```java
  public class Spell { // Top-level object that holds the current spell
      private final List<HexNode> chain;  // Top-level chain of hexes

      public Spell(HexNode singleHex);
      public Spell(List<HexNode> chain);

      public List<HexNode> getChain();
      public boolean isChained();  // Returns true if chain.size() > 1
      public int getChainLength();

      // Iteration
      public Iterator<HexNode> chainIterator();
  }
  ```
- [ ] String representation: `Beam[Fire[]]:Ice[]` shows chain with `:` separator

### 4.2 - Redesign HexNode Structure

**File to modify**: `src/main/java/com/riprod/hexcode/hex/HexNode.java`

**Completion Criteria**:
- [ ] Simplified structure focused on nesting (vary as needed depending on actual implementation):
  ```java
  public class HexNode {
      private final Glyph glyph;
      private final List<HexNode> children;  // Nested glyphs (hex relationship)

      // Factory methods
      public static HexNode leaf(Glyph glyph);
      public static HexNode wrap(Glyph glyph, HexNode child);
      public static HexNode wrap(Glyph glyph, List<HexNode> children);

      // Traversal
      public boolean isLeaf();
      public List<HexNode> getChildren();
      public void forEachDepthFirst(Consumer<HexNode> action);
  }
  ```
- [ ] Children represent nested glyphs (inside the hex), NOT chain siblings

### 4.3 - Create New HexExecutor

**File to modify**: `src/main/java/com/riprod/hexcode/execution/HexExecutor.java`

**Completion Criteria**:
- [ ] Complete rewrite with new execution model (vary as needed depending on actual implementation):
  ```java
  // Ensure to account for nested chains like Glyph1[Glyph2[Glyph3[]:Glyph4[]]:Glyph4[]]:Glyph5[]
  public class HexExecutor {
      public void execute(Hex hex, EntityRef caster, int castNumber) {
          Vector3d origin = getCasterPosition(caster);
          Vector3d direction = getCasterLookDirection(caster);

          // Create base context
          SpellContext baseContext = SpellContext.create(caster, origin, direction, castNumber);

          // Execute each hex in chain with COPY of base context
          for (HexNode hexNode : hex.getChain()) {
              SpellContext chainContext = baseContext.copy();  // Fresh copy for each chain element
              executeHexNode(hexNode, chainContext);
          }
      }

      private SpellContext executeHexNode(HexNode node, SpellContext context) {
          Glyph glyph = node.getGlyph();

          // Execute this glyph (modifies context)
          context = glyph.cast(context);

          // Execute children depth-first (context flows down)
          for (HexNode child : node.getChildren()) {
              // this may have to change to account for nested chains
              context = executeHexNode(child, context);
          }

          return context;
      }
  }
  ```
- [ ] Key behavior:
  - Chain elements get COPY of original context (isolated)
  - Nested children get SAME context (flows through)
  - Depth-first: complete entire hex before moving to next chain element

### 4.4 - Update Mana Calculation

**File to modify**: `src/main/java/com/riprod/hexcode/execution/HexExecutor.java` (add method)

**Completion Criteria**:
- [ ] Method: `float calculateTotalManaCost(Hex hex, SpellContext context)`
- [ ] Algorithm:
  ```java
  float totalCost = 0;

  for (HexNode hexNode : hex.getChain()) {
      totalCost += calculateNodeCost(hexNode, context);
  }

  return totalCost;

  // Helper:
  float calculateNodeCost(HexNode node, SpellContext context) {
      Glyph glyph = node.getGlyph();
      float cost = glyph.calculateManaCost(context);

      for (HexNode child : node.getChildren()) {
          cost += calculateNodeCost(child, context);
      }

      return cost;
  }
  ```
- [ ] Each glyph's `calculateManaCost()` uses its asset-defined `baseManaCost`

---

## Phase 5: Per-Player Glyph Data

**Goal**: Store accuracy and speed data for each player's drawn glyphs.

### 5.1 - Create PlayerGlyphData

**File to create**: `src/main/java/com/riprod/hexcode/data/PlayerGlyphData.java`

**Completion Criteria**:
- [ ] Stores per-glyph data for a single player. This also defined glyphs known. Players can only create spells out of known glyphs:
  ```java
  public class PlayerGlyphData {
      private final UUID playerId;
      private final Map<String, GlyphInstanceData> glyphInstances;

      // Get/set instance data
      public GlyphInstanceData getGlyphData(String glyphId);
      public void setGlyphData(String glyphId, GlyphInstanceData data);
      public boolean hasGlyphData(String glyphId);

      // Convenience methods
      public float getAccuracy(String glyphId);  // Returns 0 if not drawn
      public float getDrawSpeed(String glyphId); // Returns 0 if not drawn

      // Serialization
      public JsonObject toJson();
      public static PlayerGlyphData fromJson(UUID playerId, JsonObject json);
  }
  ```

### 5.2 - Create GlyphInstanceData

**File to create**: `src/main/java/com/riprod/hexcode/data/GlyphInstanceData.java`

**Completion Criteria**:
- [ ] Immutable data class:
  ```java
  public class GlyphInstanceData {
      private final String baseGlyphId;
      private final float accuracy;          // 0.0-1.0
      private final float drawSpeed;         // Seconds to draw
      private final long drawnTimestamp;     // When drawn
      private final int timesUsed;           // How many times cast with this instance

      // Builder for updates
      public GlyphInstanceData withIncrementedUseCount();
      public GlyphInstanceData withNewDrawing(float accuracy, float drawSpeed);

      // Serialization
      public JsonObject toJson();
      public static GlyphInstanceData fromJson(JsonObject json);
  }
  ```

### 5.3 - Create PlayerGlyphDataManager

**File to create**: `src/main/java/com/riprod/hexcode/data/PlayerGlyphDataManager.java`

**Completion Criteria**:
- [ ] Singleton manager:
  ```java
  public class PlayerGlyphDataManager {
      private static PlayerGlyphDataManager instance;
      private final Map<UUID, PlayerGlyphData> loadedData;

      // Access
      public PlayerGlyphData getPlayerData(UUID playerId);
      public PlayerGlyphData getOrCreatePlayerData(UUID playerId);

      // Persistence
      public void loadPlayerData(UUID playerId);
      public void savePlayerData(UUID playerId);
      public void saveAllPlayerData();

      // Events
      public void onPlayerJoin(UUID playerId);
      public void onPlayerLeave(UUID playerId);
  }
  ```
- [ ] Data stored in: `{universe}/players/{uuid}/hexcode.json`
- [ ] Lazy loading: data loaded when first accessed or on player join
- [ ] Auto-save: data saved on player leave and periodically

### 5.4 - Integrate with Glyph Execution

**File to modify**: `src/main/java/com/riprod/hexcode/execution/HexExecutor.java`

**Completion Criteria**:
- [ ] Before executing each glyph, load player's glyph data:
  ```java
  private void prepareGlyphForExecution(Glyph glyph, SpellContext context) {
      UUID casterId = context.getCasterId();
      PlayerGlyphData playerData = PlayerGlyphDataManager.getInstance()
          .getPlayerData(casterId);

      if (playerData != null && playerData.hasGlyphData(glyph.getId())) {
          GlyphInstanceData instanceData = playerData.getGlyphData(glyph.getId());
          glyph.setExecutionData(instanceData.getAccuracy(), instanceData.getDrawSpeed());
      } else {
          // No drawing data - use defaults (starter glyphs)
          glyph.setExecutionData(0.75f, 0f);
      }
  }
  ```

---

## Phase 6: Delayed Execution (BEAM/PROJECTILE)

**Goal**: Properly handle delayed glyphs that have travel time.

### 6.1 - Redesign DelayedExecutionManager

**File to modify**: `src/main/java/com/riprod/hexcode/execution/DelayedExecutionManager.java`

**Completion Criteria**:
- [ ] Track pending executions with context:
  ```java
  public class DelayedExecutionManager { // Delayed glyphs should be able to self-determine if they are blocking or non-blocking
      private final Map<UUID, DelayedExecutionState> pendingExecutions;

      public UUID queueDelayedExecution(HexNode node, SpellContext context,
                                         Vector3d startPos, Vector3d direction);

      public void onProjectileHit(UUID executionId, EntityRef hitEntity, Vector3d hitPos);
      public void onProjectileMiss(UUID executionId, Vector3d finalPos);
      public void onProjectileTimeout(UUID executionId);

      public void update(float deltaTime);  // Called each tick
  }
  ```
- [ ] `DelayedExecutionState` stores:
  - Original context (copy at time of delay)
  - Remaining HexNode children to execute on hit
  - Remaining chain elements to execute after resolution

### 6.2 - Update Select Glyphs for Delayed Execution

**Files to modify**:
- `src/main/java/com/riprod/hexcode/glyph/selects/BeamGlyph.java`
- `src/main/java/com/riprod/hexcode/glyph/selects/ProjectileGlyph.java`

**Completion Criteria**:
- [ ] `cast()` method queues delayed execution instead of immediate (rework to include blocking/nonblocking functionality. Context may include a "dontBlock" object that'll make it asynrchronous - or it will be false and this glyph should block further execution until completion):
  ```java
  @Override
  public SpellContext cast(SpellContext context) {
      context.recordGlyphExecution(this);

      // Queue for delayed execution
      UUID executionId = DelayedExecutionManager.getInstance()
          .queueDelayedExecution(getCurrentNode(), context,
                                  context.getCastOrigin(),
                                  context.getCastDirection());

      // Mark context as having pending delay
      context.setMetadata("pendingDelayId", executionId);

      return context;
  }
  ```
- [ ] Children always execute when projectile hits, not immediately

---

## Phase 7: Update All Glyph Implementations

**Goal**: Update each glyph to use the new asset-driven, context-based architecture.

### 7.1 - Update Effect Glyphs

**Files to modify** (all in `src/main/java/com/riprod/hexcode/glyph/effects/`):
- `FireGlyph.java`
- `IceGlyph.java`
- `LightningGlyph.java`
- `EarthGlyph.java`
- `VoidGlyph.java`
- `LightGlyph.java`
- `ShieldGlyph.java`
- `BlinkGlyph.java`
- `HealGlyph.java`
- `PushGlyph.java`

**Completion Criteria for each**:
- [ ] Constructor takes only `GlyphAssetDefinition`
- [ ] Remove all hard-coded constants (BASE_DAMAGE, etc.)
- [ ] Read all values from `getAssetDefinition().getProperties()`
- [ ] Use `context.calculateDecayedPower(this)` for power scaling
- [ ] Implement both `applyEffect(context, target, power)` and `applyEffectAtPosition(context, pos, power)`

**Example - FireGlyph transformation**:
```java
// OLD:
private static final float BASE_DAMAGE = 10.0f;
private static final float BURN_DURATION = 3.0f;

// NEW:
protected void applyEffect(SpellContext context, EntityRef target, float power) {
    float baseDamage = getProperty("baseDamage", 10.0f);
    float burnDuration = getProperty("burnDuration", 3.0f);

    float actualDamage = baseDamage * power * context.getEffectivePower();
    float actualDuration = burnDuration * context.getEffectiveDuration();

    // Apply damage and burn...
}
```

### 7.2 - Update Modifier Glyphs

**Files to modify** (all in `src/main/java/com/riprod/hexcode/glyph/modifiers/`):
- `PowerGlyph.java`
- `RangeGlyph.java`
- `DurationGlyph.java`
- `SpeedGlyph.java` 

**Completion Criteria for each**:
- [ ] Constructor takes only `GlyphAssetDefinition`
- [ ] `applyModifier()` reads multiplier from asset and modifies context
- [ ] No hard-coded multiplier values

### 7.3 - Update Select Glyphs

**Files to modify** (all in `src/main/java/com/riprod/hexcode/glyph/selects/`):
- `SelfGlyph.java`
- `TouchGlyph.java`
- `GazeGlyph.java`
- `BeamGlyph.java`
- `ProjectileGlyph.java`
- `BurstGlyph.java`
- `ConeGlyph.java`

**Completion Criteria for each**:
- [ ] Constructor takes only `GlyphAssetDefinition`
- [ ] Read range, speed, etc. from asset properties
- [ ] Apply context multipliers (range, speed) to base values
- [ ] Delayed selects (BEAM, PROJECTILE) use `DelayedExecutionManager` (this may need to be reworked, skip or fix this depending on implementation)

---

## Phase 8: Asset Files Creation

**Goal**: Create all asset definition files for built-in glyphs.

### 8.1 - Create Effect Glyph Assets

**Directory**: `Assets/Server/Hexcode/Glyphs/`

**Files to create** (one JSON per glyph):
- [ ] `fire.json`
- [ ] `ice.json`
- [ ] `lightning.json`
- [ ] `earth.json`
- [ ] `void.json`
- [ ] `light.json`
- [ ] `shield.json`
- [ ] `blink.json`
- [ ] `heal.json`
- [ ] `push.json`

**Template for effect assets**:
```json
{
  "id": "hexcode:{name}",
  "displayName": "{Name}",
  "role": "EFFECT",
  "modelPath": "Hexcode/Models/Glyphs/{name}.blockymodel",
  "drawingTemplatePath": "Hexcode/Drawings/{name}.png",
  "basePower": 1.0,
  "baseManaCost": 15,
  "baseVariability": 0.5,
  "properties": {
    // Effect-specific properties
  },
  "compatibleModifiers": ["hexcode:power", "hexcode:duration"]
}
```

### 8.2 - Create Modifier Glyph Assets

**Directory**: `Assets/Server/Hexcode/Glyphs/`

**Files to create**:
- [ ] `power.json`
- [ ] `range.json`
- [ ] `duration.json`
- [ ] `speed.json`
- [ ] `split.json`

### 8.3 - Create Select Glyph Assets

**Directory**: `Assets/Server/Hexcode/Glyphs/`

**Files to create**:
- [ ] `self.json`
- [ ] `touch.json`
- [ ] `gaze.json`
- [ ] `beam.json`
- [ ] `projectile.json`
- [ ] `burst.json`
- [ ] `cone.json`

### 8.4 - Create Drawing Template PNGs

**Directory**: `Assets/Common/Hexcode/Drawings/`

**Note**: This is out of scope for code implementation but must be planned for.

**Files needed** (128x128 black & white PNGs):
- [ ] `fire.png` - Flame shape
- [ ] `ice.png` - Snowflake/crystal shape
- [ ] `lightning.png` - Bolt shape
- [ ] `earth.png` - Rock/mountain shape
- [ ] `void.png` - Spiral/void shape
- [ ] `light.png` - Sun/star shape
- [ ] `shield.png` - Shield shape
- [ ] `blink.png` - Arrow/teleport shape
- [ ] `heal.png` - Heart/cross shape
- [ ] `push.png` - Wind/wave shape
- [ ] `power.png` - Plus/amplify shape
- [ ] `range.png` - Expanding circles shape
- [ ] `duration.png` - Hourglass/clock shape
- [ ] `speed.png` - Lightning bolt shape
- [ ] `split.png` - Fork/branch shape
- [ ] `self.png` - Circle/dot shape
- [ ] `touch.png` - Hand shape
- [ ] `gaze.png` - Eye shape
- [ ] `beam.png` - Line/ray shape
- [ ] `projectile.png` - Arrow shape
- [ ] `burst.png` - Explosion shape
- [ ] `cone.png` - Triangle/cone shape

---

## Phase 9: Testing & Validation

**Goal**: Ensure the reworked system functions correctly.

### 9.1 - Debug Commands

**File to modify**: `src/main/java/com/riprod/hexcode/command/HexcodeCommand.java`

**New commands**:
(use /skills for how to make sub-commands)
- [ ] `/hexcode reload` - Hot-reload all glyph assets
- [ ] `/hexcode context` - Show current spell context state
- [ ] `/hexcode decay` - Show decay calculations for current spell
- [ ] `/hexcode assets` - List all loaded glyph assets
- [ ] `/hexcode learn` - Learn all spells
- [ ] `/hexcode learn <id>` - Learn specific spell
- [ ] `/hexcode forget` - Forget all spells
- [ ] `/hexcode forget <id>` - Forget specific spell 


---

## Phase 10: Documentation & Cleanup

**Goal**: Document the new system and remove deprecated code.

### 10.1 - Update CLAUDE.md

**File to modify**: `CLAUDE.md`

**Updates**:
- [ ] Update glyph system description to reflect asset-driven approach
- [ ] Document context flow (Hex vs Chain)
- [ ] Add power decay rules
- [ ] Update package structure diagram

### 10.2 - Remove Deprecated Code

**Files to potentially remove/refactor**:
- [ ] Old `ExecutionContext.java` (replaced by `SpellContext.java`)
- [ ] Hard-coded constants in glyph classes
- [ ] `registerDefaultGlyphs()` method

### 10.3 - Skill Creation
- [ ] Add skill in ./.claude/skills/SKILLNAME/SKILL.md for each topic learned

---

## Example Walkthrough

To verify understanding, here's the complete execution flow for the user's example:

**Spell**: `Beam[Charge[Fire[]]]:Beam[Blink[]]`

**Structure**:
```
Chain[0]: Beam[Charge[Fire[]]]
Chain[1]: Beam[Blink[]]
```

**Execution**:

1. **Create base context**:
   ```
   SpellContext {
     caster: Player,
     castNumber: 1,
     powerMultiplier: 1.0,
     targets: [],
     executedGlyphs: [],
     glyphExecutionCounts: {}
   }
   ```

2. **Execute Chain[0] with context copy**:
   - `Beam.cast(context)`:
     - Records execution: `glyphExecutionCounts["hexcode:beam"] = 1`
     - Spawns beam projectile → queues delayed execution
     - Beam travels and hits Enemy
   - `DelayedExecutionManager.onProjectileHit()`:
     - Sets `targets: [Enemy]`
     - Continues with children
   - `Charge.cast(context)`:
     - Records execution: `glyphExecutionCounts["hexcode:charge"] = 1`
     - `powerMultiplier = 2.0` (or whatever Charge does)
   - `Fire.cast(context)`:
     - Records execution: `glyphExecutionCounts["hexcode:fire"] = 1`
     - Calculates: `effectivePower = basePower(0.25) * contextPower(2.0) * decay(1.0) = 0.5`
     - Applies fire damage to Enemy with power 0.5

3. **Execute Chain[1] with FRESH context copy**:
   - `Beam.cast(context)`:
     - Records execution: `glyphExecutionCounts["hexcode:beam"] = 1` (fresh copy!)
     - BUT: global cast tracking shows beam executed before
     - Decay: `1.0 / 2 = 0.5` (second beam in spell)
     - Spawns beam with reduced range/power
     - Beam travels and misses, hits air block
   - `DelayedExecutionManager.onProjectileMiss()`:
     - Sets `targetPositions: [airBlockPos]`
   - `Blink.cast(context)`:
     - Records execution: `glyphExecutionCounts["hexcode:blink"] = 1`
     - Sees target is a position (not entity)
     - Teleports caster to that position

4. **Spell complete**

**Next cast (castNumber: 2)**:
- All glyphs have `castDecay = 1.0 / 2 = 0.5`
- Fire deals 50% of first cast's damage
- Beam travels 50% as far

---

## Summary Checklist

| Phase | Description | Status |
|-------|-------------|--------|
| 1 | Asset System Foundation | Not Started |
| 2 | Core Glyph Interface Rework | Not Started |
| 3 | Glyph Registry Rework | Not Started |
| 4 | Hex/Chain Execution System | Not Started |
| 5 | Per-Player Glyph Data | Not Started |
| 6 | Delayed Execution | Not Started |
| 7 | Update All Glyph Implementations | Not Started |
| 8 | Asset Files Creation | Not Started |
| 9 | Testing & Validation | Not Started |
| 10 | Documentation & Cleanup | Not Started |

---

## Important Notes for Implementing Agent

1. **Do NOT start coding until you understand the context flow** - The distinction between Hex (nested, context flows through) and Chain (sequential, context isolated) is critical.

2. **Asset-driven means NO hard-coded values** - Every numeric constant (damage, range, cost) must come from the asset JSON file.

3. **Context is mutable within a hex, immutable across chains** - When a chain element starts, it gets a COPY of the original context.

4. **Power decay is multiplicative** - Both cast decay and glyph repetition decay apply.

5. **Drawing system is out of scope** - The `accuracy` and `drawSpeed` fields will be populated by a future drawing system. For now, use defaults (0.75 accuracy, 0 speed).

6. **Test incrementally** - After each phase, verify the system still compiles and basic functionality works.

7. **Preserve existing functionality where possible** - The orbital ring, crafting space, and visual systems should continue to work with the new glyph architecture.
