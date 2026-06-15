package ru.curs.celesta.ij.annotator

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.sql.psi.SqlFile
import ru.curs.celesta.ij.CelestaConstants
import ru.curs.celesta.ij.maven.CelestaMavenManager
import ru.curs.celesta.ij.scores.CelestaGrain

/**
 * Highlights CelestaSQL syntax errors in the editor by parsing the file with the bundled celesta
 * parser ([CelestaSqlParser]) on a background thread and underlining the offending token in red.
 */
class CelestaSqlParseAnnotator : ExternalAnnotator<CelestaSqlParseAnnotator.Info, ParseError>() {

    data class Info(val text: String, val fileName: String)

    override fun collectInformation(file: PsiFile, editor: Editor, hasErrors: Boolean): Info? {
        val project = file.project

        // Cheapest checks first: cached project check, then PSI type, then the source-root / grain check.
        if (!CelestaConstants.isCelestaProject(project)) return null
        if (file !is SqlFile) return null

        val virtualFile = file.virtualFile ?: return null

        val isScoreFile = CelestaMavenManager.getInstance(project).isCelestaScoreFile(virtualFile)
                || CelestaGrain(file).grainName != null
        if (!isScoreFile) return null

        return Info(file.text, virtualFile.name)
    }

    override fun doAnnotate(collectedInfo: Info?): ParseError? {
        val info = collectedInfo ?: return null
        // Runs on a background thread without a read lock; operates only on the captured text.
        return CelestaSqlParser.parse(info.text, info.fileName)
    }

    override fun apply(file: PsiFile, error: ParseError?, holder: AnnotationHolder) {
        error ?: return

        val document = PsiDocumentManager.getInstance(file.project).getDocument(file) ?: return

        // JavaCC line/column are 1-based; the document is 0-based. The document may have changed since
        // collectInformation, so clamp every value defensively.
        val line = (error.line - 1).coerceIn(0, (document.lineCount - 1).coerceAtLeast(0))
        val start = (document.getLineStartOffset(line) + (error.column - 1)).coerceIn(0, document.textLength)
        val end = tokenEndOffset(document.charsSequence, start).coerceIn(start, document.textLength)
        if (start >= end) return

        holder.newAnnotation(HighlightSeverity.ERROR, error.message)
            .range(TextRange(start, end))
            .create()
    }

    /**
     * Approximates the extent of the offending token starting at [start]: the run of identifier
     * characters, or a single character if [start] is on punctuation. Keeps the red underline visible
     * and roughly token-shaped without needing celesta's package-private token classes.
     */
    private fun tokenEndOffset(text: CharSequence, start: Int): Int {
        if (start >= text.length) return start
        var end = start
        while (end < text.length && (text[end].isLetterOrDigit() || text[end] == '_')) {
            end++
        }
        return if (end == start) start + 1 else end
    }
}
