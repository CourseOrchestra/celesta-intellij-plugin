package ru.curs.celesta.ij.linemarkers

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.notification.NotificationType
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.util.parentOfType
import com.intellij.util.CommonProcessors
import com.intellij.util.PsiNavigateUtil
import icons.DatabaseIcons
import ru.curs.celesta.ij.CELESTA_NOTIFICATIONS
import ru.curs.celesta.ij.CelestaBundle
import ru.curs.celesta.ij.CelestaConstants
import ru.curs.celesta.ij.generated.CelestaGeneratedObject
import ru.curs.celesta.ij.scores.CelestaGrain
import ru.curs.celesta.ij.scores.CelestaScoreSearch
import java.awt.event.MouseEvent

abstract class CelestaGeneratedClassLineMarkerProvider : LineMarkerProvider {
    protected abstract val parentFqn: String
    protected abstract val objectExtractor: ObjectExtractor

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        if (element !is PsiIdentifier) return null

        if(!CelestaConstants.isCelestaProject(element.project)) return null

        val psiClass = element.parent as? PsiClass ?: return null

        val psiFacade = JavaPsiFacade.getInstance(element.project)

        val cursorClass = psiFacade.findClass(parentFqn, element.resolveScope) ?: return null

        if (psiClass.isInheritor(cursorClass, true)) {
            return LineMarkerInfo(
                element,
                element.textRange,
                DatabaseIcons.Sql,
                tooltipProvider,
                NavHandler(element.project, objectExtractor),
                GutterIconRenderer.Alignment.CENTER
            ) {
                "Go to sql"
            }
        }

        return null
    }
}

private val tooltipProvider: com.intellij.util.Function<in PsiIdentifier, String> = com.intellij.util.Function {
    val cursorClass = it.parent as PsiClass
    CelestaBundle.message("lineMarker.generatedSources.hint", cursorClass.qualifiedName ?: "")
}

private class NavHandler(
    project: Project,
    private val objectExtractor: ObjectExtractor
) : BaseNavigator<PsiIdentifier>(project) {

    val scoreSearch = CelestaScoreSearch.getInstance(project)

    override fun navigate(e: MouseEvent?, elt: PsiIdentifier) {
        val element = findElementNavigateTo(elt)
        if (element != null)
            PsiNavigateUtil.navigate(element)
        else
            CELESTA_NOTIFICATIONS.createNotification(
                CelestaBundle.message("lineMarker.generatedSources.unableToFindDeclaration"),
                NotificationType.WARNING
            ).notify(project)
    }

    private fun findElementNavigateTo(elt: PsiIdentifier): PsiElement? = notificationOnFail {
        val cursorClass = elt.parentOfType<PsiClass>()!!

        val packageName = (cursorClass.parent as? PsiClassOwner)?.packageName ?: ""

        val cursor = CelestaGeneratedObject(cursorClass)

        val module = ModuleUtilCore.findModuleForPsiElement(elt)
            ?: fail(CelestaBundle.message("lineMarker.generatedSources.unknownModule", elt.containingFile.virtualFile.path))

        val grainName = cursor.grainName
            ?: fail(CelestaBundle.message("lineMarker.generatedSources.unknownGrain"))

        val objectName = cursor.objectName
            ?: fail(CelestaBundle.message("lineMarker.generatedSources.unknownObject"))

        val collectProcessor = CommonProcessors.CollectProcessor<CelestaGrain>()
        scoreSearch.processScores(module, collectProcessor)

        collectProcessor
            .results
            .asSequence()
            .filter { it.grainName == grainName && it.packageName == packageName }
            .mapNotNull { it.objectExtractor(objectName) }
            .firstOrNull()
    }
}

typealias ObjectExtractor = CelestaGrain.(String) -> PsiElement?

class GeneratedTableLineMarkerProvider : CelestaGeneratedClassLineMarkerProvider() {
    override val parentFqn = CelestaConstants.CURSOR_FQN

    override val objectExtractor: ObjectExtractor = { tableName -> tables[tableName] }
}

class GeneratedMaterializedViewLineMarkerProvider : CelestaGeneratedClassLineMarkerProvider() {
    override val parentFqn = CelestaConstants.MATERIALIZED_VIEW_CURSOR_FQN

    override val objectExtractor: ObjectExtractor = { viewName -> materializedViews[viewName] }
}

class GeneratedSequenceLineMarkerProvider : CelestaGeneratedClassLineMarkerProvider() {
    override val parentFqn = CelestaConstants.SEQUENCE_FQN

    override val objectExtractor: ObjectExtractor = { seqName -> sequences[seqName] }
}

class GeneratedViewLineMarkerProvider : CelestaGeneratedClassLineMarkerProvider() {
    override val parentFqn = CelestaConstants.VIEW_CURSOR_FQN

    override val objectExtractor: ObjectExtractor = { viewName -> views[viewName] }
}

class GeneratedFunctionLineMarkerProvider : CelestaGeneratedClassLineMarkerProvider() {
    override val parentFqn = CelestaConstants.PARAMETRIZED_VIEW_CURSOR_FQN

    override val objectExtractor: ObjectExtractor = { funName -> functions[funName] }
}