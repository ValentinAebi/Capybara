import com.github.valentinaebi.capybara.checking.Issue
import com.github.valentinaebi.capybara.checking.Check

data class IssueMatcher(
    val srcFileName: String,
    val line: Int,
    val check: Check
) {

    fun matches(issue: Issue): Boolean =
        issue.clazz.srcFileName == srcFileName
                && issue.line == line
                && issue.check == check

    override fun toString(): String = "[name=$srcFileName ; line=$line ; check=$check]"

}
