package ru.curs.celesta.ij.linemarkers

import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import ru.curs.celesta.ij.CelestaBundle
import ru.curs.celesta.ij.assertGutterTarget


internal class GeneratedMaterializedViewLineMarkerProviderTest : AbstractLineMarkerTest() {
    override val lineMarkerProviderClass: Class<out LineMarkerProvider> =
        GeneratedMaterializedViewLineMarkerProvider::class.java
    override val language: String = JavaLanguage.INSTANCE.id
    override val myTestDataRelativePath: String = "linemarkers/generated/viewCursor"

    fun testHasTarget() {
        myFixture.configureByFile("score.sql")

        val createViewStatement = myFixture.editor.anyElementUnderCaret()

        myFixture.configureByFile("OrderedQtyCursor.java")
        val gutters = myFixture.findGuttersAtCaret()
        assertEquals(1, gutters.size)

        val gutter = gutters[0]
        
        gutter.assertGutterTarget { editor, _ ->
            assertEquals(createViewStatement, editor.anyElementUnderCaret())
        }

        assertNoNotifications()
    }

    fun testNoTarget() {
        myFixture.configureByFile("SomeViewCursor.java")
        val gutters = myFixture.findGuttersAtCaret()
        assertEquals(1, gutters.size)

        gutters[0].assertGutterTarget { _, _ -> /*Nothing to do*/ }

        assertHasNotification {
                notification -> notification.content == CelestaBundle.message("lineMarker.generatedSources.unableToFindDeclaration")
        }
    }

    private fun Editor.anyElementUnderCaret(): PsiElement? {
        val file = PsiDocumentManager.getInstance(project!!).getPsiFile(document)!!
        return file.findElementAt(caretModel.offset)
    }
}