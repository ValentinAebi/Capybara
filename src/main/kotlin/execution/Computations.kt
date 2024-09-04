package com.github.valentinaebi.capybara.execution

import com.github.valentinaebi.capybara.repositories.ConstraintsRepository
import com.github.valentinaebi.capybara.values.Add
import com.github.valentinaebi.capybara.values.AtomicSymbolicValue
import com.github.valentinaebi.capybara.values.ConcreteDoubleValue
import com.github.valentinaebi.capybara.values.ConcreteFloatValue
import com.github.valentinaebi.capybara.values.ConcreteInt32BitsValue
import com.github.valentinaebi.capybara.values.ConcreteLongValue
import com.github.valentinaebi.capybara.values.ConcreteValue
import com.github.valentinaebi.capybara.values.Div
import com.github.valentinaebi.capybara.values.Mul
import com.github.valentinaebi.capybara.values.Negated
import com.github.valentinaebi.capybara.values.ProgramValue
import com.github.valentinaebi.capybara.values.RawValueType
import com.github.valentinaebi.capybara.values.RawValueType.Double
import com.github.valentinaebi.capybara.values.RawValueType.Float
import com.github.valentinaebi.capybara.values.RawValueType.Int32
import com.github.valentinaebi.capybara.values.RawValueType.Long
import com.github.valentinaebi.capybara.values.Sub
import com.github.valentinaebi.capybara.values.valueForType

operator fun ProgramValue.unaryMinus(): ProgramValue {
    return when (this) {
        is ConcreteInt32BitsValue -> ConcreteInt32BitsValue(-this.value)
        is ConcreteLongValue -> ConcreteLongValue(-this.value)
        is ConcreteFloatValue -> ConcreteFloatValue(-this.value)
        is ConcreteDoubleValue -> ConcreteDoubleValue(-this.value)
        else -> if (this is Negated) this.negated else Negated(this)
    }
}

operator fun ProgramValue.plus(r: ProgramValue): ProgramValue {
    val l = this
    require(l.rawValueType == r.rawValueType)
    if (l is ConcreteValue && r is ConcreteValue) {
        return performBinaryOp(
            { a, b -> a + b },
            { a, b -> a + b },
            { a, b -> a + b },
            { a, b -> a + b },
            l, r
        )
    }
    return when {
        l.hasValue(0) -> r
        r.hasValue(0) -> l
        r is Negated -> l - r.negated
        else -> Add(l, r)
    }
}

operator fun ProgramValue.minus(r: ProgramValue): ProgramValue {
    val l = this
    require(l.rawValueType == r.rawValueType)
    if (l is ConcreteValue && r is ConcreteValue) {
        return performBinaryOp(
            { a, b -> a - b },
            { a, b -> a - b },
            { a, b -> a - b },
            { a, b -> a - b },
            l, r
        )
    }
    return when {
        l == r -> valueForType(l.rawValueType, 0)
        l.hasValue(0) -> -r
        r.hasValue(0) -> l
        r is Negated -> l + r.negated
        else -> Sub(l, r)
    }
}

operator fun ProgramValue.times(r: ProgramValue): ProgramValue {
    val l = this
    require(l.rawValueType == r.rawValueType)
    if (l is ConcreteValue && r is ConcreteValue) {
        return performBinaryOp(
            { a, b -> a * b },
            { a, b -> a * b },
            { a, b -> a * b },
            { a, b -> a * b },
            l, r
        )
    }
    return when {
        r.hasValue(0) -> r
        l.hasValue(0) -> l
        l.hasValue(1) -> r
        r.hasValue(1) -> l
        l.hasValue(-1) -> -r
        r.hasValue(-1) -> -l
        else -> Mul(l, r)
    }
}

operator fun ProgramValue.div(r: ProgramValue): ProgramValue {
    val l = this
    require(l.rawValueType == r.rawValueType)
    if (l is ConcreteValue && r is ConcreteValue) {
        return performBinaryOp(
            { a, b -> a / b },
            { a, b -> a / b },
            { a, b -> a / b },
            { a, b -> a / b },
            l, r
        )
    }
    return when {
        l.hasValue(0) || r.hasValue(1) -> l
        r.hasValue(-1) -> -l
        l == r -> valueForType(l.rawValueType, 1)
        else -> Div(l, r)
    }
}

fun mkInt32(v: ProgramValue, constraintsRepository: ConstraintsRepository): ProgramValue =
    convert(Int32, v, constraintsRepository) {
        ConcreteInt32BitsValue(it.toInt())
    }

fun mkLong(v: ProgramValue, constraintsRepository: ConstraintsRepository): ProgramValue =
    convert(Long, v, constraintsRepository) {
        ConcreteLongValue(it.toLong())
    }

fun mkFloat(v: ProgramValue, constraintsRepository: ConstraintsRepository): ProgramValue =
    convert(Float, v, constraintsRepository) {
        ConcreteFloatValue(it.toFloat())
    }

fun mkDouble(v: ProgramValue, constraintsRepository: ConstraintsRepository): ProgramValue =
    convert(Double, v, constraintsRepository) {
        ConcreteDoubleValue(it.toDouble())
    }

private inline fun convert(
    rawValueType: RawValueType,
    v: ProgramValue,
    constraintsRepository: ConstraintsRepository,
    valueCreator: (Number) -> ProgramValue
): ProgramValue {
    if (v is ConcreteValue) {
        return valueCreator(v.value)
    }
    val newValue = AtomicSymbolicValue(rawValueType)
    constraintsRepository.addEqualityConstraint(newValue, v)
    return newValue
}


private fun ProgramValue.hasValue(n: Number): Boolean = this is ConcreteValue && this.value == n

private inline fun performBinaryOp(
    int32Func: (Int, Int) -> Int,
    longFunc: (Long, Long) -> Long,
    floatFunc: (Float, Float) -> Float,
    doubleFunc: (Double, Double) -> Double,
    l: ConcreteValue,
    r: ConcreteValue
): ConcreteValue {
    return when {
        l is ConcreteInt32BitsValue && r is ConcreteInt32BitsValue ->
            ConcreteInt32BitsValue(int32Func(l.value, r.value))

        l is ConcreteLongValue && r is ConcreteLongValue ->
            ConcreteLongValue(longFunc(l.value, r.value))

        l is ConcreteFloatValue && r is ConcreteFloatValue ->
            ConcreteFloatValue(floatFunc(l.value, r.value))

        l is ConcreteDoubleValue && r is ConcreteDoubleValue ->
            ConcreteDoubleValue(doubleFunc(l.value, r.value))

        else -> throw IllegalArgumentException("unexpected: ${l.javaClass.name} op ${r.javaClass.name}")
    }
}
