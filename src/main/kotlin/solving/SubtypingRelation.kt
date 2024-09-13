package com.github.valentinaebi.capybara.solving

import com.github.valentinaebi.capybara.InternalName
import org.objectweb.asm.Type
import java.util.LinkedList

typealias SubtypingRelationBuilder = MutableMap<InternalName, MutableSet<InternalName>>

private const val primitiveDescriptors: String = "VZCBSIFJD"

class SubtypingRelation(private val superTypes: Map<InternalName, Set<InternalName>>) {
    private val cache: MutableMap<Query, Boolean> = mutableMapOf()
    private val cacheQuery = Query("", "")

    fun Type.subtypeOf(superT: Type): Boolean {
        return this.descriptor.subtypeOf(superT.descriptor)
    }

    fun InternalName.subtypeOf(superType: InternalName): Boolean {
        val subtype = this

        if (subtype == superType){
            return true
        }

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
            superTypes[currT]?.let { workList.addAll(it) }
        }
        cache[Query(subtype, superType)] = false
        return false
    }

    private data class Query(var subT: InternalName, var superT: InternalName)

}
