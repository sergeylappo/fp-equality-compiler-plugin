package io.github.sergeylappo

/**
 * Annotation used to specify the precision for floating-point equality checks.
 *
 * This annotation can be applied to data classes and specific data classes fields.
 *
 * @property decimalPlaces The number of decimal places to use when performing
 * floating-point equality comparisons.
 * N.B. Floating point values would be rounded to required precision before comparison.
 */
annotation class FloatEquality(val decimalPlaces: Int)