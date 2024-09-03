package com.github.valentinaebi.capybara.repositories

import com.github.valentinaebi.capybara.values.ProgramValue

class OwnedObjectsRepository {
    private val objects: MutableMap<ProgramValue, MutableMap<String, ProgramValue>> = mutableMapOf()

    fun registerNewObject(obj: ProgramValue) {
        require(obj !in objects)
        objects[obj] = mutableMapOf()
    }

    fun markAsLeaked(obj: ProgramValue) {
        objects.remove(obj)
    }

    fun isCurrentlyOwned(obj: ProgramValue) = obj in objects

    fun saveIfOwned(obj: ProgramValue, fieldName: String, value: ProgramValue) {
        objects[obj]?.let {
            it[fieldName] = value
        }
    }

    fun getFieldValue(obj: ProgramValue, fieldName: String): ProgramValue? =
        objects[obj]?.let { it[fieldName] }

}
