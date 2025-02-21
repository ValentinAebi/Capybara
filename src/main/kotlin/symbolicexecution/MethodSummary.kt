package com.github.valentinaebi.capybara.symbolicexecution

import com.github.valentinaebi.capybara.programstruct.Method
import com.github.valentinaebi.capybara.values.ProgramValue
import io.ksmt.expr.KExpr
import io.ksmt.sort.KBoolSort

class MethodSummary private constructor(
    val method: Method,
    val params: List<ProgramValue>,
    val formulas: Array<KExpr<KBoolSort>>,
    val results: Array<MethodResult>
) {

    constructor(
        method: Method,
        params: List<ProgramValue>,
        resultsList: List<Pair<KExpr<KBoolSort>, MethodResult>>
    ) : this(
        method,
        params,
        resultsList.map { it.first }.toTypedArray(),
        resultsList.map { it.second }.toTypedArray()
    )

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("Summary of ").append(method.methodName).append(":\n¦   params = [ ")
        for (param in params) {
            sb.append(param.ksmtValue)
            sb.append(" ")
        }
        sb.append("]\n")
        for ((formula, result) in formulas.zip(results)) {
            sb.append("¦ ").append(formula).append(" -> ").append(result).append("\n")
        }
        return sb.toString()
    }

}

sealed interface MethodResult {
    fun subst(substF: (ProgramValue) -> ProgramValue): MethodResult
}

data class ReturnResult(val returnedValue: ProgramValue?) : MethodResult {
    override fun subst(substF: (ProgramValue) -> ProgramValue): ReturnResult = ReturnResult(returnedValue?.let(substF))
    override fun toString(): String = "return ${returnedValue?.ksmtValue ?: "<void>"}"
}

data class ThrowResult(val thrownException: ProgramValue) : MethodResult {
    override fun subst(substF: (ProgramValue) -> ProgramValue): ThrowResult = ThrowResult(thrownException.let(substF))
    override fun toString(): String = "throw ${thrownException.ksmtValue}"
}
