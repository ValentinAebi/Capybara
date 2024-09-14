package com.github.valentinaebi.capybara.checks

import com.github.valentinaebi.capybara.solving.Solver
import com.github.valentinaebi.capybara.symbolicexecution.ThrowEvent
import com.github.valentinaebi.capybara.values.Int32Value
import com.github.valentinaebi.capybara.values.NumericValue
import com.github.valentinaebi.capybara.values.ProgramValue
import com.github.valentinaebi.capybara.values.ReferenceValue

class Checker(
    private val reporter: Reporter,
    private val solver: Solver
) {

    fun mustBeNonNull(value: ReferenceValue, check: Check) {
        reportAndThrowIf(solver.canProveIsNull(value), check)
    }

    fun divisorMustNotBeZero(divisor: NumericValue<*>) {
        reportAndThrowIf(solver.canProveIsZero(divisor), Check.DIV_BY_ZERO)
    }

    fun arrayIndexingPrecondition(array: ProgramValue, idx: ProgramValue) {
        mustBeNonNull(array.ref(), Check.INDEXING_NULL_ARRAY)
        arrayIndexMustBeInBounds(array.ref(), idx.int32())
    }

    fun arrayLenMustBeNonNegative(len: Int32Value) {
        reportAndThrowIf(solver.canProveStrictlyNegative(len), Check.NEG_ARRAY_LEN)
    }

    private fun arrayIndexMustBeInBounds(array: ReferenceValue, index: Int32Value) {
        reportAndThrowIf(solver.canProveIsOutOfBounds(array, index), Check.ARRAY_INDEX_OUT)
    }

    private fun reportAndThrowIf(errorCond: Boolean, check: Check) {
        if (errorCond) {
            reporter.report(check)
            throw ThrowEvent(check.exception)
        }
    }

}
