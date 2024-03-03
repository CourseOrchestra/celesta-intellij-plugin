package ru.curs.celesta.ij

import com.intellij.codeInsight.TargetElementUtil
import com.intellij.codeInsight.daemon.GutterMark
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.navigation.NavigationGutterIconRenderer
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.util.PsiNavigateUtil
import org.junit.Assert

/**
 * Won't work if there are more than one target.
 */
inline fun GutterMark.assertGutterTarget(func: (Editor, PsiFile) -> Unit) {
    @Suppress("UNCHECKED_CAST")
    val lineMarkerInfo: LineMarkerInfo<PsiElement> =
        (this as LineMarkerInfo.LineMarkerGutterIconRenderer<PsiElement>).lineMarkerInfo

    val project = lineMarkerInfo.element!!.project

    val navigationHandler = lineMarkerInfo.navigationHandler

    if (navigationHandler is NavigationGutterIconRenderer) {
        val target = navigationHandler.targetElements[0]
        PsiNavigateUtil.navigate(target)
    } else {
        navigationHandler!!.navigate(null, lineMarkerInfo.element)
    }

    val selectedEditor: FileEditor = FileEditorManager.getInstance(project).selectedEditor!!
    val virtualFile = selectedEditor.file!!
    val psiFile = PsiManager.getInstance(project).findFile(virtualFile)!!

    func((selectedEditor as TextEditor).editor, psiFile)
}

fun assertGutterTarget(gutter: GutterMark, target: PsiElement?) {
    gutter.assertGutterTarget { editor, _ ->
        Assert.assertEquals(target, editor.getElementUnderCaret())
    }
}

fun Editor.getElementUnderCaret(): PsiElement? {
    val findTargetFlags = TargetElementUtil.REFERENCED_ELEMENT_ACCEPTED or TargetElementUtil.ELEMENT_NAME_ACCEPTED
    return TargetElementUtil.findTargetElement(this, findTargetFlags)
}


private fun <K> CodeInsightTestFixture.processCarets(func: () -> K): List<K> {
    val caretModel = editor.caretModel

    val allOffsets = caretModel.allCarets.map { it.offset }

    return allOffsets.map {
        editor.caretModel.moveToOffset(it)
        func()
    }.also {
        if (allOffsets.isEmpty())
            return@also

        caretModel.removeSecondaryCarets()
        caretModel.moveToOffset(allOffsets.first())

        allOffsets.drop(1).forEach {
            caretModel.addCaret(editor.offsetToVisualPosition(it))
        }
    }
}