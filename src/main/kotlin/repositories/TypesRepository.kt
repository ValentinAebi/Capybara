package com.github.valentinaebi.capybara.repositories

import com.github.valentinaebi.capybara.SubtypingRelation
import com.github.valentinaebi.capybara.TypeDescriptor
import com.github.valentinaebi.capybara.subtypeOf
import com.github.valentinaebi.capybara.values.ProgramValue

class TypesRepository(private val subtypingRelation: SubtypingRelation) {
    private val types: MutableMap<ProgramValue, TypeInfo> = mutableMapOf()

    fun saveType(value: ProgramValue, typeDescriptor: String, isExactType: Boolean) {
        val priorInfo = types[value]
        if ((priorInfo == null || priorInfo is SuperTypeInfo) && isExactType) {
            types[value] = ExactTypeInfo(typeDescriptor)
        } else if (priorInfo == null) {
            types[value] = SuperTypeInfo(typeDescriptor)
        } else if (priorInfo is SuperTypeInfo) {
            priorInfo.refineAsSubtypeOf(typeDescriptor)
        }
    }

    fun getExactType(value: ProgramValue): TypeDescriptor? {
        val info = types[value]
        return if (info is ExactTypeInfo) info.typeDescriptor else null
    }

    fun canProveIsNot(value: ProgramValue, type: TypeDescriptor): Boolean {
        val info = types[value]
        return if (info is ExactTypeInfo) !info.typeDescriptor.subtypeOf(type, subtypingRelation) else false
    }

    private sealed interface TypeInfo

    private inner class ExactTypeInfo(val typeDescriptor: TypeDescriptor) : TypeInfo

    private inner class SuperTypeInfo(superT: TypeDescriptor) : TypeInfo {
        private val _knownSuperTypes: MutableSet<TypeDescriptor> = mutableSetOf(superT)

        val knownSuperTypes: Set<TypeDescriptor> = _knownSuperTypes

        fun refineAsSubtypeOf(superT: TypeDescriptor) {
            _knownSuperTypes.removeIf { superT.subtypeOf(it, subtypingRelation) }
            _knownSuperTypes.add(superT)
        }

    }

}
