package com.github.valentinaebi.capybara.checks

import com.github.valentinaebi.capybara.programstruct.Class
import com.github.valentinaebi.capybara.programstruct.Method

class Reporter {
    private val issues: MutableList<Issue> = mutableListOf()

    var currentClass: Class? = null
    var currentMethod: Method? = null
    var currentLine: Int? = null

    fun report(description: String) {
        issues.add(Issue(currentClass!!, currentMethod!!, currentLine, description))
    }

}
