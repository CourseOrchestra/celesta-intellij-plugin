package ru.curs.celesta.intellij.linemarkers

import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.lang.java.JavaLanguage
import ru.curs.celesta.intellij.CelestaBundle
import ru.curs.celesta.intellij.assertGutterTarget


internal class GeneratedTableLineMarkerProviderTest : AbstractLineMarkerTest() {
    override val lineMarkerProviderClass: Class<out LineMarkerProvider> = GeneratedTableLineMarkerProvider::class.java
    override val language: String = JavaLanguage.INSTANCE.id
    override val myTestDataRelativePath: String = "linemarkers/generated/tableCursor"

    fun testHasTarget() {
        myFixture.configureByFile("score.sql")
        val createTableStatement = myFixture.elementAtCaret

        myFixture.configureByFile("OrderCursor.java")
        val gutters = myFixture.findGuttersAtCaret()
        assertEquals(1, gutters.size)

        val gutter = gutters[0]

        assertGutterTarget(gutter, createTableStatement)

        assertNoNotifications()
    }

    fun testNoTarget() {
        myFixture.configureByFile("OrderHeaderCursor.java")
        val gutters = myFixture.findGuttersAtCaret()
        assertEquals(1, gutters.size)

        gutters[0].assertGutterTarget { _, _ -> /*Nothing to do*/ }

        assertHasNotification {
            notification -> notification.content == CelestaBundle.message("lineMarker.generatedSources.unableToFindDeclaration")
        }
    }
}