package ru.curs.celesta.ij.linemarkers

import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.sql.psi.SqlLanguage
import ru.curs.celesta.ij.CelestaBundle
import ru.curs.celesta.ij.assertGutterTarget
import ru.curs.celesta.ij.getElementUnderCaret


internal class CelestaFunctionDefinitionLineMarkerProviderTest : AbstractLineMarkerTest() {
    override val lineMarkerProviderClass: Class<out LineMarkerProvider> = CelestaFunctionDefinitionLineMarkerProvider::class.java
    override val language: String = SqlLanguage.INSTANCE.id

    override val myTestDataRelativePath: String = "linemarkers/sql/function"

    fun testHasTarget() {
        myFixture.configureByFile("FfuncCursor.java")
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