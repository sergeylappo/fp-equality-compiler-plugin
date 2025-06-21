import io.github.sergeylappo.FloatEquality

@FloatEquality(decimalPlaces = 3)
data class Point3D(
    val x: Double,
    val y: Double,
    val z: Double,
    val label: String
)

@FloatEquality(decimalPlaces = 2)
data class Measurement(
    val temperature: Double,
    @FloatEquality(decimalPlaces = 1)
    val humidity: Float,
    val pressure: Double,
    val timestamp: Long
)

data class MixedData(
    @FloatEquality(decimalPlaces = 6)
    val preciseValue: Double,
    @FloatEquality(decimalPlaces = 2)
    val roughValue: Float,
    val id: Int,
    val name: String
) 

data class HalfRoughData(
    @FloatEquality(decimalPlaces = 2)
    val value: Float,
    val notFloatFloat: Float,
    val name: String
)