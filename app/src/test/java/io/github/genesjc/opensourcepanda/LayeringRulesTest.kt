package io.github.genesjc.opensourcepanda

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/**
 * The layering, naming, size and honesty rules, checked by reading the source files as text, so they need no
 * library and run with the other unit tests.
 *
 * The layers, innermost first: model (plain types), domain (pure rules), data (repositories, storage,
 * the network client), state (the state holders), and ui. Each may import only what is listed in
 * its rule below.
 *
 * A rule with an allowance list names the files known to break it today. A new violation fails the
 * test; so does an allowance entry that no longer violates, so the lists can only shrink. Do not
 * add to a list to make a failure go away: fix the code.
 */
class LayeringRulesTest {

    private val base = "io.github.genesjc.opensourcepanda"

    private val sources: List<Source> = run {
        val root = listOf(File("src/main/java"), File("app/src/main/java"))
            .first { it.isDirectory }
            .resolve(base.replace('.', '/'))
        root.walk().filter { it.extension == "kt" }
            .map { Source(it.relativeTo(root).invariantSeparatorsPath, it.readText()) }
            .toList()
    }

    private class Source(val path: String, val text: String) {
        val layer: String = if ('/' in path) path.substringBefore('/') else ""
        val imports: List<String> =
            text.lines().filter { it.startsWith("import ") }.map { it.removePrefix("import ").trim() }

        /** Top-level classes, interfaces and objects that are not private. */
        val publicTypes: List<String> =
            TYPE_DECLARATION.findAll(text).map { it.groupValues[1] }.toList()

        val lineCount: Int = text.lines().size

        /** Each function's name and its length in lines, from its `fun` line to the closing brace. Expression-bodied functions are skipped. */
        val functionLengths: List<Pair<String, Int>> by lazy { measureFunctions(text) }

        val isComposableFile: Boolean get() = "@Composable" in text && !path.endsWith("ViewModel.kt")

        companion object {
            private val FUNCTION = Regex("^\\s*(?:(?:private|internal|public|override|inline|suspend|operator|tailrec) )*fun\\s+(?:<[^>]*>\\s*)?([\\w.]+)\\s*\\(")
            private val STRING_LITERAL = Regex("\"(?:[^\"\\\\]|\\\\.)*\"")

            private fun measureFunctions(text: String): List<Pair<String, Int>> {
                val lines = text.lines()
                val found = mutableListOf<Pair<String, Int>>()
                for (start in lines.indices) {
                    val name = FUNCTION.find(lines[start])?.groupValues?.get(1) ?: continue
                    var depth = 0
                    var opened = false
                    var expressionBody = false
                    var end = start
                    while (end < lines.size && end < start + MAX_SCAN_LINES) {
                        val line = lines[end].replace(STRING_LITERAL, "\"\"").substringBefore("//")
                        if (!opened && end > start + 12 && '{' !in line) expressionBody = true
                        for (ch in line) {
                            if (ch == '{') {
                                depth++
                                opened = true
                            } else if (ch == '}') {
                                depth--
                            }
                        }
                        if (opened && depth <= 0) break
                        end++
                    }
                    if (opened && !expressionBody) found += name to (end - start + 1)
                }
                return found
            }

            private const val MAX_SCAN_LINES = 4000

            private val TYPE_DECLARATION = Regex(
                "^(?:@\\w+(?:\\([^)]*\\))?\\s+)*(?:(?:public|internal)\\s+)?" +
                    "(?:(?:data|sealed|enum|abstract|open|annotation|inline|value|fun)\\s+)*" +
                    "(?:class|interface|object)\\s+(\\w+)",
                RegexOption.MULTILINE,
            )
        }
    }

    private fun inLayer(layer: String) = sources.filter { it.layer == layer }

    private fun importsOf(layer: String, forbidden: (String) -> Boolean): Set<String> =
        inLayer(layer).filter { s -> s.imports.any(forbidden) }.map { it.path }.toSet()

    private fun check(rule: String, violations: Set<String>, allowed: Set<String> = emptySet()) {
        assertEquals("$rule: new violations (fix them; do not add them to the allowance)", emptySet<String>(), violations - allowed)
        assertEquals("$rule: allowance entries that no longer violate (remove them)", emptySet<String>(), allowed - violations)
    }

    private fun String.isAndroid() = startsWith("android.") || startsWith("androidx.")

    private fun String.isOwnCode() = startsWith("$base.")

    @Test
    fun modelImportsNothingOfTheApp() {
        check("model", importsOf("model") { (it.isOwnCode() && !it.startsWith("$base.model.")) || it.isAndroid() })
    }

    @Test
    fun domainImportsOnlyModel() {
        check(
            "domain",
            importsOf("domain") { (it.isOwnCode() && !it.startsWith("$base.model.") && !it.startsWith("$base.domain.")) || it.isAndroid() },
        )
    }

    @Test
    fun dataKnowsNoUiAndNoStateHolders() {
        check(
            "data",
            importsOf("data") { it.startsWith("$base.ui.") || it.startsWith("$base.state.") || it.startsWith("androidx.compose") },
        )
    }

    /** The network client knows nothing of repositories: it is handed what it needs (the token) as a function. */
    @Test
    fun networkClientImportsNothingFromData() {
        val found = sources
            .filter { it.path.startsWith("data/network/") }
            .filter { s -> s.imports.any { it.startsWith("$base.data.") && !it.startsWith("$base.data.network.") } }
            .map { it.path }
            .toSet()
        check("data/network", found)
    }

    @Test
    fun stateHoldersKnowNoUiAndNoAndroid() {
        check(
            "state",
            importsOf("state") { it.startsWith("$base.ui.") || it.startsWith("$base.config.") || it.isAndroid() },
        )
    }

    @Test
    fun viewModelsImportNoCompose() {
        val found = sources.filter { it.path.endsWith("ViewModel.kt") && it.imports.any { i -> i.startsWith("androidx.compose") } }
        check("ViewModels", found.map { it.path }.toSet())
    }

    /** Screens and components read from ViewModel state; they do not reach the data, domain or state layers. */
    @Test
    fun composablesImportNoDataDomainOrStateTypes() {
        val found = sources
            .filter { it.layer == "ui" && it.isComposableFile }
            .filter { s -> s.imports.any { it.startsWith("$base.data.") || it.startsWith("$base.domain.") || it.startsWith("$base.state.") } }
            .map { it.path }
            .toSet()
        check("composables", found)
    }

    @Test
    fun namesMatchTheLayerTheyLiveIn() {
        fun typesNamed(suffix: String, belongsIn: (Source) -> Boolean) = sources
            .filter { s -> s.publicTypes.any { it.endsWith(suffix) } && !belongsIn(s) }
            .map { it.path }
            .toSet()
        check("*Repository outside data", typesNamed("Repository") { it.layer == "data" })
        check("*ViewModel outside ui", typesNamed("ViewModel") { it.layer == "ui" })
        check("*Dto outside data/network/dto", typesNamed("Dto") { it.path.startsWith("data/network/dto/") })
    }

    /**
     * One public type per file, named for it. A file may hold a small family of related types (a
     * storage interface, its implementation and the repository) as long as one of them is the file's
     * name. A file with only functions and constants has no name to match.
     */
    @Test
    fun fileNameMatchesAPublicTypeInIt() {
        val found = sources
            .filter { it.publicTypes.isNotEmpty() && it.path.substringAfterLast('/').removeSuffix(".kt") !in it.publicTypes }
            .map { it.path }
            .toSet()
        check(
            "file names",
            found,
            allowed = setOf(
                "data/DataSourceSwitching.kt",
                "data/demo/DemoApis.kt",
                "domain/RepoIssueGrouping.kt",
            ),
        )
    }

    /** A catch block must do something with the failure (return a value, report it, or log it); an empty one hides it. */
    @Test
    fun noCatchBlockIsEmpty() {
        val found = sources
            .filter { s -> EMPTY_CATCH.findAll(s.text).any { it.groupValues[1].replace(LINE_COMMENT, "").isBlank() } }
            .map { it.path }
            .toSet()
        check("empty catch blocks", found)
    }

    /** The mode fallbacks were removed because they covered a problem; a comment calling something one is the sign of a new one. */
    @Test
    fun noDefensiveFallbackComments() {
        val found = sources.filter { it.text.contains("defensive fallback", ignoreCase = true) }.map { it.path }.toSet()
        check("\"defensive fallback\" comments", found)
    }

    /** A comment says what the code is and why; which release added it is the commit history's job and goes stale in the file. */
    @Test
    fun commentsCarryNoVersionMarkers() {
        val found = sources
            .filter { s -> s.text.lines().any { line -> COMMENT_LINE.containsMatchIn(line) && VERSION_MARKER.containsMatchIn(line) } }
            .map { it.path }
            .toSet()
        check("version markers (v0.42 and the like) in comments", found)
    }

    /** Small files are found by name and read in one sitting; a file this long is doing several jobs. */
    @Test
    fun filesStayUnderTheLineLimit() {
        check("file length (limit $MAX_FILE_LINES)", sources.filter { it.lineCount > MAX_FILE_LINES }.map { it.path }.toSet())
    }

    /** A function this long cannot be tested or reviewed in one piece, and every new feature lands in it. */
    @Test
    fun functionsStayUnderTheLineLimit() {
        val tooLong = sources
            .flatMap { s -> s.functionLengths.filter { it.second > MAX_FUNCTION_LINES }.map { "${s.path}: ${it.first}" } }
            .toSet()
        check("function length (limit $MAX_FUNCTION_LINES)", tooLong)
    }

    private companion object {
        val EMPTY_CATCH = Regex("catch \\(\\w+: \\w+\\) \\{([^{}]*)\\}")
        val LINE_COMMENT = Regex("//[^\\n]*")
        val COMMENT_LINE = Regex("^\\s*(//|/?\\*)")
        val VERSION_MARKER = Regex("\\bv\\d+\\.\\d+")
        const val MAX_FILE_LINES = 600
        const val MAX_FUNCTION_LINES = 150
    }
}
