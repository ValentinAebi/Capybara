package com.github.valentinaebi.capybara.repositories

import com.github.valentinaebi.capybara.values.AtomicSymbolicValue
import com.github.valentinaebi.capybara.values.ConcreteValue
import com.github.valentinaebi.capybara.values.ProgramValue

class ConstraintsRepository {
    private val equivClasses: MutableMap<ProgramValue, EquivClass> = mutableMapOf()

    fun addEqualityConstraint(v1: ProgramValue, v2: ProgramValue): Boolean {
        TODO()
    }

    private data class EquivClass(
        val concreteValue: ConcreteValue?,
        val atomicSymbolicValues: Set<AtomicSymbolicValue>,
        val complexValues: Set<ProgramValue>,
        val distinctEquivClasses: Set<EquivClass>
    )

}
