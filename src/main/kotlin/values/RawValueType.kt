package com.github.valentinaebi.capybara.values

import com.github.valentinaebi.capybara.values.RawValueType.Int32
import org.objectweb.asm.Type.*

enum class RawValueType(val size: Int) {
    Int32(1),
    Long(2),
    Float(1),
    Double(2),
    Reference(1),
    Placeholder(1);

    companion object {

        fun fromAsmSort(sort: Int): RawValueType {
            return when (sort) {
                BOOLEAN, CHAR, BYTE, SHORT, INT -> Int32
                LONG -> Long
                FLOAT -> Float
                DOUBLE -> Double
                ARRAY, OBJECT, METHOD -> Reference
                else -> throw IllegalArgumentException("unexpected sort: $sort")
            }
        }

        fun fromDescriptor(descriptor: String): RawValueType {
            val sort = getType(descriptor).sort
            return fromAsmSort(sort)
        }

    }

}

fun valueForType(rawValueType: RawValueType, value: Number): ProgramValue {
    return when (rawValueType) {
        Int32 -> ConcreteInt32BitsValue(value.toInt())
        RawValueType.Long -> ConcreteLongValue(value.toLong())
        RawValueType.Float -> ConcreteFloatValue(value.toFloat())
        RawValueType.Double -> ConcreteDoubleValue(value.toDouble())
        else -> throw IllegalArgumentException("unexpected: $rawValueType")
    }
}
