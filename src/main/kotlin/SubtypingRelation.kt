package com.github.valentinaebi.capybara

import org.objectweb.asm.Type
import java.util.LinkedList

private const val primitiveDescriptors: String = "VZCBSIFJD"

fun Type.subtypeOf(superT: Type, subtypingRelation: SubtypingRelation): Boolean {
    return this.descriptor.subtypeOf(superT.descriptor, subtypingRelation)
}

fun TypeDescriptor.subtypeOf(superT: TypeDescriptor, subtypingRelation: SubtypingRelation): Boolean {
    return subtypingRelation.isSubtype(this, superT)
}

class SubtypingRelation(private val superTypes: Map<TypeDescriptor, Set<TypeDescriptor>>) {
    private val cache: MutableMap<Query, Boolean> = mutableMapOf()
    private val cacheQuery = Query("", "")

    fun isSubtype(subtype: TypeDescriptor, superType: TypeDescriptor): Boolean {
        cacheQuery.subT = subtype
        cacheQuery.superT = superType
        cache[cacheQuery]?.let { return it }
        if (
            subtype.length == 1 && subtype[0] in primitiveDescriptors
            || superType.length == 1 && superType[0] in primitiveDescriptors
        ) {
            return false
        }
        val workList = LinkedList<TypeDescriptor>()
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

    private data class Query(var subT: TypeDescriptor, var superT: TypeDescriptor)

}
