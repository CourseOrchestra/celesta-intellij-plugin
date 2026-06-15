package ru.curs.celesta.ij.annotator

import ru.curs.celesta.score.CelestaParser
import ru.curs.celesta.score.ParseException
import ru.curs.celesta.score.Score
import ru.curs.celesta.score.io.FileResource
import java.io.ByteArrayInputStream
import java.io.File

/**
 * A syntactic problem found while parsing a CelestaSQL file with the celesta parser.
 *
 * [line] and [column] follow the JavaCC convention: 1-based, pointing at the start of the
 * offending token.
 */
data class ParseError(
    val line: Int,
    val column: Int,
    val message: String
)

/**
 * Parses a single CelestaSQL grain file the same way celesta does (see
 * `ru.curs.celesta.score.AbstractScore#parseGrainPart`), but in isolation.
 *
 * Only JavaCC **syntax** errors are reported: the celesta parser reports them through a
 * [ParseException] (or the package-private `TokenMgrError`) whose message embeds the
 * `"at line L, column C"` location. Semantic errors (an unresolved reference to another grain or a
 * system object) are thrown with a plain message and no location; those are ignored, because a single
 * file parsed on its own cannot resolve cross-grain/system references and would otherwise produce
 * false positives. This mirrors celesta's own `AbstractScore#extractLineColNo`.
 */
object CelestaSqlParser {

    private const val TOKEN_MGR_ERROR = "ru.curs.celesta.score.TokenMgrError"

    private val LINE_COL = Regex("""at line (\d+),?\s*column (\d+)""")

    fun parse(text: String, fileName: String): ParseError? {
        val bytes = text.toByteArray(Charsets.UTF_8)
        val score = Score()
        val resource = FileResource(File(fileName))

        val grainPart = try {
            ByteArrayInputStream(bytes).use { CelestaParser(it, "utf-8").extractGrainInfo(score, resource) }
            // TokenMgrError is a package-private Error, so Throwable is the only type that catches it.
        } catch (@Suppress("TooGenericExceptionCaught") t: Throwable) {
            return toParseError(t)
        }

        try {
            ByteArrayInputStream(bytes).use { CelestaParser(it, "utf-8").parseGrainPart(grainPart) }
            // TokenMgrError is a package-private Error, so Throwable is the only type that catches it.
        } catch (@Suppress("TooGenericExceptionCaught") t: Throwable) {
            return toParseError(t)
        }

        return null
    }

    private fun toParseError(t: Throwable): ParseError? {
        // TokenMgrError is package-private in celesta, so it can't be referenced by type.
        val isSyntaxError = t is ParseException || t.javaClass.name == TOKEN_MGR_ERROR
        if (!isSyntaxError) return null

        // No "at line L, column C" location means a semantic error (e.g. an unresolved reference),
        // which a single isolated file can't resolve; ignore it to avoid false positives.
        val match = t.message?.let { LINE_COL.find(it) } ?: return null
        val line = match.groupValues[1].toIntOrNull()
        val column = match.groupValues[2].toIntOrNull()

        return if (line != null && column != null) ParseError(line, column, t.message.orEmpty()) else null
    }
}
