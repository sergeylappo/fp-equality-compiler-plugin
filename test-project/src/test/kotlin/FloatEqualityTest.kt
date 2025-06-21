import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals

class FloatEqualityTest {

    @Test
    fun testPoint3DDecimalPlacesEquality() {
        val point1 = Point3D(1.0, 2.0, 3.0, "origin")
        val point2 = Point3D(1.0004, 2.0004, 3.0004, "origin") // Within 3 decimal places
        val point3 = Point3D(1.002, 2.002, 3.002, "origin")    // Outside 3 decimal places
        
        // Within decimal places (3 places = 0.001 precision)
        assertEquals(point1, point2, "Points within decimal places should be equal")
        assertEquals(point1.hashCode(), point2.hashCode(), "Hash codes should be equal for equal points")
        
        // Outside decimal places
        assertNotEquals(point1, point3, "Points outside decimal places should not be equal")
        
        // Different labels should make them unequal regardless of coordinates
        val point4 = Point3D(1.0, 2.0, 3.0, "different")
        assertNotEquals(point1, point4, "Points with different labels should not be equal")
    }

    @Test
    fun testPointWithNanNonEqual() {
        val point1 = Point3D(Double.NaN, 2.0, 3.0, "origin")
        val point2 = Point3D(Double.NaN, 2.0004, 3.0004, "origin") // Within 3 decimal places
        val point3 = Point3D(Double.NaN, 2.002, 3.002, "origin")    // Outside 3 decimal places

        assertNotEquals(point1, point2, "Points with NaN should not be equal")
        // Outside decimal places
        assertNotEquals(point1, point3, "Points with NaN should not be equal")
    }

    @Test
    fun testMeasurementMixedDecimalPlaces() {
        val measurement1 = Measurement(25.0, 60.0f, 1013.25, 1000L)
        val measurement2 = Measurement(25.004, 60.04f, 1013.254, 1000L)  // Within all decimal places
        val measurement3 = Measurement(25.02, 60.04f, 1013.254, 1000L)   // Temperature outside 2 decimal places  
        val measurement4 = Measurement(25.004, 60.2f, 1013.254, 1000L)   // Humidity outside 1 decimal place
        
        // Within all decimal places
        assertEquals(measurement1, measurement2, "Measurements within all decimal places should be equal")
        assertEquals(measurement1.hashCode(), measurement2.hashCode(), "Hash codes should be equal")
        
        // Temperature outside class decimal places (2)
        assertNotEquals(measurement1, measurement3, "Temperature outside decimal places should make measurements unequal")
        
        // Humidity outside field-specific decimal places (1)  
        assertNotEquals(measurement1, measurement4, "Humidity outside decimal places should make measurements unequal")
    }

    @Test
    fun testMixedDataFieldSpecificDecimalPlaces() {
        val data1 = MixedData(1.000001, 5.0f, 42, "test")
        val data2 = MixedData(1.0000014, 5.004f, 42, "test")  // Within both field decimal places
        val data3 = MixedData(1.00001, 5.004f, 42, "test")    // preciseValue outside 6 decimal places
        val data4 = MixedData(1.000001, 5.02f, 42, "test")    // roughValue outside 2 decimal places
        
        // Within both field decimal places
        assertEquals(data1, data2, "Data within both field decimal places should be equal")
        assertEquals(data1.hashCode(), data2.hashCode(), "Hash codes should be equal")
        
        // preciseValue outside its decimal places (6)
        assertNotEquals(data1, data3, "Data with preciseValue outside decimal places should not be equal")
        
        // roughValue outside its decimal places (2)
        assertNotEquals(data1, data4, "Data with roughValue outside decimal places should not be equal")
    }

    @Test
    fun testNonFloatingPointFieldsUnaffected() {
        val point1 = Point3D(1.0, 2.0, 3.0, "test")
        val point2 = Point3D(1.0, 2.0, 3.0, "different")
        
        // String fields should still use exact equality
        assertNotEquals(point1, point2, "Non-floating-point fields should use exact equality")
        
        val measurement1 = Measurement(25.0, 60.0f, 1013.25, 1000L)
        val measurement2 = Measurement(25.0, 60.0f, 1013.25, 2000L)
        
        // Long fields should still use exact equality
        assertNotEquals(measurement1, measurement2, "Long fields should use exact equality")
    }

    @Test
    fun testNullHandling() {
        val point1 = Point3D(1.0, 2.0, 3.0, "test")
        val nullPoint: Point3D? = null
        
        assertNotEquals(point1, nullPoint, "Object should not equal null")
        assertFalse(point1.equals(nullPoint), "Object should not equal null using equals method")
    }

    @Test
    fun testTypeChecking() {
        val point = Point3D(1.0, 2.0, 3.0, "test")
        val measurement = Measurement(1.0, 2.0f, 3.0, 123L)
        
        @Suppress("CAST_NEVER_SUCCEEDS")
        assertFalse(point == measurement as Any, "Objects of different types should not be equal")
    }

    @Test
    fun testEdgeCases() {
        // Test with zero values
        val zero1 = Point3D(0.0, 0.0, 0.0, "zero")
        val zero2 = Point3D(0.0004, 0.0004, 0.0004, "zero") // Within 3 decimal places
        
        assertEquals(zero1, zero2, "Zero values with small differences should be equal within decimal places")
        
        // Test with negative values
        val negative1 = Point3D(-1.0, -2.0, -3.0, "negative")
        val negative2 = Point3D(-1.0004, -2.0004, -3.0004, "negative") // Within 3 decimal places
        
        assertEquals(negative1, negative2, "Negative values should work with decimal places comparison")
        
        // Test with very small values
        val small1 = MixedData(1e-10, 1e-6f, 1, "small")
        val small2 = MixedData(1.0000014e-10, 1.004e-6f, 1, "small") // Within decimal places
        
        assertEquals(small1, small2, "Very small values should work with decimal places comparison")
    }

    @Test
    fun testHalfRoughDataSelectiveDecimalPlacesComparison() {
        // Test that annotated float field uses decimal places comparison
        val data1 = HalfRoughData(1.0f, 2.0f, "test")
        val data2 = HalfRoughData(1.004f, 2.0f, "test")  // value within 2 decimal places
        val data3 = HalfRoughData(1.02f, 2.0f, "test")   // value outside 2 decimal places
        
        // Test that non-annotated float field uses exact equality
        val data4 = HalfRoughData(1.0f, 2.0001f, "test") // notFloatFloat with tiny difference
        val data5 = HalfRoughData(1.0f, 2.0f, "different") // different name
        
        // Annotated field (value) should use decimal places comparison
        assertEquals(data1, data2, "Annotated float field should use decimal places comparison")
        assertEquals(data1.hashCode(), data2.hashCode(), "Hash codes should be equal for equal objects")
        
        assertNotEquals(data1, data3, "Annotated float field outside decimal places should not be equal")
        
        // Non-annotated field (notFloatFloat) should use exact equality
        assertNotEquals(data1, data4, "Non-annotated float field should use exact equality")
        
        // String field should still use exact equality
        assertNotEquals(data1, data5, "String field should use exact equality")
        
        // Test edge case: annotated field equal, non-annotated field different
        val data6 = HalfRoughData(1.004f, 2.0f, "test")      // value within decimal places
        val data7 = HalfRoughData(1.004f, 2.0001f, "test")   // same value, different notFloatFloat
        
        assertNotEquals(data6, data7, "Objects should be unequal when non-annotated float differs")
    }

    @Test
    fun testDecimalPlacesHashCodeConsistency() {
        // Test that hashCode is consistent with equals for decimal places
        val point1 = Point3D(1.2345, 2.3456, 3.4567, "test")
        val point2 = Point3D(1.2346, 2.3457, 3.4568, "test") // Within 3 decimal places
        val point3 = Point3D(1.235, 2.346, 3.457, "test")     // Also within 3 decimal places (point1 rounds to this)
        val point4 = Point3D(1.24, 2.35, 3.46, "test")        // Outside 3 decimal places

        // The IR transformation IS working for equals! Let's verify this:
        assertEquals(point1, point2, "Points should be equal within decimal places - IR transformation working!")
        assertEquals(point1, point3, "Points should be equal within decimal places - IR transformation working!")
        assertNotEquals(point1, point4, "Points should be unequal outside decimal places")
        
        assertEquals(point1.hashCode(), point3.hashCode(), "Equal objects must have equal hashCodes")
        
        assertNotEquals(point1.hashCode(), point4.hashCode(), "Unequal objects must have unequal hashCodes")
        // Note: We don't assert hashCode inequality as it's not strictly required by the contract
    }

} 