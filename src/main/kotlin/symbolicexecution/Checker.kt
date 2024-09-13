package com.github.valentinaebi.capybara.symbolicexecution

import com.github.valentinaebi.capybara.ARRAY_IDX_OUT_OF_BOUNDS_EXCEPTION
import com.github.valentinaebi.capybara.NEG_ARRAY_SIZE_EXCEPTION
import com.github.valentinaebi.capybara.NULL_POINTER_EXCEPTION
import com.github.valentinaebi.capybara.checks.Reporter
import com.github.valentinaebi.capybara.solving.Solver
import com.github.valentinaebi.capybara.values.Int32Value
import com.github.valentinaebi.capybara.values.ProgramValue
import com.github.valentinaebi.capybara.values.ReferenceValue

class Checker(
    private val reporter: Reporter,
    private val solver: Solver
) {

    fun mustBeNonNull(value: ReferenceValue, check: Check) {
        if (solver.canProveIsNull(value)) {
            reporter.report(check)
            throw ThrowEvent(NULL_POINTER_EXCEPTION)
        }
    }

    fun arrayIndexingPrecondition(array: ProgramValue, idx: ProgramValue) {
        mustBeNonNull(array.ref(), Check.INDEXING_NULL_ARRAY)
        arrayIndexMustBeInBounds(array.ref(), idx.int32())
    }

    fun arrayLenMustBeNonNegative(len: Int32Value) {
        if (solver.canProveStrictlyNegative(len)) {
            reporter.report(Check.NEG_ARRAY_LEN)
            throw ThrowEvent(NEG_ARRAY_SIZE_EXCEPTION)
        }
    }

    private fun arrayIndexMustBeInBounds(array: ReferenceValue, index: Int32Value) {
        if (solver.canProveIsOutOfBounds(array, index)) {
            reporter.report(Check.ARRAY_INDEX_OUT)
            throw ThrowEvent(ARRAY_IDX_OUT_OF_BOUNDS_EXCEPTION)
        }
    }

}
