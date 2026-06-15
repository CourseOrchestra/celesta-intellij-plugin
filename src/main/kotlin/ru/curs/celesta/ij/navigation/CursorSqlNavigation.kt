package ru.curs.celesta.ij.navigation

import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
import com.intellij.sql.psi.SqlStatement
import com.intellij.util.CommonProcessors
import ru.curs.celesta.ij.generated.CelestaGeneratedObject
import ru.curs.celesta.ij.generated.ObjectType
import ru.curs.celesta.ij.scores.CelestaGrain
import ru.curs.celesta.ij.scores.CelestaScoreSearch

/**
 * Resolves a generated Celesta cursor [PsiClass] to the CelestaSQL element that defines it, mirroring
 * the navigation performed by the gutter line markers but exposed as a plain function so other
 * features (e.g. quick documentation) can reuse it.
 */
object CursorSqlNavigation {

    fun findSqlElement(cursorClass: PsiClass): PsiElement? {
        val cursor = CelestaGeneratedObject(cursorClass)
        val type = cursor.type
        val grainName = cursor.grainName
        val objectName = cursor.objectName
        val module = ModuleUtilCore.findModuleForPsiElement(cursorClass)
        if (type == null || grainName == null) return null
        if (objectName == null || module == null) return null

        val packageName = (cursorClass.parent as? PsiClassOwner)?.packageName ?: ""
        val extractor = extractorFor(type)
        val grains = CommonProcessors.CollectProcessor<CelestaGrain>()
        CelestaScoreSearch.getInstance(cursorClass.project).processScores(module, grains)

        return grains.results
            .asSequence()
            .filter { it.grainName == grainName && it.packageName == packageName }
            .mapNotNull { grain -> grain.extractor(objectName) }
            .firstOrNull()
    }

    /** The DDL text of the CelestaSQL statement that [sqlElement] belongs to. */
    fun ddlText(sqlElement: PsiElement): String =
        (sqlElement as? SqlStatement ?: sqlElement.parentOfType<SqlStatement>() ?: sqlElement).text

    private fun extractorFor(type: ObjectType): CelestaGrain.(String) -> PsiElement? = when (type) {
        ObjectType.TableCursor -> { name -> tables[name] }
        ObjectType.MaterializedViewCursor -> { name -> materializedViews[name] }
        ObjectType.Sequence -> { name -> sequences[name] }
        ObjectType.View -> { name -> views[name] }
        ObjectType.Function -> { name -> functions[name] }
    }
}
