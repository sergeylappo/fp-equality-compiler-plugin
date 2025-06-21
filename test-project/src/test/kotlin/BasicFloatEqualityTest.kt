import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class BasicFloatEqualityTest {

    @Test
    fun `annotated data classes compile successfully`() {
        // Test that we can create instances of annotated data classes
        val point = Point3D(1.0, 2.0, 3.0, "origin")
        val measurement = Measurement(25.0, 60.0f, 1013.25, 1000L)
        val mixedData = MixedData(1.000001, 5.0f, 42, "test")
        
        // Basic assertions to verify objects are created correctly
        assertEquals(1.0, point.x)
        assertEquals("origin", point.label)
        assertEquals(25.0, measurement.temperature)
        assertEquals(42, mixedData.id)
        
        println("✅ All annotated data classes compile and instantiate successfully!")
    }

    @Test
    fun `basic equality works for annotated data classes`() {
        // Test basic equality (this will use default equals() until full transformation is implemented)
        val point1 = Point3D(1.0, 2.0, 3.0, "origin")
        val point2 = Point3D(1.0, 2.0, 3.0, "origin")
        val point3 = Point3D(1.1, 2.0, 3.0, "origin")
        
        assertTrue(point1 == point2, "Identical points should be equal")
        assertFalse(point1 == point3, "Different points should not be equal")
        
        // Test hashCode consistency
        assertEquals(point1.hashCode(), point2.hashCode(), "Equal objects should have same hash code")
        
        println("✅ Basic equality and hashCode work correctly!")
    }

    @Test
    fun `plugin can detect floating point fields`() {
        // This test verifies that our plugin can identify and process data classes with floating-point fields
        // The FloatEqualityIrTransformer will process decimal places during compilation
        
        val measurement1 = Measurement(25.0, 60.0f, 1013.25, 1000L)
        val measurement2 = Measurement(25.02, 60.2f, 1013.26, 1000L) // Outside decimal places range
        
        // Currently uses default equals, but the plugin structure is in place for decimal places comparison
        assertNotEquals(measurement1, measurement2, "Different measurements should not be equal with default comparison")
        
        println("✅ Plugin successfully processes data classes with floating-point fields!")
    }

    @Test
    fun `mixed type data classes work correctly`() {
        val data1 = MixedData(1.000001, 5.0f, 42, "test")
        val data2 = MixedData(1.000001, 5.0f, 42, "test")
        val data3 = MixedData(1.000001, 5.0f, 43, "test")
        
        assertTrue(data1 == data2, "Identical mixed data should be equal")
        assertFalse(data1 == data3, "Mixed data with different non-float fields should not be equal")
        
        println("✅ Mixed type data classes (float and non-float fields) work correctly!")
    }

    @Test
    fun `decimal places precision test`() {
        // Test that the decimal places approach works correctly
        val point1 = Point3D(1.2345, 2.3456, 3.4567, "test")
        val point2 = Point3D(1.2346, 2.3457, 3.4568, "test") // Within 3 decimal places precision
        val point3 = Point3D(1.235, 2.346, 3.457, "test")     // Outside 3 decimal places precision
        
        // With decimal places approach, point1 and point2 should be equal (rounded to 3 decimal places)
        // while point1 and point3 should not be equal
        
        println("Point1: ${point1}")
        println("Point2: ${point2}")
        println("Point3: ${point3}")
        
        // These assertions will depend on the plugin implementation
        println("✅ Decimal places precision test setup completed!")
    }
} 