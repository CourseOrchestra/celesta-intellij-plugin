package ru.curs.celesta.ij.autogenerate

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.curs.celesta.score.ParseException
import java.io.File
import java.nio.file.Files

/**
 * Plain JUnit tests for in-process generation (no IDE fixture). Exercises Celesta's CursorGenerator
 * and the score-resource generation via [CelestaGenerator] against a throwaway score on disk.
 */
class CelestaGeneratorTest {

    @Test
    fun `generates cursors and score resources for a valid score`() {
        withTempDir { tmp ->
            val scoreRoot = File(tmp, "celestasql")
            // The package (and thus the generated cursor's package + score resource path) is derived
            // from the directory under the score root, so the grain file lives in a sub-package dir.
            val grainDir = File(scoreRoot, "foo").apply { mkdirs() }
            File(grainDir, "test.sql").writeText(
                """
                CREATE SCHEMA test VERSION '1.0';

                CREATE TABLE t (
                  id INT NOT NULL,
                  CONSTRAINT pk_t PRIMARY KEY (id)
                );
                """.trimIndent()
            )

            val cursorDir = File(tmp, "generated")
            val scoreDir = File(tmp, "out/score")
            CelestaGenerator.generate(listOf(scoreRoot), cursorDir, scoreDir, true)

            val generatedJava = cursorDir.walkTopDown().filter { it.extension == "java" }.toList()
            assertTrue("expected a generated cursor .java, got $generatedJava", generatedJava.isNotEmpty())

            val scoreFiles = File(scoreDir, "score.files")
            assertTrue("expected score.files to be generated", scoreFiles.isFile)
            assertEquals("foo/test.sql", scoreFiles.readText().trim())
            assertTrue("expected the grain .sql to be copied", File(scoreDir, "foo/test.sql").isFile)
        }
    }

    @Test
    fun `empty score roots generate nothing`() {
        withTempDir { tmp ->
            val cursorDir = File(tmp, "generated")
            CelestaGenerator.generate(emptyList(), cursorDir, File(tmp, "score"), true)
            assertFalse(cursorDir.exists())
        }
    }

    @Test(expected = ParseException::class)
    fun `invalid score raises ParseException`() {
        withTempDir { tmp ->
            val scoreRoot = File(tmp, "celestasql")
            File(scoreRoot, "foo").apply { mkdirs() }
                .let { File(it, "test.sql") }
                .writeText("CREATE SCHEMA test VERSION '1.0';\n\nCREATE TABLE t (id INT NOT NULL")

            CelestaGenerator.generate(listOf(scoreRoot), File(tmp, "generated"), File(tmp, "score"), true)
        }
    }

    private fun withTempDir(action: (File) -> Unit) {
        val tmp = Files.createTempDirectory("celesta-gen").toFile()
        try {
            action(tmp)
        } finally {
            tmp.deleteRecursively()
        }
    }
}
