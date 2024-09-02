package com.github.valentinaebi.capybara.values

import org.objectweb.asm.tree.analysis.Value

sealed interface ProgramValue : Value {
    val rawValueType: RawValueType
    override fun getSize(): Int = rawValueType.size
}

object SecondBytePlaceholder : ProgramValue {
    override val rawValueType: RawValueType = RawValueType.Placeholder
}

sealed interface ConcreteValue : ProgramValue {
    val value: Number
}

data class ConcreteInt32BitsValue(override val value: Int) : ConcreteValue {
    override val rawValueType: RawValueType = RawValueType.Int32
}

data class ConcreteLongValue(override val value: Long) : ConcreteValue {
    override val rawValueType: RawValueType = RawValueType.Long
}

data class ConcreteFloatValue(override val value: Float) : ConcreteValue {
    override val rawValueType: RawValueType = RawValueType.Float
}

data class ConcreteDoubleValue(override val value: Double) : ConcreteValue {
    override val rawValueType: RawValueType = RawValueType.Double
}

class SymbolicValue(override val rawValueType: RawValueType) : ProgramValue

data class Negated(val negated: ProgramValue) : ProgramValue {
    override val rawValueType: RawValueType get() = negated.rawValueType
}

sealed class BinaryValue : ProgramValue {
    abstract val l: ProgramValue
    abstract val r: ProgramValue

    init {
        require(l.rawValueType == r.rawValueType)
    }

    override val rawValueType: RawValueType get() = l.rawValueType
}

data class Add(override val l: ProgramValue, override val r: ProgramValue) : BinaryValue()
data class Sub(override val l: ProgramValue, override val r: ProgramValue) : BinaryValue()
data class Mul(override val l: ProgramValue, override val r: ProgramValue) : BinaryValue()
data class Div(override val l: ProgramValue, override val r: ProgramValue) : BinaryValue()
