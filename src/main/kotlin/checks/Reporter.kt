package com.github.valentinaebi.capybara.checks

import com.github.valentinaebi.capybara.UNKNOWN_LINE_NUMBER
import com.github.valentinaebi.capybara.programstruct.Class
import com.github.valentinaebi.capybara.programstruct.Method
import java.io.PrintStream

class Reporter {
    private val issues: LinkedHashSet<Issue> = linkedSetOf()

    var currentClass: Class? = null
    var currentMethod: Method? = null
    var currentLine: Int = UNKNOWN_LINE_NUMBER

    fun report(description: String) {
        issues.add(Issue(currentClass!!, currentMethod!!, currentLine, description))
    }

    fun printReport(printStream: PrintStream) {
        for (issue in issues.toList().sortedWith(IssuesComparator)) {
            printStream.println(issue)
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
                ?: compareUsing { it.description }
                ?: 0
        }
    }

}
