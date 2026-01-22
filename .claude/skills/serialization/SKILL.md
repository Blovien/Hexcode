# Hytale Serialization (Codec) System

## Overview
The Codec System provides serialization and deserialization for BSON and JSON formats using a type-safe, builder-based approach with versioning, validation, and polymorphism support.

## Package Location
- Core: `com.hypixel.hytale.codec`
- Builders: `com.hypixel.hytale.codec.builder`
- Built-in: `com.hypixel.hytale.codec.codecs`
- Validation: `com.hypixel.hytale.codec.validation`

## Codec Interface

```java
public interface Codec<T> extends RawJsonCodec<T>, SchemaConvertable<T> {
    // Decode from BSON
    T decode(BsonValue bsonValue, ExtraInfo extraInfo);

    // Encode to BSON
    BsonValue encode(T value, ExtraInfo extraInfo);

    // Decode from JSON
    T decodeJson(RawJsonReader reader, ExtraInfo extraInfo) throws IOException;

    // Generate JSON schema
    Schema toSchema(SchemaContext context);
}
```

## KeyedCodec

Wraps codec with field name:

```java
public class KeyedCodec<T> {
    public KeyedCodec(String key, Codec<T> codec);

    // Get from document (throws if missing)
    public T get(BsonDocument doc, ExtraInfo info);

    // Get or null
    public T getOrNull(BsonDocument doc, ExtraInfo info);

    // Get with default
    public T getOrDefault(BsonDocument doc, ExtraInfo info, T defaultValue);

    // Put into document
    public void put(BsonDocument doc, T value, ExtraInfo info);
}
```

**Note:** Key names must start with uppercase letter.

## Built-in Codecs

### Primitives

| Codec | Java Type |
|-------|-----------|
| `Codec.STRING` | String |
| `Codec.BOOLEAN` | Boolean |
| `Codec.INTEGER` | Integer |
| `Codec.LONG` | Long |
| `Codec.FLOAT` | Float |
| `Codec.DOUBLE` | Double |
| `Codec.BYTE` | Byte |
| `Codec.SHORT` | Short |

### Arrays

| Codec | Java Type |
|-------|-----------|
| `Codec.STRING_ARRAY` | String[] |
| `Codec.INT_ARRAY` | int[] |
| `Codec.LONG_ARRAY` | long[] |
| `Codec.FLOAT_ARRAY` | float[] |
| `Codec.DOUBLE_ARRAY` | double[] |

### Utilities

| Codec | Java Type | Description |
|-------|-----------|-------------|
| `Codec.UUID_BINARY` | UUID | Binary format |
| `Codec.UUID_STRING` | UUID | String format |
| `Codec.PATH` | Path | File path |
| `Codec.INSTANT` | Instant | Timestamp |
| `Codec.DURATION` | Duration | String duration |
| `Codec.DURATION_SECONDS` | Duration | Seconds |

### Collections

```java
// Map with string keys
new MapCodec<>(valueCodec, HashMap::new);

// Array of objects
new ArrayCodec<>(elementCodec, defaultArray);

// Set
new SetCodec<>(valueCodec, HashSet::new);

// Enum
new EnumCodec<>(MyEnum.class);
```

## BuilderCodec

For complex objects:

```java
public class BuilderCodec<T> implements Codec<T> {
    // Create builder
    public static <T> BuilderCodecBuilder<T> builder(
        Class<T> clazz,
        Supplier<T> constructor
    );

    // With parent (inheritance)
    public static <T> BuilderCodecBuilder<T> builder(
        Class<T> clazz,
        Supplier<T> constructor,
        BuilderCodec<? super T> parent
    );

    // Abstract class
    public static <T> BuilderCodecBuilder<T> abstractBuilder(Class<T> clazz);
}
```

## Creating Custom Codecs

### Simple Object

```java
public class MyConfig {
    public String name;
    public float value;
    public boolean enabled;

    public static final BuilderCodec<MyConfig> CODEC =
        BuilderCodec.builder(MyConfig.class, MyConfig::new)
            .append(
                new KeyedCodec<>("Name", Codec.STRING),
                (config, name) -> config.name = name,
                config -> config.name
            )
            .addValidator(Validators.nonNull())
            .documentation("Configuration name")
            .add()

            .append(
                new KeyedCodec<>("Value", Codec.FLOAT),
                (config, value) -> config.value = value,
                config -> config.value
            )
            .documentation("Numeric value")
            .add()

            .append(
                new KeyedCodec<>("Enabled", Codec.BOOLEAN),
                (config, enabled) -> config.enabled = enabled,
                config -> config.enabled
            )
            .add()

            .build();
}
```

### With Inheritance

```java
public abstract class BaseConfig {
    public String id;

    public static final BuilderCodec<BaseConfig> BASE_CODEC =
        BuilderCodec.abstractBuilder(BaseConfig.class)
            .appendInherited(
                new KeyedCodec<>("Id", Codec.STRING),
                (obj, id) -> obj.id = id,
                obj -> obj.id,
                (obj, parent) -> obj.id = parent.id
            )
            .addValidator(Validators.nonNull())
            .add()
            .build();
}

public class DerivedConfig extends BaseConfig {
    public int level;

    public static final BuilderCodec<DerivedConfig> CODEC =
        BuilderCodec.builder(DerivedConfig.class, DerivedConfig::new,
            BaseConfig.BASE_CODEC)
            .append(
                new KeyedCodec<>("Level", Codec.INTEGER),
                (obj, level) -> obj.level = level,
                obj -> obj.level
            )
            .add()
            .build();
}
```

### Polymorphic Types

```java
public abstract class ActionConfig {
    public static final CodecMapCodec<ActionConfig> CODEC =
        new CodecMapCodec<>("Type");

    public static final BuilderCodec<ActionConfig> BASE_CODEC =
        BuilderCodec.abstractBuilder(ActionConfig.class)
            .build();
}

public class DamageActionConfig extends ActionConfig {
    public float damage;

    public static final BuilderCodec<DamageActionConfig> CODEC =
        BuilderCodec.builder(DamageActionConfig.class, DamageActionConfig::new,
            ActionConfig.BASE_CODEC)
            .append(
                new KeyedCodec<>("Damage", Codec.FLOAT),
                (obj, damage) -> obj.damage = damage,
                obj -> obj.damage
            )
            .add()
            .build();

    static {
        ActionConfig.CODEC.register("Damage", DamageActionConfig.class,
            DamageActionConfig.CODEC);
    }
}
```

JSON:
```json
{
  "Type": "Damage",
  "Damage": 10.0
}
```

## Validation

```java
// Non-null
.addValidator(Validators.nonNull())

// Range
.addValidator(Validators.range(0.0, 100.0))

// Greater than
.addValidator(Validators.greaterThan(0))

// Less than
.addValidator(Validators.lessThan(1000))

// Custom
.addValidator((value, info) -> {
    if (!isValid(value)) {
        throw new ValidationException("Invalid value");
    }
})
```

## Versioning

```java
BuilderCodec.builder(MyClass.class, MyClass::new)
    // Field in versions 1-2
    .append(new KeyedCodec<>("OldField", Codec.STRING), ...)
    .codecVersion(1, 2)
    .add()

    // Field from version 2
    .append(new KeyedCodec<>("NewField", Codec.INTEGER), ...)
    .codecVersion(2)
    .add()

    .versioned()
    .build();
```

## ExtraInfo Context

```java
public class ExtraInfo {
    public static ExtraInfo get();

    // Current decode path
    public List<String> getPath();

    // Add validation error
    public void addError(String message);

    // Check for errors
    public boolean hasErrors();
}
```

## Using Codecs

### Encoding

```java
MyConfig config = new MyConfig();
config.name = "Test";
config.value = 42.0f;
config.enabled = true;

BsonValue bson = MyConfig.CODEC.encode(config, ExtraInfo.get());
```

### Decoding BSON

```java
BsonDocument doc = BsonDocument.parse("{\"Name\": \"Test\", \"Value\": 42.0}");
MyConfig config = MyConfig.CODEC.decode(doc, ExtraInfo.get());
```

### Decoding JSON

```java
String json = "{\"Name\": \"Test\", \"Value\": 42.0}";
RawJsonReader reader = new RawJsonReader(new StringReader(json));
MyConfig config = MyConfig.CODEC.decodeJson(reader, ExtraInfo.get());
```

## Optional Fields

```java
.append(
    new KeyedCodec<>("OptionalField", Codec.STRING),
    (obj, value) -> obj.optionalField = value,
    obj -> obj.optionalField
)
.setOptional(true)
.setDefaultValue("default")
.add()
```

## Field Documentation

```java
.append(...)
.documentation("This field controls...")
.add()
```

## Best Practices

1. **Use KeyedCodec**: Always use uppercase field names
2. **Add validators**: Validate required fields
3. **Document fields**: Use `.documentation()` for clarity
4. **Version codecs**: Enable versioning for evolution
5. **Register polymorphic**: Use `CodecMapCodec` for type dispatch
6. **Reuse codecs**: Store as `static final` fields
7. **Handle nulls**: Use optional fields appropriately

## Documentation Reference
- Serialization overview: `/docs/src/content/docs/api-reference/serialization/overview.mdx`
- Asset System: `/docs/src/content/docs/api-reference/assets/overview.mdx`
