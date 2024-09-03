package com.github.valentinaebi.capybara.repositories

import com.github.valentinaebi.capybara.values.ProgramValue

class StringsRepository {
    private val strings: MutableMap<ProgramValue, String> = mutableMapOf()

    fun addString(v: ProgramValue, str: String){
        require(strings[v] == null || strings[v] == str)
        strings[v] = str
    }

    fun getStringValueOf(v: ProgramValue): String? = strings[v]

}
