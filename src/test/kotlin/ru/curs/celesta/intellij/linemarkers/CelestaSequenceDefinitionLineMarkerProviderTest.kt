package ru.curs.celesta.intellij.linemarkers

import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.sql.psi.SqlLanguage
import ru.curs.celesta.intellij.CelestaBundle
import ru.curs.celesta.intellij.assertGutterTarget
import ru.curs.celesta.intellij.getElementUnderCaret


internal class CelestaSequenceDefinitionLineMarkerProviderTest : AbstractLineMarkerTest() {
    override val lineMarkerProviderClass: Class<out LineMarkerProvider> = CelestaSequenceDefinitionLineMarkerProvider::class.java
    override val language: String = SqlLanguage.INSTANCE.id

    override val myTestDataRelativePath: String = "linemarkers/sql/sequence"

    fun testHasTarget() {
        myFixture.configureByFile("FooSequence.java")
        val sequenceClass = myFixture.editor.getElementUnderCaret()

        myFixture.configureByFile("example.sql")
        val gutters = myFixture.findGuttersAtCaret()
        assertEquals(1, gutters.size)

        val gutter = gutters[0]
        assertGutterTarget(gutter, sequenceClass)

        assertNoNotifications()
    }

    fun testNoTarget() {
        myFixture.configureByFile("example_no_target.sql")
        val gutters = myFixture.findGuttersAtCaret()
        assertEquals(1, gutters.size)

        gutters[0].assertGutterTarget { _, _ -> /*Nothing to do*/ }

        assertHasNotification {
                notification -> notification.content == CelestaBundle.message("lineMarker.generatedSources.unableToFindDeclaration")
        }
    }
}