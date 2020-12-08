package ru.curs.celesta.intellij.generated

import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import ru.curs.celesta.intellij.CelestaConstants
import ru.curs.celesta.intellij.cachedValue

class CelestaGeneratedObject private constructor(cursorClass: PsiClass) {
    private val pointer: SmartPsiElementPointer<PsiClass> = SmartPointerManager.createPointer(cursorClass)

    val objectClass: PsiClass
        get() = pointer.element
            ?: throw IllegalStateException()

    val grainName: String?
        get() = getGrainName(objectClass)

    val objectName: String?
        get() = getObjectName(objectClass)

    val type: ObjectType?
        get() = getCursorType(objectClass)

    companion object {
        operator fun invoke(cursorClass: PsiClass): CelestaGeneratedObject = cursorClass.cachedValue {
            CelestaGeneratedObject(this)
        }

        private fun getGrainName(cursorClass: PsiClass): String? = cursorClass.cachedValue {
            getConstant(cursorClass, CelestaConstants.GRAIN_NAME_FIELD)
        }

        private fun getObjectName(cursorClass: PsiClass): String? = cursorClass.cachedValue {
            getConstant(cursorClass, CelestaConstants.OBJECT_NAME_FIELD)
        }

        private fun getCursorType(cursorClass: PsiClass): ObjectType? = cursorClass.cachedValue {
            val psiFacade = JavaPsiFacade.getInstance(project)

            val tableCursor = psiFacade.findClass(CelestaConstants.CURSOR_FQN, cursorClass.resolveScope)
            tableCursor?.let {
                if(cursorClass.isInheritor(it, true))
                    return@cachedValue ObjectType.TableCursor
            }

            val materializedViewCursor = psiFacade.findClass(CelestaConstants.MATERIALIZED_VIEW_CURSOR_FQN, cursorClass.resolveScope)
            materializedViewCursor?.let {
                if(cursorClass.isInheritor(it, true))
                    return@cachedValue ObjectType.MaterializedViewCursor
            }

            val sequenceClass = psiFacade.findClass(CelestaConstants.SEQUENCE_FQN, cursorClass.resolveScope)
            sequenceClass?.let {
                if(cursorClass.isInheritor(it, true))
                    return@cachedValue ObjectType.Sequence
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

enum class ObjectType {
    TableCursor, MaterializedViewCursor, Sequence
}