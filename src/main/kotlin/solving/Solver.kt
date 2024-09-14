package com.github.valentinaebi.capybara.solving

import com.github.valentinaebi.capybara.values.Int32Value
import com.github.valentinaebi.capybara.values.NumericValue
import com.github.valentinaebi.capybara.values.ReferenceValue
import com.github.valentinaebi.capybara.values.ValuesCreator
import io.ksmt.KContext
import io.ksmt.expr.KExpr
import io.ksmt.solver.KSolverStatus
import io.ksmt.solver.z3.KZ3Solver
import io.ksmt.sort.KBoolSort
import kotlin.time.Duration.Companion.seconds

class Solver(private val ctx: KContext, private val valuesCreator: ValuesCreator) {
    private val timeout = 1.seconds
    private val ksmtSolver = KZ3Solver(ctx)

    init {
        with(valuesCreator) {
            val arrayVarName = "a"
            val arrayDecl = ctx.mkConstDecl(arrayVarName, ctx.bv32Sort)
            val arrayExpr = ctx.mkConst(arrayVarName, ctx.bv32Sort)
            val arrayLenIsNonNeg = ctx.mkUniversalQuantifier(
                ctx.mkNot(isLessThanZeroFormula(arrayLen(ReferenceValue(arrayExpr)))),
                listOf(arrayDecl)
            )
            ksmtSolver.assert(arrayLenIsNonNeg)
            ksmtSolver.push()
        }
    }

    var pushLevel = 0
        private set

    fun canProveIsNull(v: ReferenceValue): Boolean = with(valuesCreator) {
        val formula = ctx.mkNot(v eq valuesCreator.nullValue)
        val status = ksmtSolver.checkWithAssumptions(listOf(formula), timeout)
        status == KSolverStatus.UNSAT
    }

    fun canProveIsZero(v: NumericValue<*>): Boolean = canProve(true, valuesCreator.isZeroFormula(v))

    fun canProveIsOutOfBounds(array: ReferenceValue, idx: Int32Value): Boolean {
        return with(ctx) {
            with(valuesCreator) {
                val inBoundsFormula = mkAnd(
                    lessThanOrEqualToFormula(zero_int, idx),
                    lessThanFormula(idx, arrayLen(array))
                )
                canProve(false, inBoundsFormula)
            }
        }
    }

    fun canProveStrictlyNegative(value: Int32Value): Boolean =
        canProve(true, valuesCreator.isLessThanZeroFormula(value))

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

    private fun canProve(desiredResult: Boolean, formula: KExpr<KBoolSort>): Boolean {
        var smtInputFormula = formula
        if (desiredResult) {
            smtInputFormula = ctx.mkNot(smtInputFormula)
        }
        val solverResponse = ksmtSolver.checkWithAssumptions(listOf(smtInputFormula), timeout)
        return solverResponse == KSolverStatus.UNSAT
    }

}
