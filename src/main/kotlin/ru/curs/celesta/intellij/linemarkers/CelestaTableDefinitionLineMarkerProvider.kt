package ru.curs.celesta.intellij.linemarkers

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.icons.AllIcons
import com.intellij.notification.NotificationType
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import com.intellij.sql.psi.SqlElementTypes
import com.intellij.sql.psi.SqlFile
import com.intellij.sql.psi.SqlTokens
import com.intellij.sql.psi.impl.SqlTokenElement
import com.intellij.util.PsiNavigateUtil
import ru.curs.celesta.intellij.CELESTA_NOTIFICATIONS
import ru.curs.celesta.intellij.CelestaBundle
import ru.curs.celesta.intellij.maven.CelestaMavenManager
import ru.curs.celesta.intellij.scores.CelestaGrain
import java.awt.event.MouseEvent

class CelestaTableDefinitionLineMarkerProvider : LineMarkerProvider {
    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        val matches =
            element.containingFile is SqlFile
                    && element.parent?.elementType == SqlElementTypes.SQL_IDENTIFIER
                    && element.parent?.parent?.elementType == SqlElementTypes.SQL_TABLE_REFERENCE
                    && element.parent?.parent?.parent?.elementType == SqlElementTypes.SQL_CREATE_TABLE_STATEMENT

        if (!matches)
            return null

        return LineMarkerInfo(
            element as SqlTokenElement,
            element.textRange,
            AllIcons.Nodes.Class,
            tooltipProvider,
            Navigator(element.project),
            GutterIconRenderer.Alignment.CENTER
        )
    }
}

private val tooltipProvider: com.intellij.util.Function<in SqlTokenElement, String> = com.intellij.util.Function {
    CelestaBundle.message("lineMarker.sqlDefinition.hint")
}

private class Navigator(project: Project): BaseNavigator<SqlTokenElement>(project) {
    override fun navigate(e: MouseEvent, elt: SqlTokenElement) {
        val element = findElementNavigateTo(elt)
        if (element != null)
            PsiNavigateUtil.navigate(element)
        else
            CELESTA_NOTIFICATIONS.createNotification(
                CelestaBundle.message("lineMarker.generatedSources.unableToFindDeclaration"),
                NotificationType.WARNING
            ).notify(project)
    }

    private fun findElementNavigateTo(elt: SqlTokenElement): PsiElement? = notificationOnFail {
        val grain = CelestaGrain(elt.containingFile as SqlFile)

        val packageName = grain.packageName ?: fail("No package")
        fail(packageName)

        null
    }
}