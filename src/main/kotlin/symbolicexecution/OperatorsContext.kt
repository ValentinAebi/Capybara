package com.github.valentinaebi.capybara.symbolicexecution

import com.github.valentinaebi.capybara.values.DoubleValue
import com.github.valentinaebi.capybara.values.FloatValue
import com.github.valentinaebi.capybara.values.Int32Value
import com.github.valentinaebi.capybara.values.LongValue
import com.github.valentinaebi.capybara.values.ProgramValue
import io.ksmt.KContext
import io.ksmt.expr.KExpr
import io.ksmt.expr.KFpRoundingMode
import io.ksmt.sort.KFp32Sort
import io.ksmt.sort.KFp64Sort
import io.ksmt.sort.KIntSort

private typealias BinOp<S> = (KExpr<S>, KExpr<S>) -> KExpr<S>

class OperatorsContext(private val ctx: KContext) {
    private val roundingModeExpr = ctx.mkFpRoundingModeExpr(KFpRoundingMode.RoundNearestTiesToEven)

    operator fun Int32Value.unaryMinus(): Int32Value = with(ctx) { Int32Value(-ksmtValue) }
    operator fun LongValue.unaryMinus(): LongValue = with(ctx) { LongValue(-ksmtValue) }
    operator fun FloatValue.unaryMinus(): FloatValue = FloatValue(ctx.mkFpNegationExpr(ksmtValue))
    operator fun DoubleValue.unaryMinus(): DoubleValue = DoubleValue(ctx.mkFpNegationExpr(ksmtValue))

    operator fun Int32Value.plus(r: Int32Value): Int32Value = int32Binop(this, r) { a, b ->
        with(ctx) { a + b }
    }

    operator fun LongValue.plus(r: LongValue): LongValue = longBinop(this, r) { a, b ->
        with(ctx) { a + b }
    }

    operator fun FloatValue.plus(r: FloatValue): FloatValue = floatBinop(this, r) { a, b ->
        ctx.mkFpAddExpr(roundingModeExpr, a, b)
    }

    operator fun DoubleValue.plus(r: DoubleValue): DoubleValue = doubleBinop(this, r) { a, b ->
        ctx.mkFpAddExpr(roundingModeExpr, a, b)
    }

    operator fun Int32Value.minus(r: Int32Value): Int32Value = int32Binop(this, r) { a, b ->
        with(ctx) { a - b }
    }

    operator fun LongValue.minus(r: LongValue): LongValue = longBinop(this, r) { a, b ->
        with(ctx) { a - b }
    }

    operator fun FloatValue.minus(r: FloatValue): FloatValue = floatBinop(this, r) { a, b ->
        ctx.mkFpSubExpr(roundingModeExpr, a, b)
    }

    operator fun DoubleValue.minus(r: DoubleValue): DoubleValue = doubleBinop(this, r) { a, b ->
        ctx.mkFpSubExpr(roundingModeExpr, a, b)
    }

    operator fun Int32Value.times(r: Int32Value): Int32Value = int32Binop(this, r) { a, b ->
        with(ctx) { a * b }
    }

    operator fun LongValue.times(r: LongValue): LongValue = longBinop(this, r) { a, b ->
        with(ctx) { a * b }
    }

    operator fun FloatValue.times(r: FloatValue): FloatValue = floatBinop(this, r) { a, b ->
        ctx.mkFpMulExpr(roundingModeExpr, a, b)
    }

    operator fun DoubleValue.times(r: DoubleValue): DoubleValue = doubleBinop(this, r) { a, b ->
        ctx.mkFpMulExpr(roundingModeExpr, a, b)
    }

    operator fun Int32Value.div(r: Int32Value): Int32Value = int32Binop(this, r) { a, b ->
        with(ctx) { a / b }
    }

    operator fun LongValue.div(r: LongValue): LongValue = longBinop(this, r) { a, b ->
        with(ctx) { a / b }
    }

    operator fun FloatValue.div(r: FloatValue): FloatValue = floatBinop(this, r) { a, b ->
        ctx.mkFpDivExpr(roundingModeExpr, a, b)
    }

    operator fun DoubleValue.div(r: DoubleValue): DoubleValue = doubleBinop(this, r) { a, b ->
        ctx.mkFpDivExpr(roundingModeExpr, a, b)
    }

    operator fun Int32Value.rem(r: Int32Value): Int32Value = int32Binop(this, r, ctx::mkIntRem)

    operator fun LongValue.rem(r: LongValue): LongValue = longBinop(this, r, ctx::mkIntRem)

    operator fun FloatValue.rem(r: FloatValue): FloatValue = floatBinop(this, r, ctx::mkFpRemExpr)

    operator fun DoubleValue.rem(r: DoubleValue): DoubleValue = doubleBinop(this, r, ctx::mkFpRemExpr)

    private inline fun int32Binop(l: Int32Value, r: Int32Value, f: BinOp<KIntSort>): Int32Value =
        Int32Value(f(l.ksmtValue, r.ksmtValue))

    private inline fun longBinop(l: LongValue, r: LongValue, f: BinOp<KIntSort>): LongValue =
        LongValue(f(l.ksmtValue, r.ksmtValue))

    private inline fun floatBinop(l: FloatValue, r: FloatValue, f: BinOp<KFp32Sort>): FloatValue =
        FloatValue(f(l.ksmtValue, r.ksmtValue))

    private inline fun doubleBinop(l: DoubleValue, r: DoubleValue, f: BinOp<KFp64Sort>): DoubleValue =
        DoubleValue(f(l.ksmtValue, r.ksmtValue))

}
