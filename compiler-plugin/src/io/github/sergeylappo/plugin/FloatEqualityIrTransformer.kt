@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package io.github.sergeylappo.plugin

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irNot
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irEquals
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetField
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.isDouble
import org.jetbrains.kotlin.ir.types.isFloat
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isSubtypeOfClass
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import kotlin.math.pow

/**
 * IR transformer that modifies floating-point equality comparisons and hash code calculations
 * in data classes annotated with @FloatEquality or having fields annotated with @FloatEquality.
 *
 * This transformer:
 * - Replaces direct floating-point equality comparisons (==) with decimal-place-aware comparisons
 * - Modifies floating-point hash code calculations to use rounded values
 * - Supports both Float and Double types
 * - Allows per-field and per-class decimal place configuration
 *
 * The transformation works by:
 * 1. Rounding floating-point values to specified decimal places
 * 2. Converting rounded values to integers for comparison/hashing
 * 3. Using integer equality/hash codes for consistent behavior
 *
 * @param context The IR plugin context providing access to built-in symbols and types
 */
class FloatEqualityIrTransformer(
    private val context: IrPluginContext,
) : IrElementTransformerVoid() {

    companion object {
        /** Annotation FQN for class-level float equality configuration */
        private val FLOAT_EQUALITY_ANNOTATION = FqName("io.github.sergeylappo.FloatEquality")

        // Method names
        private const val EQUALS_METHOD = "equals"
        private const val HASHCODE_METHOD = "hashCode"
        private const val EQEQ_METHOD = "EQEQ"
        private const val TIMES_METHOD = "times"
        private const val PLUS_METHOD = "plus"
        private const val TO_INT_METHOD = "toInt"

        // Constants
        /** Offset added for proper rounding before truncation to integer */
        private const val ROUNDING_OFFSET = 0.5
    }

    /** Currently processed data class, if any */
    private var currentDataClass: IrClass? = null

    /** Currently processed method (equals or hashCode) */
    private var currentMethod: IrSimpleFunction? = null

    /** Map of field names to their configured decimal places */
    private var fieldDecimalPlaces: Map<String, Int> = emptyMap()

    override fun visitElement(element: IrElement): IrElement {
        return when (element) {
            is IrDeclaration, is IrFile, is IrModuleFragment -> element.transform(this, null)
            else -> element
        }
    }

    /**
     * Visits class declarations and sets up context for data classes with float equality annotations.
     */
    override fun visitClass(declaration: IrClass): IrClass {
        if (declaration.isData && hasFloatEqualityAnnotations(declaration)) {
            currentDataClass = declaration
            fieldDecimalPlaces = buildFieldDecimalPlacesMap(declaration)
            declaration.transformChildrenVoid()
            currentDataClass = null
            fieldDecimalPlaces = emptyMap()
        } else {
            declaration.transformChildrenVoid()
        }
        return declaration
    }

    /**
     * Visits function declarations and sets up context for equals/hashCode methods.
     */
    override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement {
        if (currentDataClass != null && isTargetMethod(declaration)) {
            currentMethod = declaration
            declaration.transformChildrenVoid()
            currentMethod = null
        } else {
            declaration.transformChildrenVoid()
        }
        return declaration
    }

    /**
     * Transforms function calls to replace floating-point operations with decimal-place-aware versions.
     */
    override fun visitCall(expression: IrCall): IrExpression {
        val transformedCall = super.visitCall(expression) as IrCall

        if (currentDataClass != null && currentMethod != null) {
            val functionName = transformedCall.symbol.owner.name.asString()

            return when (currentMethod!!.name.asString()) {
                EQUALS_METHOD -> {
                    if (functionName == EQEQ_METHOD && isFloatingPointEqEq(transformedCall)) {
                        val fieldName = extractFieldNameFromComparison(transformedCall)
                        replaceWithDecimalPlacesComparison(transformedCall, fieldName)
                    } else transformedCall
                }

                HASHCODE_METHOD -> {
                    if (isFloatingPointHashCode(transformedCall)) {
                        val fieldName = extractFieldNameFromHashCode(transformedCall)
                        replaceWithDecimalPlacesHashCode(transformedCall, fieldName)
                    } else transformedCall
                }

                else -> transformedCall
            }
        }

        return transformedCall
    }

    /**
     * Checks if a class has float equality annotations (either class-level or field-level).
     */
    private fun hasFloatEqualityAnnotations(declaration: IrClass): Boolean {
        val hasClassAnnotation = declaration.hasAnnotation(FLOAT_EQUALITY_ANNOTATION)
        val hasFieldAnnotations = declaration.primaryConstructor?.valueParameters?.any { parameter ->
            parameter.hasAnnotation(FLOAT_EQUALITY_ANNOTATION)
        } ?: false
        return hasClassAnnotation || hasFieldAnnotations
    }

    /**
     * Checks if a function is a target method for transformation (equals or hashCode).
     */
    private fun isTargetMethod(declaration: IrSimpleFunction): Boolean {
        return (declaration.name.asString() == EQUALS_METHOD && declaration.valueParameters.size == 1) ||
                (declaration.name.asString() == HASHCODE_METHOD && declaration.valueParameters.isEmpty())
    }

    /**
     * Checks if a type is a floating-point type (Float or Double).
     */
    private fun isFloatingPointType(type: IrType?): Boolean {
        return type != null && (type.isFloat() || type.isDouble())
    }

    /**
     * Checks if a call is a floating-point equality comparison.
     */
    private fun isFloatingPointEqEq(call: IrCall): Boolean {
        return call.symbol.owner.name.asString() == EQEQ_METHOD &&
                call.valueArgumentsCount >= 2 &&
                (isFloatingPointType(call.getValueArgument(0)?.type) ||
                        isFloatingPointType(call.getValueArgument(1)?.type))
    }

    /**
     * Checks if a call is a floating-point hashCode operation.
     */
    private fun isFloatingPointHashCode(call: IrCall): Boolean {
        return call.symbol.owner.name.asString() == HASHCODE_METHOD &&
                call.valueArgumentsCount == 0 &&
                isFloatingPointType(call.dispatchReceiver?.type)
    }

    /**
     * Extracts the field name from a comparison expression.
     */
    private fun extractFieldNameFromComparison(call: IrCall): String? {
        return extractFieldNameFromExpression(call.getValueArgument(0))
            ?: extractFieldNameFromExpression(call.getValueArgument(1))
    }

    /**
     * Extracts the field name from a hashCode expression.
     */
    private fun extractFieldNameFromHashCode(call: IrCall): String? {
        return extractFieldNameFromExpression(call.dispatchReceiver)
    }

    /**
     * Extracts field name from various expression types (property access, field access).
     */
    private fun extractFieldNameFromExpression(expression: IrExpression?): String? {
        return when (expression) {
            is IrCall -> {
                val function = expression.symbol.owner
                if (function.isPropertyAccessor) {
                    function.correspondingPropertySymbol?.owner?.name?.asString()
                } else {
                    null
                }
            }

            is IrGetField -> expression.symbol.owner.name.asString()
            else -> null
        }
    }

    /**
     * Replaces a floating-point equality comparison with a decimal-place-aware version.
     * Rounds both operands to the specified decimal places and compares the resulting integers.
     */
    private fun replaceWithDecimalPlacesComparison(call: IrCall, fieldName: String?): IrExpression {
        val leftOperand = call.getValueArgument(0)!!
        val rightOperand = call.getValueArgument(1)!!
        val decimalPlaces = getDecimalPlacesForField(fieldName) ?: return call

        val useDouble = leftOperand.type.isDouble() || rightOperand.type.isDouble()
        val irBuilder = DeclarationIrBuilder(context, currentMethod!!.symbol)

        return irBuilder.run {
            val leftRounded = roundToDecimalPlacesAsInt(leftOperand, decimalPlaces, useDouble)
            val rightRounded = roundToDecimalPlacesAsInt(rightOperand, decimalPlaces, useDouble)

            // Get isNaN function
            val targetClass = if (useDouble) {
                context.irBuiltIns.doubleClass
            } else {
                context.irBuiltIns.floatClass
            }

            val isNaNFunction = context.irBuiltIns.getNonBuiltInFunctionsByExtensionReceiver(
                Name.identifier("isNaN"),
                "kotlin"
            ).filter { it.key.isSubtypeOfClass(targetClass) }.values.first()

            // Create !isNaN calls for both operands
            val isNotNanLeft = irNot(irCall(isNaNFunction).apply { extensionReceiver = leftOperand })
            val isNotNanRight = irNot(irCall(isNaNFunction).apply { extensionReceiver = rightOperand })

            // Create (notNanLeft && notNanRight) expression
            val bothNotNaN = irCall(context.irBuiltIns.andandSymbol).apply {
                putValueArgument(0, isNotNanLeft)
                putValueArgument(1, isNotNanRight)
            }

            // Create (leftRounded == rightRounded) expression
            val roundedEqual = irEquals(leftRounded, rightRounded)

            // Create final (bothNotNaN && roundedEqual) expression
            irCall(context.irBuiltIns.andandSymbol).apply {
                putValueArgument(0, bothNotNaN)
                putValueArgument(1, roundedEqual)
            }
        }
    }

    /**
     * Replaces a floating-point hashCode calculation with a decimal-place-aware version.
     * Rounds the value to the specified decimal places and returns the integer's hash code.
     */
    private fun replaceWithDecimalPlacesHashCode(call: IrCall, fieldName: String?): IrExpression {
        val receiver = call.dispatchReceiver!!
        val decimalPlaces = getDecimalPlacesForField(fieldName) ?: return call

        val irBuilder = DeclarationIrBuilder(context, currentMethod!!.symbol)
        val useDouble = receiver.type.isDouble()

        return irBuilder.run {
            val roundedInt = roundToDecimalPlacesAsInt(receiver, decimalPlaces, useDouble)

            irCall(
                context.irBuiltIns.intClass.owner.declarations
                    .filterIsInstance<IrSimpleFunction>()
                    .first { it.name.asString() == HASHCODE_METHOD }
            ).apply {
                dispatchReceiver = roundedInt
            }
        }
    }

    /**
     * Rounds a floating-point value to the specified decimal places and converts to Int.
     *
     * The transformation: value -> (value * 10^decimalPlaces + 0.5).toInt()
     * This ensures proper rounding behavior for both positive and negative numbers.
     */
    private fun roundToDecimalPlacesAsInt(value: IrExpression, decimalPlaces: Int, useDouble: Boolean): IrExpression {
        val irBuilder = DeclarationIrBuilder(context, currentMethod!!.symbol)
        val multiplier = 10.0.pow(decimalPlaces)

        return irBuilder.run {
            val multiplierConst = createNumberConstant(multiplier, useDouble)
            val halfConst = createNumberConstant(ROUNDING_OFFSET, useDouble)

            // (value * multiplier + 0.5).toInt()
            val multiplied = createArithmeticCall(value, TIMES_METHOD, multiplierConst, useDouble)
            val addedHalf = createArithmeticCall(multiplied, PLUS_METHOD, halfConst, useDouble)
            createArithmeticCall(addedHalf, TO_INT_METHOD, null, useDouble)
        }
    }

    /**
     * Creates a numeric constant (Float or Double) based on the useDouble flag.
     */
    private fun DeclarationIrBuilder.createNumberConstant(value: Double, useDouble: Boolean): IrExpression {
        return if (useDouble) {
            IrConstImpl.double(UNDEFINED_OFFSET, UNDEFINED_OFFSET, context.irBuiltIns.doubleType, value)
        } else {
            IrConstImpl.float(UNDEFINED_OFFSET, UNDEFINED_OFFSET, context.irBuiltIns.floatType, value.toFloat())
        }
    }

    /**
     * Creates an arithmetic method call (times, plus, toInt) on floating-point types.
     */
    private fun DeclarationIrBuilder.createArithmeticCall(
        receiver: IrExpression,
        methodName: String,
        argument: IrExpression?,
        useDouble: Boolean
    ): IrExpression {
        val numberClass = if (useDouble) context.irBuiltIns.doubleClass else context.irBuiltIns.floatClass
        val method = numberClass.owner.declarations
            .filterIsInstance<IrSimpleFunction>()
            .first { it.name.asString() == methodName }

        return irCall(method).apply {
            dispatchReceiver = receiver
            argument?.let { putValueArgument(0, it) }
        }
    }

    /**
     * Builds a map of field names to their configured decimal places from constructor parameters.
     */
    private fun buildFieldDecimalPlacesMap(dataClass: IrClass): Map<String, Int> {
        return dataClass.primaryConstructor?.valueParameters
            ?.mapNotNull { parameter ->
                getFieldDecimalPlaces(parameter)?.let { decimalPlaces ->
                    parameter.name.asString() to decimalPlaces
                }
            }?.toMap() ?: emptyMap()
    }

    /**
     * Gets the decimal places configuration for a field, checking field-level annotation first,
     * then falling back to class-level annotation.
     */
    private fun getDecimalPlacesForField(fieldName: String?): Int? {
        return fieldName?.let { fieldDecimalPlaces[it] }
            ?: getClassDecimalPlaces(currentDataClass!!)
    }

    /**
     * Extracts decimal places from class-level @FloatEquality annotation.
     */
    private fun getClassDecimalPlaces(dataClass: IrClass): Int? {
        return dataClass.annotations
            .find { it.type.classFqName == FLOAT_EQUALITY_ANNOTATION }
            ?.getValueArgument(0)
            ?.let { (it as? IrConstImpl)?.value as? Int }
    }

    /**
     * Extracts decimal places from field-level @FloatEquality annotation.
     */
    private fun getFieldDecimalPlaces(parameter: IrValueParameter): Int? {
        return parameter.annotations
            .find { it.type.classFqName == FLOAT_EQUALITY_ANNOTATION }
            ?.getValueArgument(0)
            ?.let { (it as? IrConstImpl)?.value as? Int }
    }
} 
