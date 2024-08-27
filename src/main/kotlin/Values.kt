package com.github.valentinaebi.capybara

import org.objectweb.asm.tree.analysis.Value
import kotlin.math.max

sealed interface ProgramValue : Value

sealed interface ReferenceValue : ProgramValue {
    override fun getSize(): Int = 1
}

class ConcreteReferenceValue(val typeInternalName: String) : ReferenceValue
data object NullValue : ReferenceValue

sealed interface NumericValue : ProgramValue
data class NumericUnaryValue(val op: UnaryOperation, val operand: NumericValue) : NumericValue {
    override fun getSize(): Int = operand.size
}
data class NumericBinaryValue(val left: NumericValue, val operation: BinaryOperation, val right: NumericValue) : NumericValue {
    override fun getSize(): Int = max(left.size, right.size)
}
class ConcreteNumeric32BitsValue(val value: Int) : NumericValue {
    override fun getSize(): Int = 1
}
class ConcreteLongValue(val value: Long) : NumericValue {
    override fun getSize(): Int = 2
}
class ConcreteDoubleValue(val value: Double) : NumericValue {
    override fun getSize(): Int = 2
}
class Symbolic32BitsValue : NumericValue {
    override fun getSize(): Int = 1
}
class Symbolic64BitsValue : NumericValue {
    override fun getSize(): Int = 2
}


enum class UnaryOperation {
    Neg, Not, // TODO others
}

enum class BinaryOperation {
    Plus, Minus, Times, Div, Mod,
    LAnd, LOr, // TODO others
}
