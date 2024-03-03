package ru.curs.celesta.ij.linemarkers

import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.lang.java.JavaLanguage
import ru.curs.celesta.ij.CelestaBundle
import ru.curs.celesta.ij.assertGutterTarget
import ru.curs.celesta.ij.getElementUnderCaret


internal class GeneratedFunctionLineMarkerProviderTest : AbstractLineMarkerTest() {
    override val lineMarkerProviderClass: Class<out LineMarkerProvider> = GeneratedFunctionLineMarkerProvider::class.java
    override val language: String = JavaLanguage.INSTANCE.id
    override val myTestDataRelativePath: String = "linemarkers/generated/functionCursor"

    fun testHasTarget() {
        myFixture.configureByFile("score.sql")
        val createSequenceStatement= myFixture.editor.getElementUnderCaret()

        myFixture.configureByFile("FuncCursor.java")
        val gutters = myFixture.findGuttersAtCaret()
        assertEquals(1, gutters.size)

        val gutter = gutters[0]

        assertGutterTarget(gutter, createSequenceStatement)

        assertNoNotifications()
    }

    fun testNoTarget() {
        myFixture.configureByFile("FfuncCursor.java")
        val gutters = myFixture.findGuttersAtCaret()
        assertEquals(1, gutters.size)

        gutters[0].assertGutterTarget { _, _ -> /*Nothing to do*/ }

        assertHasNotification {
            notification -> notification.content == CelestaBundle.message("lineMarker.generatedSources.unableToFindDeclaration")
        }
    }
}