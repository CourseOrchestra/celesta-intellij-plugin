package ru.curs.celesta.ij.autogenerate

import ru.curs.celesta.plugin.maven.CursorGenerator
import ru.curs.celesta.score.AbstractScore
import ru.curs.celesta.score.Grain
import ru.curs.celesta.score.GrainElement
import ru.curs.celesta.score.GrainPart
import ru.curs.celesta.score.MaterializedView
import ru.curs.celesta.score.Namespace
import ru.curs.celesta.score.ParameterizedView
import ru.curs.celesta.score.ReadOnlyTable
import ru.curs.celesta.score.Score
import ru.curs.celesta.score.SequenceElement
import ru.curs.celesta.score.Table
import ru.curs.celesta.score.View
import ru.curs.celesta.score.discovery.ScoreByScorePathDiscovery
import ru.curs.celesta.score.io.FileResource
import ru.curs.celesta.score.io.Resource
import java.io.File

/**
 * Generates Celesta artifacts in-process, mirroring celesta-maven-plugin's `gen-cursors` and
 * `gen-score-resources` goals — but without launching Maven, which is much faster.
 *
 *  - **Cursors**: Java cursor classes, via Celesta's own [CursorGenerator].
 *  - **Score resources**: the `score.files` index plus copies of the grain `.sql` files, which
 *    Celesta loads from the classpath at runtime (see `ScoreByScoreResourceDiscovery`). These are
 *    written under `generated-resources/score`, mirroring the Maven plugin's layout.
 *
 * The score is read from disk, so callers must save any unsaved documents first.
 */
object CelestaGenerator {

    private const val SCORE_FILES_NAME = "score.files"

    /**
     * Builds the score from [scoreRoots] once, then writes cursor classes into [cursorOutputDir] and
     * score resources into [scoreResourceDir] (the `score` directory on the runtime classpath). Does
     * nothing when [scoreRoots] is empty.
     *
     * @throws ru.curs.celesta.score.ParseException if the score cannot be parsed.
     */
    fun generate(scoreRoots: List<File>, cursorOutputDir: File, scoreResourceDir: File, snakeToCamel: Boolean) {
        if (scoreRoots.isEmpty()) return

        val scorePath = scoreRoots.joinToString(File.pathSeparator) { it.absolutePath }
        val score: Score = AbstractScore.ScoreBuilder(Score::class.java)
            .scoreDiscovery(ScoreByScorePathDiscovery(scorePath))
            .build()

        generateCursors(score, scoreRoots, cursorOutputDir, snakeToCamel)
        generateScoreResources(score, scoreRoots, scoreResourceDir)
    }

    private fun generateCursors(
        score: Score,
        scoreRoots: List<File>,
        cursorOutputDir: File,
        snakeToCamel: Boolean
    ) {
        val generator = CursorGenerator(cursorOutputDir, snakeToCamel)

        userGrains(score).forEach { grain ->
            val partsToElements = LinkedHashMap<GrainPart, MutableList<GrainElement>>()
            grainElements(grain).forEach { element ->
                partsToElements.getOrPut(element.grainPart) { mutableListOf() }.add(element)
            }

            partsToElements.forEach { (part, partElements) ->
                // CursorGenerator derives the target package from the path of the score root that
                // contains this grain part's source file.
                val rootPath = scoreRoots
                    .firstOrNull { FileResource(it.absoluteFile).contains(part.source) }
                    ?.absolutePath
                    ?: ""
                partElements.forEach { generator.generateCursor(it, rootPath) }
            }
        }
    }

    private fun generateScoreResources(
        score: Score,
        scoreRoots: List<File>,
        scoreResourceDir: File
    ) {
        val scoreResources = scoreRoots.map { FileResource(it.absoluteFile) }
        val relativePaths = sortedSetOf<String>()

        userGrains(score)
            .flatMap { it.grainParts }
            .forEach { part ->
                val grainSource = part.source ?: return@forEach
                if (Namespace.DEFAULT == part.namespace) {
                    throw ru.curs.celesta.CelestaException(
                        "Couldn't generate score resource for %s without package", grainSource
                    )
                }
                val scoreSource = scoreResources.first { it.contains(grainSource) }
                val relativePath = scoreSource.getRelativePath(grainSource)

                relativePaths.add(relativePath.replace(File.separatorChar, '/'))
                copyResource(grainSource, File(scoreResourceDir, relativePath))
            }

        if (relativePaths.isEmpty()) return

        File(scoreResourceDir, SCORE_FILES_NAME).apply {
            parentFile?.mkdirs()
            writeText(relativePaths.joinToString(System.lineSeparator()))
        }
    }

    private fun copyResource(from: Resource, to: File) {
        to.parentFile?.mkdirs()
        from.inputStream.use { input ->
            to.outputStream().use { output -> input.copyTo(output) }
        }
    }

    private fun userGrains(score: Score): List<Grain> =
        score.grains.values.filter { it.name != it.score.sysSchemaName }

    private fun grainElements(grain: Grain): List<GrainElement> = buildList {
        addAll(grain.getElements(SequenceElement::class.java).values)
        addAll(grain.getElements(Table::class.java).values)
        addAll(grain.getElements(ReadOnlyTable::class.java).values)
        addAll(grain.getElements(View::class.java).values)
        addAll(grain.getElements(MaterializedView::class.java).values)
        addAll(grain.getElements(ParameterizedView::class.java).values)
    }
}
