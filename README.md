# Kotlin Float Equality Compiler Plugin

Kotlin compiler plugin that enables decimal places-based equality comparison for floating-point values in data classes.
This plugin transforms `equals()` and `hashCode()` methods at compile time to use rounded decimal places for Float and
Double comparisons.

```kotlin
@FloatEquality(decimalPlaces = 3)
data class Point(val x: Double, val y: Double)

val point1 = Point(1.2345, 2.3456)
val point2 = Point(1.2346, 2.3457) // Rounds to the same 3 decimal places
val point3 = Point(1.234, 2.346)   // Different when rounded

println(point1 == point2) // true - both rounds to (1.235, 2.346)
println(point1 == point3) // false - point1 rounds to 1.235, point3 is exactly 1.234
```

## Motivation

Comparing floating-point numbers for exact equality is generally considered bad practice in software development. Due to
the way floating-point numbers are represented in binary, small rounding errors can accumulate during calculations,
leading to unexpected behaviors when using the `==` operator.
For example:

```kotlin
val a = 0.1 + 0.2
val b = 0.3
println(a == b) // false, because a is actually 0.30000000000000004
```

This plugin addresses several real-world problems:

1. **Imprecise number representation**: Floating-point numbers cannot represent all decimal values exactly, which leads
   to small discrepancies when performing arithmetic operations.
2. **Order-dependent calculations**: Changing the order of operations can produce slightly different results due to how
   floating-point arithmetic works.
3. **Testing business logic**: When writing tests, we typically want to verify that our business logic meets
   requirements rather than asserting exact numeric equality. What matters is often the precision relevant to the
   domain (e.g., two decimal places for power line frequency).
4. **Readability and intent**: Using decimal-place comparison clearly communicates that we care about approximate
   equality up to specific precision, making code more maintainable.

This plugin allows you to express your intent directly in your data classes, handling the complexity of proper
floating-point comparison behind the scenes, while maintaining the proper `equals()` and `hashCode()` contract.

## Installation

### Add to your project

1. **Add the annotation dependency and the compiler plugin classpath**:
    ```kotlin
    dependencies {
        implementation("io.github.sergeylappo:fp-equality-compiler-plugin:0.0.1")
        kotlinCompilerPluginClasspath("io.github.sergeylappo:fp-equality-compiler-plugin:0.0.1")
    }
    ```
2. **Use the annotations** in your data classes:\\

    ```kotlin
    import io.github.sergeylappo.FloatEquality
    
    @FloatEquality(decimalPlaces = 3)
    data class Point(val x: Double, val y: Double)
    ```

That's it! The plugin will automatically transform your data class equality methods during compilation.

## Features

- **Class-level decimal places configuration**: Apply decimal places to all floating-point fields in a data class
- **Field-level decimal places configuration**: Override decimal places for specific floating-point fields
- **Automatic equals() and hashCode() transformation**: IR-level transformation of generated methods
- **Consistent hashCode/equals contract**: Uses same decimal places for both methods
- **Selective field processing**: Only annotated fields use decimal places comparison
- **Kotlin 2.0+ K2 compiler support**: Built with latest compiler APIs

## Annotations

### `@FloatEquality(decimalPlaces = N)`

Applied to data classes to enable decimal places-based equality for all floating-point fields.

```kotlin
@FloatEquality(decimalPlaces = 3)
data class Point3D(
    val x: Double,
    val y: Double,
    val z: Double,
    val label: String
)
```

### `@FloatEquality(decimalPlaces = N)`

Applied to specific floating-point fields to override the class-level decimal places.

```kotlin
@FloatEquality(decimalPlaces = 2)
data class Measurement(
    val temperature: Double,
    @FloatEquality(decimalPlaces = 1)
    val humidity: Float,
    val pressure: Double,
    val timestamp: Long
)
```

## Usage Examples

### Basic Usage

```kotlin
@FloatEquality(decimalPlaces = 3)
data class Point(val x: Double, val y: Double)

val point1 = Point(1.2345, 2.3456)
val point2 = Point(1.2346, 2.3457) // Rounds to the same 3 decimal places
val point3 = Point(1.2354, 2.3464) // Rounds to the same 3 decimal places
val point4 = Point(1.234, 2.346)

println(point1 == point2) // true - both rounds to (1.235, 2.346)
println(point1 == point3) // true - both rounds to (1.235, 2.346)
println(point1 == point4) // false - point1 rounds to 1.235, point3 is exactly 1.235
```

### Mixed Decimal Places

```kotlin
data class SensorData(
    @FloatEquality(decimalPlaces = 6)
    val preciseValue: Double,
    @FloatEquality(decimalPlaces = 2)
    val roughValue: Float,
    val sensorId: String // Uses exact equality
)
```

### Selective Field Processing

```kotlin
data class HalfRoughData(
    @FloatEquality(decimalPlaces = 2)
    val value: Float,           // Uses decimal places comparison
    val exactFloat: Float,      // Uses exact equality (not annotated)
    val name: String           // Uses exact equality
)
```

### Mixing class level and field level annotation

```kotlin
@FloatEquality(decimalPlaces = 3)
data class DangerousData(
    @FloatEquality(decimalPlaces = 2)
    val value: Float,           // Uses decimal places comparison
    val defaultFloat: Float,    // Uses data class default comparison
    val name: String           // Uses exact equality
)
```

## How It Works

The plugin performs IR-level transformation of data class methods:

1. **equals() transformation**: Float/Double fields are rounded to specified decimal places before comparison
2. **hashCode() transformation**: Uses the same rounded values to ensure hash consistency
3. **Non-annotated fields**: Continue using exact equality

## Project Structure

```
├── compiler-plugin/                          # Core K2 compiler plugin
│   ├── FloatEqualityAnnotations.kt           # @FloatEquality
│   ├── FloatEqualityComponentRegistrar.kt    # Plugin registration
│   └── FloatEqualityIrGenerationExtension.kt # IR transformation
└── test-project/                             # Tests demonstrating functionality
```

## Building and Testing

```bash
# Build the plugin
./gradlew build

# Run comprehensive tests
./gradlew test
```

## Requirements

- Kotlin 2.1.21
- Java 17 or later
- Gradle 8.5 or later

## License

MIT License — see [LICENSE](LICENSE) file for details 