package com.github.valentinaebi.capybara.checking

import com.github.valentinaebi.capybara.UNKNOWN_LINE_NUMBER
import com.github.valentinaebi.capybara.programstruct.Class
import com.github.valentinaebi.capybara.programstruct.Method

data class Issue(
    val clazz: Class,
    val method: Method,
    val line: Int,
    val check: Check
) {

    override fun toString(): String {
        val lineDescr = if (line == UNKNOWN_LINE_NUMBER) "??" else line
        return "file ${clazz.srcFileName}, line $lineDescr : ${check.msg} ($check)"
    }

}
