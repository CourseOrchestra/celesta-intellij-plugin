package ru.curs.celesta.intellij.linemarkers

import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.lang.java.JavaLanguage
import ru.curs.celesta.intellij.CelestaBundle
import ru.curs.celesta.intellij.assertGutterTarget


internal class GeneratedSequenceLineMarkerProviderTest : AbstractLineMarkerTest() {
    override val lineMarkerProviderClass: Class<out LineMarkerProvider> = GeneratedSequenceLineMarkerProvider::class.java
    override val language: String = JavaLanguage.INSTANCE.id
    override val myTestDataRelativePath: String = "linemarkers/generated/sequence"

    fun testHasTarget() {
        myFixture.configureByFile("example.sql")
        val createSequenceStatement= myFixture.elementAtCaret

        myFixture.configureByFile("FooSequence.java")
        val gutters = myFixture.findGuttersAtCaret()
        assertEquals(1, gutters.size)

        val gutter = gutters[0]

        assertGutterTarget(gutter, createSequenceStatement)

        assertNoNotifications()
    }

    fun testNoTarget() {
        myFixture.configureByFile("BarSequence.java")
        val gutters = myFixture.findGuttersAtCaret()
        assertEquals(1, gutters.size)

        gutters[0].assertGutterTarget { _, _ -> /*Nothing to do*/ }

        assertHasNotification {
            notification -> notification.content == CelestaBundle.message("lineMarker.generatedSources.unableToFindDeclaration")
        }
    }
}