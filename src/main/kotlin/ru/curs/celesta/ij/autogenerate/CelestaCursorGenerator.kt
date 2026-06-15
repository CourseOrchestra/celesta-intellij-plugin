package ru.curs.celesta.ij.autogenerate

import ru.curs.celesta.plugin.maven.CursorGenerator
import ru.curs.celesta.score.AbstractScore
import ru.curs.celesta.score.Grain
import ru.curs.celesta.score.GrainElement
import ru.curs.celesta.score.GrainPart
import ru.curs.celesta.score.MaterializedView
import ru.curs.celesta.score.ParameterizedView
import ru.curs.celesta.score.ReadOnlyTable
import ru.curs.celesta.score.Score
import ru.curs.celesta.score.SequenceElement
import ru.curs.celesta.score.Table
import ru.curs.celesta.score.View
import ru.curs.celesta.score.discovery.ScoreByScorePathDiscovery
import ru.curs.celesta.score.io.FileResource
import java.io.File

/**
 * Generates Celesta cursor classes in-process using Celesta's own [CursorGenerator], mirroring what
 * celesta-maven-plugin's `gen-cursors` / `gen-test-cursors` goals do — but without launching Maven,
 * which is much faster.
 *
 * The score is read from disk, so callers must save any unsaved documents first.
 */
object CelestaCursorGenerator {

    /**
     * Builds the score from [scoreRoots] and writes generated cursor classes into [outputDir].
     * Does nothing when [scoreRoots] is empty.
     *
     * @throws ru.curs.celesta.score.ParseException if the score cannot be parsed.
     */
    fun generate(scoreRoots: List<File>, outputDir: File, snakeToCamel: Boolean) {
        if (scoreRoots.isEmpty()) return

        val scorePath = scoreRoots.joinToString(File.pathSeparator) { it.absolutePath }

        val score: Score = AbstractScore.ScoreBuilder(Score::class.java)
            .scoreDiscovery(ScoreByScorePathDiscovery(scorePath))
            .build()

        val generator = CursorGenerator(outputDir, snakeToCamel)

        score.grains.values
            .filter { it.name != it.score.sysSchemaName }
            .forEach { grain -> generateGrain(grain, scoreRoots, generator) }
    }

    private fun generateGrain(grain: Grain, scoreRoots: List<File>, generator: CursorGenerator) {
        val partsToElements = LinkedHashMap<GrainPart, MutableList<GrainElement>>()

        val elements = buildList<GrainElement> {
            addAll(grain.getElements(SequenceElement::class.java).values)
            addAll(grain.getElements(Table::class.java).values)
            addAll(grain.getElements(ReadOnlyTable::class.java).values)
            addAll(grain.getElements(View::class.java).values)
            addAll(grain.getElements(MaterializedView::class.java).values)
            addAll(grain.getElements(ParameterizedView::class.java).values)
        }

        elements.forEach { element ->
            partsToElements.getOrPut(element.grainPart) { mutableListOf() }.add(element)
        }

        partsToElements.forEach { (part, partElements) ->
            // CursorGenerator derives the target package from the path of the score root that contains
            // this grain part's source file.
            val rootPath = scoreRoots
                .firstOrNull { FileResource(it).contains(part.source) }
                ?.absolutePath
                ?: ""
            partElements.forEach { generator.generateCursor(it, rootPath) }
        }
    }
}
