package ru.curs.celesta.intellij.generated

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ClassInheritorsSearch
import ru.curs.celesta.intellij.CelestaConstants

class GeneratedClassesSearch(project: Project) {
    private val psiFacade: JavaPsiFacade = JavaPsiFacade.getInstance(project)

    fun searchTableCursor(grainName: String, cursorName: String, searchScope: GlobalSearchScope): CelestaCursor? {
        return searchCursor(grainName, cursorName, CursorType.TableCursor, searchScope)
    }

    fun searchMaterializedViewCursor(grainName: String, cursorName: String, searchScope: GlobalSearchScope): CelestaCursor? {
        return searchCursor(grainName, cursorName, CursorType.MaterializedViewCursor, searchScope)
    }

    private fun searchCursor(grainName: String, cursorName: String, cursorType: CursorType, searchScope: GlobalSearchScope): CelestaCursor? = runReadAction {
        val fqn = when (cursorType) {
            CursorType.TableCursor -> CelestaConstants.CURSOR_FQN
            CursorType.MaterializedViewCursor -> CelestaConstants.MATERIALIZED_VIEW_CURSOR_FQN
        }
        val superClass = psiFacade.findClass(fqn, searchScope) ?: return@runReadAction null

        ClassInheritorsSearch.search(superClass, searchScope, true)
            .asSequence()
            .map { CelestaCursor(it) }
            .firstOrNull { it.cursorName == cursorName && it.grainName == grainName }
    }

    companion object {
        fun getInstance(project: Project): GeneratedClassesSearch = project.service()
    }
}