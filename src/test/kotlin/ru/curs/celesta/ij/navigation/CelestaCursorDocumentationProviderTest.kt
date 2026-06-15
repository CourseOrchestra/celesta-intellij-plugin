package ru.curs.celesta.ij.navigation

import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import ru.curs.celesta.ij.CelestaProjectDescriptor

internal class CelestaCursorDocumentationProviderTest : LightJavaCodeInsightFixtureTestCase() {

    override fun getTestDataPath(): String = "testdata/linemarkers/generated/tableCursor"

    override fun getProjectDescriptor(): LightProjectDescriptor = CelestaProjectDescriptor()

    fun testResolvesSqlAndBuildsDocumentation() {
        myFixture.configureByFile("score.sql")
        myFixture.configureByFile("OrderCursor.java")

        val cursorClass = myFixture.findClass("OrderCursor")

        val sqlElement = CursorSqlNavigation.findSqlElement(cursorClass)
        assertNotNull("expected to resolve the CelestaSQL element for OrderCursor", sqlElement)

        val ddl = CursorSqlNavigation.ddlText(sqlElement!!)
        assertTrue("DDL should be the CREATE TABLE statement, was: $ddl", ddl.contains("CREATE TABLE"))
        assertTrue("DDL should contain the table's columns, was: $ddl", ddl.contains("customer_name"))

        val target = CelestaCursorDocumentationProvider()
            .documentationTarget(cursorClass, cursorClass.nameIdentifier)
        assertNotNull("expected a documentation target for a cursor class", target)
        assertNotNull("expected rendered documentation", target!!.computeDocumentation())

        val navigatable = target.navigatable
        assertNotNull("expected the doc target to be navigable to the SQL", navigatable)
        assertTrue("the SQL navigatable should be navigable", navigatable!!.canNavigate())
    }

    fun testNoDocumentationWhenNoMatchingSql() {
        myFixture.configureByFile("score.sql")
        myFixture.configureByFile("OrderHeaderCursor.java")

        val cursorClass = myFixture.findClass("OrderHeaderCursor")

        assertNull(CursorSqlNavigation.findSqlElement(cursorClass))
        assertNull(
            CelestaCursorDocumentationProvider().documentationTarget(cursorClass, cursorClass.nameIdentifier)
        )
    }
}
