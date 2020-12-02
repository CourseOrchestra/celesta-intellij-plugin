package ru.curs.celesta.intellij.generated

import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import ru.curs.celesta.intellij.CelestaConstants
import ru.curs.celesta.intellij.cachedValue

class CelestaCursor private constructor(cursorClass: PsiClass) {
    private val pointer: SmartPsiElementPointer<PsiClass> = SmartPointerManager.createPointer(cursorClass)

    val cursorClass: PsiClass
        get() = pointer.element
            ?: throw IllegalStateException()

    val grainName: String?
        get() = getGrainName(cursorClass)

    val cursorName: String?
        get() = getObjectName(cursorClass)

    val cursorType: CursorType?
        get() = getCursorType(cursorClass)

    companion object {
        operator fun invoke(cursorClass: PsiClass): CelestaCursor = cursorClass.cachedValue {
            CelestaCursor(this)
        }

        private fun getGrainName(cursorClass: PsiClass): String? = cursorClass.cachedValue {
            getConstant(cursorClass, CelestaConstants.GRAIN_NAME_FIELD)
        }

        private fun getObjectName(cursorClass: PsiClass): String? = cursorClass.cachedValue {
            getConstant(cursorClass, CelestaConstants.OBJECT_NAME_FIELD)
        }

        private fun getCursorType(cursorClass: PsiClass): CursorType? = cursorClass.cachedValue {
            val psiFacade = JavaPsiFacade.getInstance(project)

            val tableCursor = psiFacade.findClass(CelestaConstants.CURSOR_FQN, cursorClass.resolveScope)
            tableCursor?.let {
                if(cursorClass.isInheritor(it, true))
                    return@cachedValue CursorType.TableCursor
            }

            val materializedViewCursor = psiFacade.findClass(CelestaConstants.MATERIALIZED_VIEW_CURSOR_FQN, cursorClass.resolveScope)
            materializedViewCursor?.let {
                if(cursorClass.isInheritor(it, true))
                    return@cachedValue CursorType.MaterializedViewCursor
            }

            return@cachedValue null
        }

        private fun getConstant(psiClass: PsiClass, name: String): String? {
            return psiClass.findFieldByName(name, false)
                ?.initializer
                ?.let {
                    JavaPsiFacade.getInstance(psiClass.project)
                        .constantEvaluationHelper.computeConstantExpression(it)
                } as? String
        }

    }
}

enum class CursorType {
    TableCursor, MaterializedViewCursor
}