package ru.curs.celesta.intellij.linemarkers

import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.sql.psi.SqlLanguage
import junit.framework.TestCase
import ru.curs.celesta.intellij.CelestaBundle
import ru.curs.celesta.intellij.assertGutterTarget
import ru.curs.celesta.intellij.getElementUnderCaret


internal class CelestaViewDefinitionLineMarkerProviderTest : AbstractLineMarkerTest() {
    override val lineMarkerProviderClass: Class<out LineMarkerProvider> = CelestaViewDefinitionLineMarkerProvider::class.java
    override val language: String = SqlLanguage.INSTANCE.id

    override val myTestDataRelativePath: String = "linemarkers/sql/view"

    fun testHasTarget() {
        myFixture.configureByFile("FooCursor.java")
        val cursorClass = myFixture.editor.getElementUnderCaret()

        myFixture.configureByFile("score.sql")
        val gutters = myFixture.findGuttersAtCaret()
        assertEquals(1, gutters.size)

        val gutter = gutters[0]
        assertGutterTarget(gutter, cursorClass)

        assertNoNotifications()
    }

    fun testNoTarget() {
        myFixture.configureByFile("score_no_target.sql")
        val gutters = myFixture.findGuttersAtCaret()
        assertEquals(1, gutters.size)

        gutters[0].assertGutterTarget { _, _ -> /*Nothing to do*/ }

        assertHasNotification {
                notification -> notification.content == CelestaBundle.message("lineMarker.generatedSources.unableToFindDeclaration")
        }
    }
}