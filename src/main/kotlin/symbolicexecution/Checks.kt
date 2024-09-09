package com.github.valentinaebi.capybara.symbolicexecution

import com.github.valentinaebi.capybara.NULL_POINTER_EXCEPTION
import com.github.valentinaebi.capybara.checks.Reporter
import com.github.valentinaebi.capybara.solving.Solver
import com.github.valentinaebi.capybara.values.ReferenceValue

class Checker(
    private val reporter: Reporter,
    private val solver: Solver
) {

    fun mustBeNonNull(
        value: ReferenceValue,
        msg: String
    ) {
        if (solver.canProveIsNull(value)) {
            reporter.report(msg)
            throw ThrowEvent(NULL_POINTER_EXCEPTION)
        }
    }

}
