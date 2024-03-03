package ru.curs.celesta.ij.generated

import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import ru.curs.celesta.ij.CelestaConstants
import ru.curs.celesta.ij.cachedValue

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

            for (type in ObjectType.values()) {
                psiFacade.findClass(type.parentFqn, cursorClass.resolveScope)?.let {

                    if (cursorClass.isInheritor(it, true))
                        return@cachedValue type

                }
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
    TableCursor, MaterializedViewCursor, Sequence, View, Function;

    val parentFqn: String
        get() = when (this) {
            TableCursor -> CelestaConstants.CURSOR_FQN
            MaterializedViewCursor -> CelestaConstants.MATERIALIZED_VIEW_CURSOR_FQN
            Sequence -> CelestaConstants.SEQUENCE_FQN
            View -> CelestaConstants.VIEW_CURSOR_FQN
            Function -> CelestaConstants.PARAMETRIZED_VIEW_CURSOR_FQN
        }

}