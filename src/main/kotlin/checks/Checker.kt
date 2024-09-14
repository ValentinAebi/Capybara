package com.github.valentinaebi.capybara.checks

import com.github.valentinaebi.capybara.solving.Solver
import com.github.valentinaebi.capybara.symbolicexecution.ThrowEvent
import com.github.valentinaebi.capybara.values.Int32Value
import com.github.valentinaebi.capybara.values.NumericValue
import com.github.valentinaebi.capybara.values.ProgramValue
import com.github.valentinaebi.capybara.values.ReferenceValue
import com.github.valentinaebi.capybara.values.ValuesCreator
import io.ksmt.KContext
import io.ksmt.expr.KExpr
import io.ksmt.sort.KBoolSort

class Checker(
    private val reporter: Reporter,
    private val solver: Solver,
    private val ctx: KContext,
    private val valuesCreator: ValuesCreator
) {

    fun mustBeNonNull(value: ReferenceValue, check: Check) = with(valuesCreator) {
        val nonNullFormula = ctx.mkNot(value eq valuesCreator.nullValue)
        mustValidate(nonNullFormula, check)
    }

    fun divisorMustNotBeZero(divisor: NumericValue<*>) {
        val isNotZeroFormula = ctx.mkNot(valuesCreator.isZeroFormula(divisor))
        mustValidate(isNotZeroFormula, Check.DIV_BY_ZERO)
    }

    fun arrayIndexingPrecondition(array: ProgramValue, idx: ProgramValue) {
        mustBeNonNull(array.ref(), Check.INDEXING_NULL_ARRAY)
        arrayIndexMustBeInBounds(array.ref(), idx.int32())
    }

    fun arrayLenMustBeNonNegative(len: Int32Value) {
        val nonNegFormula = ctx.mkNot(valuesCreator.isLessThanZeroFormula(len))
        mustValidate(nonNegFormula, Check.NEG_ARRAY_LEN)
    }

    private fun arrayIndexMustBeInBounds(array: ReferenceValue, idx: Int32Value) = with(valuesCreator) {
        val inBoundsFormula = ctx.mkAnd(
            lessThanOrEqualToFormula(zero_int, idx),
            lessThanFormula(idx, arrayLen(array))
        )
        mustValidate(inBoundsFormula, Check.ARRAY_INDEX_OUT)
    }

    private fun mustValidate(okFormula: KExpr<KBoolSort>, check: Check) {
        if (solver.canProveWrong(okFormula)) {
            reporter.report(check)
            throw ThrowEvent(check.exception)
        } else {
            solver.assert(okFormula)
        }
    }

}
