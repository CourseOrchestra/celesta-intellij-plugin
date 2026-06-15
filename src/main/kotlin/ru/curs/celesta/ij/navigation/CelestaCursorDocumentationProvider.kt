package ru.curs.celesta.ij.navigation

import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.model.Pointer
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.util.text.StringUtil
import com.intellij.platform.backend.documentation.DocumentationResult
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.documentation.PsiDocumentationTargetProvider
import com.intellij.platform.backend.presentation.TargetPresentation
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import ru.curs.celesta.ij.CelestaConstants
import ru.curs.celesta.ij.generated.CelestaGeneratedObject

/**
 * Adds the defining CelestaSQL to the Quick Documentation popup (Ctrl+Q / hover) of a generated Celesta
 * cursor class, so the table/view/sequence DDL can be read straight from a cursor usage in Java code
 * without navigating to the generated class and then to the `.sql` file.
 *
 * Generated cursor classes carry no Javadoc, so replacing their (empty) documentation is fine.
 */
class CelestaCursorDocumentationProvider : PsiDocumentationTargetProvider {

    override fun documentationTarget(element: PsiElement, originalElement: PsiElement?): DocumentationTarget? {
        if (element !is PsiClass) return null
        if (!CelestaConstants.isCelestaProject(element.project)) return null

        val cursor = CelestaGeneratedObject(element)
        if (cursor.type == null) return null

        val sqlElement = CursorSqlNavigation.findSqlElement(element) ?: return null
        val title = listOfNotNull(cursor.grainName, cursor.objectName).joinToString(".")

        return CelestaSqlDocumentationTarget(
            presentationText = title.ifEmpty { element.name ?: "CelestaSQL" },
            html = buildHtml(title, CursorSqlNavigation.ddlText(sqlElement)),
            sqlPointer = SmartPointerManager.getInstance(element.project).createSmartPsiElementPointer(sqlElement)
        )
    }

    private fun buildHtml(title: String, ddl: String): String = buildString {
        append(DocumentationMarkup.DEFINITION_START)
        append("CelestaSQL")
        if (title.isNotEmpty()) append(": ").append(StringUtil.escapeXmlEntities(title))
        append(DocumentationMarkup.DEFINITION_END)
        append(DocumentationMarkup.CONTENT_START)
        append("<pre>").append(StringUtil.escapeXmlEntities(ddl)).append("</pre>")
        append(DocumentationMarkup.CONTENT_END)
    }

    private class CelestaSqlDocumentationTarget(
        private val presentationText: String,
        private val html: String,
        private val sqlPointer: SmartPsiElementPointer<PsiElement>
    ) : DocumentationTarget {

        // The target holds only immutable strings and a smart pointer (both safe to retain).
        override fun createPointer(): Pointer<out DocumentationTarget> = Pointer.hardPointer(this)

        override fun computePresentation(): TargetPresentation =
            TargetPresentation.builder(presentationText).presentation()

        override fun computeDocumentation(): DocumentationResult = DocumentationResult.documentation(html)

        // Enables the "Edit Source" action (F4 / toolbar icon) in the doc popup to jump to the .sql.
        override val navigatable: Navigatable?
            get() {
                val sqlElement = sqlPointer.element ?: return null
                val virtualFile = sqlElement.containingFile?.virtualFile ?: return null
                return OpenFileDescriptor(sqlElement.project, virtualFile, sqlElement.textOffset)
            }
    }
}
