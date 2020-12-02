package ru.curs.celesta.intellij.linemarkers

import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.sql.psi.SqlLanguage
import ru.curs.celesta.intellij.CelestaBundle
import ru.curs.celesta.intellij.assertGutterTarget
import ru.curs.celesta.intellij.getElementUnderCaret


internal class CelestaMaterializedViewDefinitionLineMarkerProviderTest : AbstractLineMarkerTest() {
    override val lineMarkerProviderClass: Class<out LineMarkerProvider> = CelestaMaterializedViewDefinitionLineMarkerProvider::class.java
    override val language: String = SqlLanguage.INSTANCE.id

    override val myTestDataRelativePath: String = "linemarkers/sql/materializedView"

    fun testHasTarget() {
        myFixture.configureByFile("OrderedQtyCursor.java")
        val cursorClass = myFixture.editor.getElementUnderCaret()

        myFixture.configureByFile("score.sql")
        val gutters = myFixture.findGuttersAtCaret()
        assert(gutters.size == 1)

        val gutter = gutters[0]
        assertGutterTarget(gutter, cursorClass)

        assertNoNotifications()
    }

    fun testNoTarget() {
        myFixture.configureByFile("score_no_target.sql")
        val gutters = myFixture.findGuttersAtCaret()
        assert(gutters.size == 1)

        gutters[0].assertGutterTarget { _, _ -> /*Nothing to do*/ }

        assertHasNotification {
                notification -> notification.content == CelestaBundle.message("lineMarker.generatedSources.unableToFindDeclaration")
        }
    }
}