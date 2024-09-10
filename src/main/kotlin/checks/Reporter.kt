package com.github.valentinaebi.capybara.checks

import com.github.valentinaebi.capybara.UNKNOWN_LINE_NUMBER
import com.github.valentinaebi.capybara.programstruct.Class
import com.github.valentinaebi.capybara.programstruct.Method
import com.github.valentinaebi.capybara.symbolicexecution.Check

class Reporter {
    private val issues: MutableSet<Issue> = mutableSetOf()

    var currentClass: Class? = null
    var currentMethod: Method? = null
    var currentLine: Int = UNKNOWN_LINE_NUMBER

    fun report(check: Check) {
        issues.add(
            Issue(
                currentClass!!,
                currentMethod!!,
                currentLine,
                check
            )
        )
    }

    fun dumpIssues(consumer: (Issue) -> Unit) {
        for (issue in issues.toList().sortedWith(IssuesComparator)) {
            consumer(issue)
        }
    }

    private object IssuesComparator : Comparator<Issue> {
        override fun compare(
            issue1: Issue,
            issue2: Issue
        ): Int {

            fun compareUsing(compFunc: (Issue) -> Comparable<*>): Int? =
                compareBy(compFunc).compare(issue1, issue2).takeIf { it != 0 }

            return compareUsing { it.clazz.srcFileName }
                ?: compareUsing { it.clazz.className }
                ?: compareUsing { it.line }
                ?: compareUsing { it.check.ordinal }
                ?: 0
        }
    }

}
