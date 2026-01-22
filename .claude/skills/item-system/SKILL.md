# Hytale Item System

## Overview
The Item System defines all holdable objects including weapons, tools, armor, and consumables. Items are defined as JSON assets with support for template inheritance, interactions, durability, and recipes.

## Package Location
- Item: `com.hypixel.hytale.server.core.asset.type.item.config.Item`
- ItemStack: `com.hypixel.hytale.server.core.inventory.ItemStack`

## Asset Locations
- Server definitions: `Assets/Server/Item/Items/`
- Templates: `Assets/Server/Item/Items/[Category]/Template_*.json`
- Interactions: `Assets/Server/Item/Interactions/`
- Root interactions: `Assets/Server/Item/RootInteractions/`
- Models: `Assets/Common/Items/`
- Icons: `Assets/Common/Icons/ItemsGenerated/`

## Item Categories

| Category | Examples |
|----------|----------|
| Weapon | Swords, bows, staves, daggers |
| Tool | Pickaxes, shovels, hoes |
| Armor | Helmets, chestplates, boots |
| Consumable | Food, potions |
| Container | Chests, bags |
| Ingredient | Bars, leather, fabric |
| Furniture | Chairs, tables |
| Misc | Various other items |

## Basic Item JSON

```json
{
  "TranslationProperties": {
    "Name": "server.items.MyPlugin_Custom_Item.name"
  },
  "Model": "Items/Misc/Custom_Item.blockymodel",
  "Texture": "Items/Misc/Custom_Texture.png",
  "Icon": "Icons/ItemsGenerated/MyPlugin_Custom_Item.png",
  "Quality": "Common",
  "MaxStack": 64,
  "Categories": ["Items.Misc"]
}
```

## Template Inheritance

Items use `Parent` for inheritance:

```json
{
  "Parent": "Template_Weapon_Sword",
  "TranslationProperties": {
    "Name": "server.items.Weapon_Sword_Iron.name"
  },
  "Model": "Items/Weapons/Sword/Iron.blockymodel",
  "Texture": "Items/Weapons/Sword/Iron_Texture.png",
  "Quality": "Uncommon",
  "ItemLevel": 20,
  "MaxDurability": 120
}
```

## Core Properties

| Property | Type | Description |
|----------|------|-------------|
| `Parent` | string | Template to inherit from |
| `Model` | string | Path to .blockymodel file |
| `Texture` | string | Path to texture file |
| `Icon` | string | Path to icon image |
| `Quality` | string | Quality tier |
| `MaxStack` | int | Maximum stack size |
| `Categories` | string[] | Item categories |
| `Tags` | object | Type and family classification |

## Quality Tiers

| Quality | Color | Description |
|---------|-------|-------------|
| `Template` | - | Base template |
| `Common` | White | Basic items |
| `Uncommon` | Green | Better items |
| `Rare` | Blue | Valuable items |
| `Epic` | Purple | High-tier items |
| `Legendary` | Orange | Best-in-class |

## Durability Properties

| Property | Type | Description |
|----------|------|-------------|
| `MaxDurability` | float | Maximum durability (0 = unbreakable) |
| `DurabilityLossOnHit` | float | Durability lost per hit |

## Weapon Configuration

```json
{
  "Parent": "Template_Weapon_Sword",
  "Quality": "Rare",
  "ItemLevel": 30,
  "MaxDurability": 150,
  "DurabilityLossOnHit": 0.21,
  "Interactions": {
    "Primary": "Root_Weapon_Sword_Primary",
    "Secondary": "Root_Weapon_Sword_Secondary_Guard"
  },
  "InteractionVars": {
    "Swing_Left_Damage": {
      "Interactions": [{
        "Parent": "Weapon_Sword_Primary_Swing_Left_Damage",
        "DamageCalculator": {
          "BaseDamage": { "Physical": 15 }
        }
      }]
    }
  }
}
```

## Armor Configuration

```json
{
  "Parent": "Template_Armor_Chestplate",
  "Quality": "Rare",
  "Armor": {
    "Slot": "Chest",
    "Defense": 8,
    "Toughness": 2
  },
  "MaxDurability": 240
}
```

Armor slots: `Head`, `Chest`, `Legs`, `Feet`

## Tool Configuration

```json
{
  "Parent": "Template_Tool_Pickaxe",
  "Quality": "Uncommon",
  "Tool": {
    "Type": "Pickaxe",
    "MiningSpeed": 6.0,
    "HarvestLevel": 2
  },
  "MaxDurability": 250
}
```

## Consumable Configuration

```json
{
  "Consumable": true,
  "Interactions": {
    "Primary": "Root_Consumable_Eat"
  },
  "Effects": {
    "Hunger": 6,
    "Saturation": 1.2
  }
}
```

## Recipe Configuration

```json
{
  "Recipe": {
    "TimeSeconds": 5.0,
    "KnowledgeRequired": false,
    "Input": [
      { "ItemId": "Ingredient_Bar_Iron", "Quantity": 3 },
      { "ItemId": "Ingredient_Wood", "Quantity": 1 }
    ],
    "BenchRequirement": [
      {
        "Type": "Crafting",
        "Categories": ["Weapons"],
        "Id": "Weapon_Bench",
        "RequiredTierLevel": 1
      }
    ]
  }
}
```

## Icon Properties

```json
{
  "IconProperties": {
    "Scale": 0.35,
    "Translation": [-22, -22],
    "Rotation": [45, 90, 0]
  }
}
```

## Item Appearance Conditions

Visual changes based on durability:

```json
{
  "ItemAppearanceConditions": {
    "Health": [
      {
        "Condition": [0, 20],
        "Model": "Items/Weapons/Axe/Iron.blockymodel",
        "Texture": "Items/Weapons/Axe/Iron_Damaged.png"
      },
      {
        "Condition": [21, 100],
        "Texture": "Items/Weapons/Axe/Iron_Texture.png"
      }
    ]
  }
}
```

## Accessing Items in Code

```java
// Get item by ID
Item ironSword = Item.getAssetMap().get("Weapon_Sword_Iron");

// Check item exists
boolean exists = Item.getAssetMap().containsKey("MyPlugin_Custom_Item");

// Get all item IDs
Set<String> allItems = Item.getAssetMap().keySet();

// Get item properties
String model = item.getModel();
double durability = item.getMaxDurability();
int maxStack = item.getMaxStack();
String[] categories = item.getCategories();
```

## Working with ItemStacks

```java
// Create item stack
ItemStack sword = new ItemStack("Weapon_Sword_Iron");
ItemStack arrows = new ItemStack("Arrow_Standard", 64);

// With metadata
BsonDocument metadata = new BsonDocument();
metadata.put("enchantment", new BsonString("fire"));
ItemStack enchanted = new ItemStack("Weapon_Sword_Iron", 1, metadata);

// With durability
ItemStack damaged = new ItemStack(
    "Weapon_Sword_Iron",
    1,      // quantity
    50.0,   // current durability
    100.0,  // max durability
    null    // metadata
);

// Check properties
boolean empty = stack.isEmpty();
boolean broken = stack.isBroken();
boolean stackable = stack.isStackableWith(otherStack);

// Clone
ItemStack copy = stack.clone();
```

## Damage System

```json
{
  "DamageCalculator": {
    "BaseDamage": { "Physical": 18 },
    "Type": "Absolute"
  },
  "DamageEffects": {
    "Knockback": { "Force": 1, "VelocityY": 5 },
    "WorldSoundEventId": "SFX_Sword_Impact",
    "WorldParticles": [{ "SystemId": "Impact_Blade_01" }]
  }
}
```

## Interaction Variables

Variables passed to interactions:

```json
{
  "InteractionVars": {
    "Attack_Damage": {
      "DamageCalculator": {
        "BaseDamage": { "Physical": 12 }
      }
    },
    "Swing_Effect": {
      "Effects": {
        "WorldSoundEventId": "SFX_Sword_Swing"
      }
    }
  }
}
```

## Sound Configuration

```json
{
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

## Weapon Template Types

| Template | Description |
|----------|-------------|
| `Template_Weapon_Sword` | Swords with swing attacks |
| `Template_Weapon_Shortbow` | Bows with charge mechanics |
| `Template_Weapon_Crossbow` | Crossbows with reload |
| `Template_Weapon_Mace` | Maces with stun effects |
| `Template_Weapon_Daggers` | Quick dual attacks |
| `Template_Weapon_Shield` | Blocking mechanics |
| `Template_Weapon_Battleaxe` | Heavy attacks |
| `Template_Weapon_Spear` | Thrust attacks |
| `Template_Weapon_Staff` | Magic weapons |

## Best Practices

1. **Use plugin prefix**: Always prefix custom item IDs (e.g., `MyPlugin_Sword`)
2. **Inherit from templates**: Use `Parent` to reduce duplication
3. **Provide icons**: Always include an `Icon` path
4. **Set appropriate MaxStack**: Weapons/tools = 1, materials = 64-100
5. **Use quality tiers**: Match quality to item power level
6. **Test durability**: Balance `MaxDurability` and `DurabilityLossOnHit`
7. **Define recipes**: Make items craftable when appropriate

## Documentation Reference
- Item overview: `/docs/src/content/docs/api-reference/assets/items/overview.mdx`
- Weapons: `/docs/src/content/docs/api-reference/assets/items/weapons.mdx`
- Armor: `/docs/src/content/docs/api-reference/assets/items/armor.mdx`
- Tools: `/docs/src/content/docs/api-reference/assets/items/tools.mdx`
- Consumables: `/docs/src/content/docs/api-reference/assets/items/consumables.mdx`
- Durability: `/docs/src/content/docs/api-reference/assets/items/durability.mdx`
- Recipes: `/docs/src/content/docs/api-reference/assets/items/recipes.mdx`
- Combat: `/docs/src/content/docs/api-reference/assets/items/combat.mdx`
