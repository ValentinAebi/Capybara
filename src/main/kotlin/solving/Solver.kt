package com.github.valentinaebi.capybara.solving

import com.github.valentinaebi.capybara.values.Int32Value
import com.github.valentinaebi.capybara.values.ReferenceValue
import com.github.valentinaebi.capybara.values.ValuesCreator
import io.ksmt.KContext
import io.ksmt.expr.KExpr
import io.ksmt.expr.KUniversalQuantifier
import io.ksmt.solver.KSolverStatus
import io.ksmt.solver.z3.KZ3Solver
import io.ksmt.sort.KBoolSort
import kotlin.time.Duration.Companion.seconds

class Solver(private val ctx: KContext, private val valuesCreator: ValuesCreator) {
    private val timeout = 1.seconds
    private val ksmtSolver = KZ3Solver(ctx)

    init {
        val arrayLenIsNonNeg = forallArrayLengthIsAtLeastZero()
        ksmtSolver.assert(arrayLenIsNonNeg)
        ksmtSolver.push()
    }

    var pushLevel = 0
        private set

    fun saveArrayLength(array: ReferenceValue, length: Int32Value) {
        with(valuesCreator) {
            ksmtSolver.assert(arrayLen(array) eq length)
        }
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

    fun canProveWrong(formula: KExpr<KBoolSort>): Boolean {
        val solverResponse = ksmtSolver.checkWithAssumptions(listOf(formula), timeout)
        return solverResponse == KSolverStatus.UNSAT
    }

    private fun forallArrayLengthIsAtLeastZero(): KUniversalQuantifier = with(valuesCreator) {
        val arrayVarName = "a"
        val arrayDecl = ctx.mkConstDecl(arrayVarName, ctx.bv32Sort)
        val arrayExpr = ctx.mkConst(arrayVarName, ctx.bv32Sort)
        val arrayLenIsNonNeg = ctx.mkUniversalQuantifier(
            ctx.mkNot(isLessThanZeroFormula(arrayLen(ReferenceValue(arrayExpr)))),
            listOf(arrayDecl)
        )
        return arrayLenIsNonNeg
    }

}
