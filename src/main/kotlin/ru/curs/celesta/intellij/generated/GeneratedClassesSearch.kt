package ru.curs.celesta.intellij.generated

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ClassInheritorsSearch
import ru.curs.celesta.intellij.castSafelyTo

class GeneratedClassesSearch(project: Project) {
    private val psiFacade: JavaPsiFacade = JavaPsiFacade.getInstance(project)

    fun searchGeneratedClass(
        grainName: String,
        cursorName: String,
        packageName: String,
        objectType: ObjectType,
        searchScope: GlobalSearchScope
    ): CelestaGeneratedObject? = runReadAction {
        val fqn = objectType.parentFqn
        val superClass = psiFacade.findClass(fqn, searchScope) ?: return@runReadAction null

        ClassInheritorsSearch.search(superClass, searchScope, true)
            .asSequence()
            .map { CelestaGeneratedObject(it) }
            .firstOrNull {
                it.objectName == cursorName
                        && it.grainName == grainName
                        && it.objectClass.parent.castSafelyTo<PsiClassOwner>()?.packageName == packageName
            }
    }

    companion object {
        fun getInstance(project: Project): GeneratedClassesSearch = project.service()
    }
}