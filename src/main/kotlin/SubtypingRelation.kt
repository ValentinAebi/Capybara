package com.github.valentinaebi.capybara

import org.objectweb.asm.Type
import java.util.LinkedList

typealias SubtypingRelationBuilder = MutableMap<InternalName, MutableSet<InternalName>>

private const val primitiveDescriptors: String = "VZCBSIFJD"

fun Type.subtypeOf(superT: Type, subtypingRelation: SubtypingRelation): Boolean {
    return this.descriptor.subtypeOf(superT.descriptor, subtypingRelation)
}

fun InternalName.subtypeOf(superT: InternalName, subtypingRelation: SubtypingRelation): Boolean {
    return subtypingRelation.isSubtype(this, superT)
}

class SubtypingRelation(private val superTypes: Map<InternalName, Set<InternalName>>) {
    private val cache: MutableMap<Query, Boolean> = mutableMapOf()
    private val cacheQuery = Query("", "")

    fun isSubtype(subtype: InternalName, superType: InternalName): Boolean {
        cacheQuery.subT = subtype
        cacheQuery.superT = superType
        cache[cacheQuery]?.let { return it }
        if (
            subtype.length == 1 && subtype[0] in primitiveDescriptors
            || superType.length == 1 && superType[0] in primitiveDescriptors
        ) {
            return false
        }
        val workList = LinkedList<InternalName>()
        workList.addLast(subtype)
        while (workList.isNotEmpty()) {
            val currT = workList.removeFirst()
            if (currT == superType) {
                cache[Query(subtype, superType)] = true
                return true
            }
        }
        cache[Query(subtype, superType)] = false
        return false
    }

    private data class Query(var subT: InternalName, var superT: InternalName)

}
