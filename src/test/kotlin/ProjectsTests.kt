import com.github.valentinaebi.capybara.JAVA_LANG_OBJECT
import com.github.valentinaebi.capybara.JAVA_LANG_STRING
import com.github.valentinaebi.capybara.checks.Issue
import com.github.valentinaebi.capybara.checks.Reporter
import com.github.valentinaebi.capybara.loading.readClassFile
import com.github.valentinaebi.capybara.solving.Solver
import com.github.valentinaebi.capybara.solving.SubtypingRelation
import com.github.valentinaebi.capybara.solving.SubtypingRelationBuilder
import com.github.valentinaebi.capybara.symbolicexecution.Checker
import com.github.valentinaebi.capybara.symbolicexecution.Executor
import com.github.valentinaebi.capybara.symbolicexecution.OperatorsContext
import com.github.valentinaebi.capybara.symbolicexecution.SymbolicInterpreter
import com.github.valentinaebi.capybara.values.ValuesCreator
import io.ksmt.KContext
import org.apache.maven.shared.invoker.DefaultInvocationRequest
import org.apache.maven.shared.invoker.DefaultInvoker
import java.io.File
import kotlin.io.path.Path
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

class ProjectsTests {

    @Test
    fun fooClassTest() {
        val projectPath = Path("src", "test", "resources", "testprojects", "TargetedTestCases")
        val pomFilePath = projectPath.resolve("pom.xml")
        val fooSourceFile = projectPath.resolve("src", "main", "java", "Foo.java")
        val fooClassFile = projectPath.resolve("target", "classes", "Foo.class")
        mvnCompile(pomFilePath.toFile())
        val issuesMatchers = readAnnotatedSrcFile(fooSourceFile).toMutableList()

        val subtypeRelBuilder: SubtypingRelationBuilder = mutableMapOf()
        val fooClass = readClassFile(fooClassFile, subtypeRelBuilder)

        val reporter = Reporter()
        reporter.currentClass = fooClass
        val ctx = KContext()
        val valuesCreator = ValuesCreator(ctx)
        val operatorsContext = OperatorsContext(ctx)
        val solver = Solver(ctx, valuesCreator)
        val checker = Checker(reporter, solver)
        val interpreter = SymbolicInterpreter(reporter, valuesCreator, operatorsContext, checker, solver)
        val executor = Executor(interpreter, solver, ctx, valuesCreator, reporter)

        for (method in fooClass.methods.values) {
            method.computeCfg()
            executor.execute(method)
        }

        val issues = mutableListOf<Issue>()
        reporter.dumpIssues { issues.add(it) }

        checkIssuesAgainstMatchers(issuesMatchers, issues)

        val subtypingRelation = SubtypingRelation(subtypeRelBuilder)
        with(subtypingRelation) {
            assertTrue(fooClass.className.subtypeOf(JAVA_LANG_OBJECT))
            assertFalse(fooClass.className.subtypeOf(JAVA_LANG_STRING))
        }
    }

    private fun checkIssuesAgainstMatchers(
        issuesMatchers: MutableList<IssueMatcher>,
        issues: MutableList<Issue>
    ) {
        val sb = StringBuilder()
        for (matcher in issuesMatchers) {
            val matchedIssues = issues.filter { matcher.matches(it) }
            if (matchedIssues.isEmpty()) {
                sb.append("No issue matches matcher $matcher\n")
            } else if (matchedIssues.size > 1) {
                sb.append("Several issues match matcher $matcher: $matchedIssues\n")
            }
            issues.removeAll(matchedIssues)
        }
        if (issues.isNotEmpty()) {
            sb.append("Unexpected issues:\n")
            for (issue in issues) {
                sb.append(" $issue\n")
            }
        }
        if (sb.isNotEmpty()) {
            fail(sb.toString())
        }
    }

    private fun mvnCompile(pomFile: File) {
        val request = DefaultInvocationRequest()
        request.pomFile = pomFile
        request.goals = listOf("compile")
        val invoker = DefaultInvoker()
        val mavenHome = System.getenv("MAVEN_HOME")
        assert(mavenHome != null){ "Please define the MAVEN_HOME environment variable" }
        invoker.mavenHome = File(mavenHome)
        invoker.execute(request)
    }

}