package com.github.valentinaebi.capybara.checks

import com.github.valentinaebi.capybara.TypeDescriptor
import com.github.valentinaebi.capybara.repositories.TypesRepository
import com.github.valentinaebi.capybara.values.ProgramValue

fun checkCastPrecondition(
    value: ProgramValue,
    desiredType: TypeDescriptor,
    typesRepository: TypesRepository,
    reporter: Reporter
): TypeDescriptor? /* exception type */ {
    if (typesRepository.canProveIsNot(value, desiredType)) {
        val actualType = typesRepository.getExactType(value)!!
        reporter.report("cast may fail: casted value may have type $actualType")
        return "java/lang/ClassCastException"
    }
    return null
}
