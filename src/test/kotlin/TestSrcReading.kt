import com.github.valentinaebi.capybara.checking.Check
import java.nio.file.Path
import java.util.regex.Pattern

private val issueMarkerDetector = Pattern.compile("#issue\\[([A-Z0-9_]*)]")

fun readAnnotatedSrcFile(path: Path): List<IssueMatcher> {
    val fileName = path.fileName.toString()
    val file = path.toFile()
    val matchers = mutableListOf<IssueMatcher>()
    var lineIdx = 1
    for (lineContent in file.readLines()) {
        val m = issueMarkerDetector.matcher(lineContent)
        while (m.find()) {
            val check = Check.valueOf(m.group(1))
            matchers.add(IssueMatcher(fileName, lineIdx, check))
        }
        lineIdx += 1
    }
    return matchers
}
