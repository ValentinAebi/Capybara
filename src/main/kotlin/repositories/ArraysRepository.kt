package com.github.valentinaebi.capybara.repositories

import com.github.valentinaebi.capybara.values.ProgramValue

class ArraysRepository {
    private val arraysToLen: MutableMap<ProgramValue, ProgramValue> = mutableMapOf()

    fun saveArrayOfLen(array: ProgramValue, len: ProgramValue) {
        arraysToLen[array] = len
    }

    fun lengthOf(array: ProgramValue): ProgramValue? = arraysToLen[array]

    fun markAsLeaked(array: ProgramValue) {
        // do nothing as long as array cells are not recorded for owned arrays
    }

}
