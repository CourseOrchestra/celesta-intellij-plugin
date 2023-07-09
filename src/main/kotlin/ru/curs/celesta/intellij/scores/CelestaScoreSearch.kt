package ru.curs.celesta.intellij.scores

import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.GlobalSearchScopes
import com.intellij.sql.SqlFileType
import com.intellij.sql.psi.SqlFile
import com.intellij.util.Processor

class CelestaScoreSearch(project: Project) {
    private val psiManager: PsiManager = PsiManager.getInstance(project)

    fun processScores(module: Module, processor: Processor<CelestaGrain>) {
        val excludeRoots = ModuleRootManager.getInstance(module).excludeRoots

        val excludedScope = GlobalSearchScopes.directoriesScope(module.project, true, *excludeRoots).let {
            GlobalSearchScope.notScope(it)
        }

        val searchScope = module.moduleContentScope.intersectWith(excludedScope)

        val sqlFiles = FileTypeIndex.getFiles(
            SqlFileType.INSTANCE, searchScope
        )

        sqlFiles.mapNotNull {
            psiManager.findFile(it) as? SqlFile
        }.map {
            CelestaGrain(it)
        }.forEach {
            processor.process(it)
        }
    }

    companion object {
        fun getInstance(project: Project): CelestaScoreSearch = project.service()
    }
}