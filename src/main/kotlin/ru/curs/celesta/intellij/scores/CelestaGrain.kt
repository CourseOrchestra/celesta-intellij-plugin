package ru.curs.celesta.intellij.scores

import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.*
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.elementType
import com.intellij.psi.util.nextLeaf
import com.intellij.sql.psi.SqlCreateStatement
import com.intellij.sql.psi.SqlElementTypes
import com.intellij.sql.psi.SqlFile
import com.intellij.sql.psi.SqlTokens
import com.intellij.sql.psi.impl.SqlTokenElement
import ru.curs.celesta.intellij.cachedValue
import ru.curs.celesta.intellij.maven.CelestaMavenManager

class CelestaGrain private constructor(sqlFile: SqlFile) {
    private val pointer: SmartPsiElementPointer<SqlFile> = SmartPointerManager.createPointer(sqlFile)

    val sqlFile: SqlFile
        get() = pointer.element
            ?: throw IllegalStateException()

    val packageName: String?
        get() = getPackageName(sqlFile)

    val grainName: String?
        get() = getScoreName(sqlFile)

    val tables: Map<String, SqlCreateStatement>
        get() = getTables(sqlFile)

    val materializedViews: Map<String, SqlTokenElement>
        get() = getViews(sqlFile)

    companion object {
        operator fun invoke(sqlFile: SqlFile): CelestaGrain = CachedValuesManager.getCachedValue(sqlFile) {
            return@getCachedValue CachedValueProvider.Result.create(CelestaGrain(sqlFile), sqlFile)
        }

        private fun getScoreName(sqlFile: SqlFile): String? = sqlFile.cachedValue {
            val firstIdentifier = children.firstOrNull { it.elementType == SqlTokens.SQL_IDENT }
                ?: return@cachedValue null

            return@cachedValue firstIdentifier.text
        }

        private fun getTables(sqlFile: SqlFile): Map<String, SqlCreateStatement> = sqlFile.cachedValue {
            children.filterIsInstance<SqlCreateStatement>()
                .filter { it.elementType == SqlElementTypes.SQL_CREATE_TABLE_STATEMENT }
                .associateBy { it.name }
        }

        private fun getPackageName(sqlFile: SqlFile): String? = sqlFile.cachedValue {
            val celestaMavenManager = CelestaMavenManager.getInstance(project)

            val mavenProject = celestaMavenManager.guessProject(containingFile.virtualFile.parent)
                ?: return@cachedValue null

            celestaMavenManager.getCelestaSourcesRoot(mavenProject)?.let {
                VfsUtil.getRelativePath(containingFile.virtualFile, it)
            }?.replace('/', '.')
        }

        private fun getViews(sqlFile: SqlFile): Map<String, SqlTokenElement> = sqlFile.cachedValue {
            children.filterIsInstance<SqlTokenElement>()
                .filter { it.elementType == SqlTokens.SQL_VIEW }
                .mapNotNull {
                    it.next() as? SqlTokenElement
                }.filter {
                    it.elementType == SqlTokens.SQL_IDENT
                }.associateBy {
                    it.text
                }
        }

        private fun SqlTokenElement.next(): PsiElement? =
            nextLeaf { it !is PsiWhiteSpace && it !is PsiComment }
    }
}