package com.github.valentinaebi.capybara.symbolicexecution

import com.github.valentinaebi.capybara.programstruct.Method
import com.github.valentinaebi.capybara.values.ProgramValue
import io.ksmt.expr.KExpr
import io.ksmt.sort.KBoolSort

data class MethodSummary(
    val method: Method,
    val params: List<ProgramValue>,
    val results: Map<KExpr<KBoolSort>, MethodResult>
) {

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("Summary of ").append(method.methodName).append(":\n¦   params = [ ")
        for (param in params) {
            sb.append(param.ksmtValue)
            sb.append(" ")
        }
        sb.append("]\n")
        for ((formula, result) in results) {
            sb.append("¦ ").append(formula).append(" -> ").append(result).append("\n")
        }
        return sb.toString()
    }

}

sealed class MethodResult

data class ReturnResult(val returnedValue: ProgramValue?) : MethodResult() {
    override fun toString(): String = "return ${returnedValue?.ksmtValue ?: "<void>"}"
}

data class ThrowResult(val thrownException: ProgramValue) : MethodResult() {
    override fun toString(): String = "throw ${thrownException.ksmtValue}"
}
