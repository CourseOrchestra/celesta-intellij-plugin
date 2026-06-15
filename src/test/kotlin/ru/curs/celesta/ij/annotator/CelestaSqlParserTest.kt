package ru.curs.celesta.ij.annotator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Plain JUnit tests for the pure celesta parsing logic (no IDE fixture). These run against the
 * celesta-core version bundled with the plugin.
 */
class CelestaSqlParserTest {

    @Test
    fun `valid grain produces no error`() {
        val sql = """
            CREATE SCHEMA test VERSION '1.0';

            CREATE SEQUENCE s1;
        """.trimIndent()

        assertNull(CelestaSqlParser.parse(sql, "test.sql"))
    }

    @Test
    fun `syntax error is reported with a location`() {
        // A number where the sequence name (an identifier) is expected -> JavaCC syntax error.
        val sql = """
            CREATE SCHEMA test VERSION '1.0';

            CREATE SEQUENCE 123;
        """.trimIndent()

        val error = CelestaSqlParser.parse(sql, "test.sql")

        assertNotNull("Expected a syntax error to be reported", error)
        assertEquals("Error should be on the malformed line", 3, error!!.line)
    }

    @Test
    fun `error in the schema header is reported`() {
        // Missing VERSION value -> syntax error in the header (extractGrainInfo phase).
        val sql = "CREATE SCHEMA test VERSION ;"

        val error = CelestaSqlParser.parse(sql, "test.sql")

        assertNotNull("Expected a header syntax error to be reported", error)
        assertEquals(1, error!!.line)
    }

    @Test
    fun `semantic-only problems are ignored`() {
        // Syntactically valid, but the foreign key references a table that does not exist.
        // A single file cannot resolve such references, so this must NOT be flagged.
        val sql = """
            CREATE SCHEMA test VERSION '1.0';

            CREATE TABLE t (
              id INT NOT NULL,
              fk INT,
              CONSTRAINT pk PRIMARY KEY (id),
              FOREIGN KEY (fk) REFERENCES test.nonexistent (x)
            );
        """.trimIndent()

        assertNull(CelestaSqlParser.parse(sql, "test.sql"))
    }
}
