package com.github.valentinaebi.capybara.solving

import com.github.valentinaebi.capybara.values.ReferenceValue
import com.github.valentinaebi.capybara.values.ValuesCreator
import io.ksmt.KContext
import io.ksmt.expr.KExpr
import io.ksmt.solver.KSolverStatus
import io.ksmt.solver.cvc5.KCvc5Solver
import io.ksmt.sort.KBoolSort
import kotlin.time.Duration.Companion.seconds

class Solver(private val ctx: KContext, private val valuesCreator: ValuesCreator) {
    private val timeout = 1.seconds
    private val ksmtSolver = KCvc5Solver(ctx)

    var pushLevel = 0
        private set

    fun canProveIsNull(v: ReferenceValue): Boolean = with(valuesCreator) {
        val formula = ctx.mkNot(v eq valuesCreator.nullValue)
        val status = ksmtSolver.checkWithAssumptions(listOf(formula), timeout)
        status == KSolverStatus.UNSAT
    }

    fun push() {
        pushLevel += 1
        ksmtSolver.push()
    }

    fun pop() {
        ksmtSolver.pop()
        pushLevel -= 1
    }

    fun assert(formula: KExpr<KBoolSort>) {
        ksmtSolver.assert(formula)
    }

    fun isConsistent(): Boolean = ksmtSolver.check(timeout) == KSolverStatus.SAT

}
