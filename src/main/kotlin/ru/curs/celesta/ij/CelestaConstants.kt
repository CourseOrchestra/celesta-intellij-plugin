package ru.curs.celesta.ij

import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import ru.curs.celesta.ij.autogenerate.LibraryModificationTracker

object CelestaConstants {
    const val CURSOR_FQN = "ru.curs.celesta.dbutils.Cursor"

    const val MATERIALIZED_VIEW_CURSOR_FQN = "ru.curs.celesta.dbutils.MaterializedViewCursor"

    const val SEQUENCE_FQN = "ru.curs.celesta.dbutils.Sequence"

    const val VIEW_CURSOR_FQN = "ru.curs.celesta.dbutils.ViewCursor"

    const val PARAMETRIZED_VIEW_CURSOR_FQN = "ru.curs.celesta.dbutils.ParameterizedViewCursor"

    const val DEFAULT_SOURCE_PATH = "src/main/celestasql"
    const val DEFAULT_TEST_SOURCE_PATH = "src/test/celestasql"

    const val GRAIN_NAME_FIELD = "GRAIN_NAME"

    const val OBJECT_NAME_FIELD = "OBJECT_NAME"

    @JvmStatic
    fun isCelestaProject(project: Project): Boolean = !project.isDisposed && project.isInitialized && CachedValuesManager.getManager(project).getCachedValue(project) {
        val psiFacade = JavaPsiFacade.getInstance(project)

        val hasCursorClass = DumbService.getInstance(project).computeWithAlternativeResolveEnabled<Boolean, Throwable> {
            psiFacade.findClass(CURSOR_FQN, GlobalSearchScope.allScope(project)) != null
        }

        return@getCachedValue CachedValueProvider.Result.create(hasCursorClass, LibraryModificationTracker.getInstance(project))
    }
}