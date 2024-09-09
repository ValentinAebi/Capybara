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
            val classComp = issue1.clazz.className.compareTo(issue2.clazz.className)
            if (classComp != 0) {
                return classComp
            }
            val methodComp = issue1.method.methodName.compareTo(issue2.method.methodName)
            if (methodComp != 0) {
                return methodComp
            }
            val lineComp = issue1.line.compareTo(issue2.line)
            if (lineComp != 0) {
                return lineComp
            }
            return issue1.description.compareTo(issue2.description)
        }
    }

}
