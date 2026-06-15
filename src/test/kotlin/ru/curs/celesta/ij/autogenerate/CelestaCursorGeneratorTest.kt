package ru.curs.celesta.ij.autogenerate

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.curs.celesta.score.ParseException
import java.io.File
import java.nio.file.Files

/**
 * Plain JUnit tests for in-process cursor generation (no IDE fixture). Exercises Celesta's
 * CursorGenerator via [CelestaCursorGenerator] against a throwaway score on disk.
 */
class CelestaCursorGeneratorTest {

    @Test
    fun `generates cursor java for a valid score`() {
        withTempDir { tmp ->
            val scoreRoot = File(tmp, "celestasql")
            // The package (and thus the generated cursor's package) is derived from the directory
            // under the score root, so the grain file must live in a sub-package directory.
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

            val outputDir = File(tmp, "generated")
            CelestaCursorGenerator.generate(listOf(scoreRoot), outputDir, true)

            val generated = outputDir.walkTopDown().filter { it.extension == "java" }.toList()
            assertTrue("expected at least one generated .java file, got $generated", generated.isNotEmpty())
        }
    }

    @Test
    fun `empty score roots generate nothing`() {
        withTempDir { tmp ->
            val outputDir = File(tmp, "generated")
            CelestaCursorGenerator.generate(emptyList(), outputDir, true)
            assertFalse(outputDir.exists())
        }
    }

    @Test(expected = ParseException::class)
    fun `invalid score raises ParseException`() {
        withTempDir { tmp ->
            val scoreRoot = File(tmp, "celestasql")
            File(scoreRoot, "foo").apply { mkdirs() }
                .let { File(it, "test.sql") }
                .writeText("CREATE SCHEMA test VERSION '1.0';\n\nCREATE TABLE t (id INT NOT NULL")

            CelestaCursorGenerator.generate(listOf(scoreRoot), File(tmp, "generated"), true)
        }
    }

    private fun withTempDir(action: (File) -> Unit) {
        val tmp = Files.createTempDirectory("celesta-cursor-gen").toFile()
        try {
            action(tmp)
        } finally {
            tmp.deleteRecursively()
        }
    }
}
