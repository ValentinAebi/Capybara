package com.github.valentinaebi.capybara.symbolicexecution

import com.github.valentinaebi.capybara.values.DoubleValue
import com.github.valentinaebi.capybara.values.FloatValue
import com.github.valentinaebi.capybara.values.Int32Value
import com.github.valentinaebi.capybara.values.LongValue
import com.github.valentinaebi.capybara.values.NumericValue
import com.github.valentinaebi.capybara.values.ProgramValue
import com.github.valentinaebi.capybara.values.ReferenceValue
import io.ksmt.KContext
import io.ksmt.expr.KExpr
import io.ksmt.expr.KFpRoundingMode
import io.ksmt.sort.KFp32Sort
import io.ksmt.sort.KFp64Sort
import io.ksmt.sort.KIntSort

private typealias BinOp<S> = (KExpr<S>, KExpr<S>) -> KExpr<S>

class OperatorsContext(private val ctx: KContext) {
    private val roundingModeExpr = ctx.mkFpRoundingModeExpr(KFpRoundingMode.RoundNearestTiesToEven)

    operator fun ProgramValue.unaryMinus(): ProgramValue = with(ctx) {
        when (this@unaryMinus) {
            is Int32Value -> Int32Value(-ksmtValue)
            is LongValue -> LongValue(-ksmtValue)
            is FloatValue -> FloatValue(mkFpNegationExpr(ksmtValue))
            is DoubleValue -> DoubleValue(mkFpNegationExpr(ksmtValue))
            is ReferenceValue -> throw UnsupportedOperationException("-reference")
        }
    }

    operator fun ProgramValue.plus(r: ProgramValue): ProgramValue = binaryOp(
        this, r,
        { a, b -> with(ctx) { a + b } },
        { a, b -> ctx.mkFpAddExpr(roundingModeExpr, a, b) },
        { a, b -> ctx.mkFpAddExpr(roundingModeExpr, a, b) },
        "+"
    )

    operator fun ProgramValue.minus(r: ProgramValue): ProgramValue = binaryOp(
        this, r,
        { a, b -> with(ctx) { a - b } },
        { a, b -> ctx.mkFpSubExpr(roundingModeExpr, a, b) },
        { a, b -> ctx.mkFpSubExpr(roundingModeExpr, a, b) },
        "-"
    )

    operator fun ProgramValue.times(r: ProgramValue): ProgramValue = binaryOp(
        this, r,
        { a, b -> with(ctx) { a * b } },
        { a, b -> ctx.mkFpMulExpr(roundingModeExpr, a, b) },
        { a, b -> ctx.mkFpMulExpr(roundingModeExpr, a, b) },
        "*"
    )

    operator fun ProgramValue.div(r: ProgramValue): ProgramValue = binaryOp(
        this, r,
        { a, b -> with(ctx) { a / b } },
        { a, b -> ctx.mkFpDivExpr(roundingModeExpr, a, b) },
        { a, b -> ctx.mkFpDivExpr(roundingModeExpr, a, b) },
        "/"
    )

    operator fun ProgramValue.rem(r: ProgramValue): ProgramValue = binaryOp(
        this, r,
        { a, b -> ctx.mkIntRem(a, b) },
        { a, b -> ctx.mkFpRemExpr(a, b) },
        { a, b -> ctx.mkFpRemExpr(a, b) },
        "%"
    )

    private inline fun binaryOp(
        l: ProgramValue, r: ProgramValue,
        intF: BinOp<KIntSort>,
        fp32F: BinOp<KFp32Sort>,
        fp64F: BinOp<KFp64Sort>,
        operatorRepr: String
    ): ProgramValue = when {
        l is Int32Value && r is Int32Value -> Int32Value(intF(l.ksmtValue, r.ksmtValue))
        l is LongValue && r is LongValue -> LongValue(intF(l.ksmtValue, r.ksmtValue))
        l is FloatValue && r is FloatValue -> FloatValue(fp32F(l.ksmtValue, r.ksmtValue))
        l is DoubleValue && r is DoubleValue -> DoubleValue(fp64F(l.ksmtValue, r.ksmtValue))
        else -> throw UnsupportedOperationException("${l::class.simpleName} $operatorRepr ${r::class.simpleName}")
    }

}
