package com.github.valentinaebi.capybara.types

sealed interface Type {

    fun isSubtypeOf(superT: Type, subtypingRelation: SubtypingRelation): Boolean =
        subtypingRelation.isSubtype(this, superT)

}

data class ReferenceType(val fullName: String) : Type
