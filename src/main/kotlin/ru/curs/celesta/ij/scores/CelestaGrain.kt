package ru.curs.celesta.ij.scores

import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.*
import com.intellij.psi.util.*
import com.intellij.sql.psi.*
import com.intellij.sql.psi.impl.SqlTokenElement
import ru.curs.celesta.ij.cachedValue
import ru.curs.celesta.ij.maven.CelestaMavenManager
import ru.curs.celesta.ij.prev

class CelestaGrain private constructor(sqlFile: SqlFile) {
    private val pointer: SmartPsiElementPointer<SqlFile> = SmartPointerManager.createPointer(sqlFile)

    val sqlFile: SqlFile
        get() = pointer.element
            ?: throw IllegalStateException()

    val packageName: String
        get() = getPackageName(sqlFile)

    val grainName: String?
        get() = getScoreName(sqlFile)

    val tables: Map<String, SqlCreateStatement>
        get() = getTables(sqlFile)

    val materializedViews: Map<String, SqlTokenElement>
        get() = getMaterializedViews(sqlFile)

    val sequences: Map<String, SqlCreateStatement>
        get() = getSequences(sqlFile)

    val views: Map<String, SqlCreateStatement>
        get() = getViews(sqlFile)

    val functions: Map<String, SqlCreateStatement>
        get() = getFunctions(sqlFile)

    val isTestGrain: Boolean
        get() = isTestGrain(sqlFile)

    companion object {
        operator fun invoke(sqlFile: SqlFile): CelestaGrain = CachedValuesManager.getCachedValue(sqlFile) {
            return@getCachedValue CachedValueProvider.Result.create(CelestaGrain(sqlFile), sqlFile)
        }

        private fun getScoreName(sqlFile: SqlFile): String? = sqlFile.cachedValue {
            val leafElements: MutableList<PsiElement> = mutableListOf()

            accept(object : PsiRecursiveElementVisitor() {
                override fun visitElement(element: PsiElement) {
                    if(leafElements.size >= 3)
                        return

                    if(element !is PsiWhiteSpace && element !is PsiComment && element.children.isEmpty()) {
                        leafElements.add(element)
                    }
                    super.visitElement(element)
                }
            })

            return@cachedValue leafElements.getOrNull(2)?.takeIf {
                it.prev()?.text.equals("GRAIN", true) || it.prev()?.text.equals("SCHEMA", true)
            }?.text
        }

        private fun getTables(sqlFile: SqlFile): Map<String, SqlCreateStatement> = sqlFile.cachedValue {
            PsiTreeUtil.findChildrenOfType(sqlFile,  SqlCreateStatement::class.java)
                .filter { it.elementType == SqlElementTypes.SQL_CREATE_TABLE_STATEMENT }
                .associateBy { it.name }
        }

        private fun getSequences(sqlFile: SqlFile): Map<String, SqlCreateStatement> = sqlFile.cachedValue {
            PsiTreeUtil.findChildrenOfType(sqlFile,  SqlCreateStatement::class.java)
                .filter { it.elementType == SqlElementTypes.SQL_CREATE_SEQUENCE_STATEMENT }
                .associateBy { it.name }
        }

        private fun getViews(sqlFile: SqlFile): Map<String, SqlCreateStatement> = sqlFile.cachedValue {
            PsiTreeUtil.findChildrenOfType(sqlFile,  SqlCreateStatement::class.java)
                .filter { it.elementType == SqlElementTypes.SQL_CREATE_VIEW_STATEMENT }
                .associateBy { it.name }
        }

        private fun getFunctions(sqlFile: SqlFile): Map<String, SqlCreateStatement> = sqlFile.cachedValue {
            PsiTreeUtil.findChildrenOfType(sqlFile,  SqlCreateStatement::class.java)
                .filter { it.elementType == SqlElementTypes.SQL_CREATE_FUNCTION_STATEMENT }
                .associateBy { it.name }
        }

        private fun isTestGrain(sqlFile: SqlFile): Boolean = sqlFile.cachedValue {
            resolvePackageName(this, true) != null
        }

        private fun getPackageName(sqlFile: SqlFile): String = sqlFile.cachedValue {
            return@cachedValue resolvePackageName(this, false)
                ?: resolvePackageName(this, true)
                ?: ""
        }

        private fun resolvePackageName(sqlFile: SqlFile, isTestGrain: Boolean): String? {
            val project = sqlFile.project

            val celestaMavenManager = CelestaMavenManager.getInstance(project)

            val mavenProject = celestaMavenManager.guessProject(sqlFile.virtualFile.parent)
                ?: return null

            val sourcesRoots = if (isTestGrain)
                celestaMavenManager.getCelestaTestSourcesRoots(mavenProject)
            else
                celestaMavenManager.getCelestaSourcesRoots(mavenProject)

            return sourcesRoots
                .asSequence()
                .mapNotNull {
                    VfsUtil.getRelativePath(sqlFile.virtualFile.parent, it)?.replace('/', '.')
                }.firstOrNull()
        }

        private fun getMaterializedViews(sqlFile: SqlFile): Map<String, SqlTokenElement> = sqlFile.cachedValue {
            PsiTreeUtil.findChildrenOfType(sqlFile,  SqlTokenElement::class.java)
                .filter { it.elementType == SqlTokens.SQL_VIEW && it.prev()?.text.equals("MATERIALIZED", true) }
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