package com.github.valentinaebi.capybara.execution

import com.github.valentinaebi.capybara.values.Add
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

fun ProgramValue.negated(): ProgramValue {
    return when (this) {
        is ConcreteInt32BitsValue -> ConcreteInt32BitsValue(-this.value)
        is ConcreteLongValue -> ConcreteLongValue(-this.value)
        is ConcreteFloatValue -> ConcreteFloatValue(-this.value)
        is ConcreteDoubleValue -> ConcreteDoubleValue(-this.value)
        else -> if (this is Negated) this.negated else Negated(this)
    }
}

fun ProgramValue.plus(r: ProgramValue): ProgramValue {
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
        r is Negated -> l.minus(r.negated)
        else -> Add(l, r)
    }
}

fun ProgramValue.minus(r: ProgramValue): ProgramValue {
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
        l.hasValue(0) -> r.negated()
        r.hasValue(0) -> l
        r is Negated -> l.plus(r.negated)
        else -> Sub(l, r)
    }
}

fun ProgramValue.times(r: ProgramValue): ProgramValue {
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
        l.hasValue(-1) -> r.negated()
        r.hasValue(-1) -> l.negated()
        else -> Mul(l, r)
    }
}

fun ProgramValue.divBy(r: ProgramValue): ProgramValue {
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
        r.hasValue(-1) -> l.negated()
        l == r -> valueForType(l.rawValueType, 1)
        else -> Div(l, r)
    }
}


private fun ProgramValue.hasValue(n: Number): Boolean = this is ConcreteValue && this.value == n

private fun valueForType(rawValueType: RawValueType, value: Number): ProgramValue {
    return when (rawValueType) {
        Int32 -> ConcreteInt32BitsValue(value.toInt())
        Long -> ConcreteLongValue(value.toLong())
        Float -> ConcreteFloatValue(value.toFloat())
        Double -> ConcreteDoubleValue(value.toDouble())
        else -> throw IllegalArgumentException("unexpected: $rawValueType")
    }
}

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
