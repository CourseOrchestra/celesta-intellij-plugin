package ru.curs.celesta.intellij.maven

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.util.Pair
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.profile.codeInspection.ProjectInspectionProfileManager
import org.jetbrains.idea.maven.project.MavenProject
import org.jetbrains.idea.maven.project.MavenProjectChanges
import org.jetbrains.idea.maven.project.MavenProjectsManager
import org.jetbrains.idea.maven.project.MavenProjectsTree
import org.jetbrains.idea.maven.server.NativeMavenProjectHolder
import ru.curs.celesta.intellij.CelestaConstants
import java.io.File

class CelestaMavenManager(private val project: Project) {

    private val module2mavenProject: MutableMap<Module, MavenProject> = mutableMapOf()

    private val mavenProjectsManager = MavenProjectsManager.getInstance(project)

    private val listener = object : MavenProjectsTree.Listener {
        override fun projectResolved(
            projectWithChanges: Pair<MavenProject, MavenProjectChanges>,
            nativeMavenProject: NativeMavenProjectHolder?
        ) {
            updateProjects()
        }

        override fun projectsUpdated(
            updated: MutableList<Pair<MavenProject, MavenProjectChanges>>,
            deleted: MutableList<MavenProject>
        ) {
            updateProjects()
        }

        override fun foldersResolved(projectWithChanges: Pair<MavenProject, MavenProjectChanges>) {
            reloadGeneratedSources(projectWithChanges)
        }

    }

    fun getCelestaSourcesRoots(mavenProject: MavenProject): List<VirtualFile> {
        return getSourcesRoot(mavenProject, "scores", CelestaConstants.DEFAULT_SOURCE_PATH)
    }

    fun getCelestaTestSourcesRoots(mavenProject: MavenProject): List<VirtualFile> {
        return getSourcesRoot(mavenProject, "testScores", CelestaConstants.DEFAULT_TEST_SOURCE_PATH)
    }

    private fun getSourcesRoot(
        mavenProject: MavenProject,
        configElementName: String,
        defaultPath: String
    ): List<VirtualFile> {
        val module = getModule(mavenProject) ?: return emptyList()

        val configurationElement =
            mavenProject.plugins.firstOrNull { it.artifactId == "celesta-maven-plugin" && it.groupId == "ru.curs" }?.configurationElement

        val relativeScorePaths = (configurationElement?.let { configuration ->
            configuration.getChild(configElementName)
                ?.getChildren("score")
                ?.mapNotNull {
                    it.getChild("path")?.text
                }?.map {
                    it.removePrefix(mavenProject.path.removeSuffix("pom.xml"))
                }
        } ?: listOf()) + listOf(defaultPath)

        return ModuleRootManager.getInstance(module).contentRoots.flatMap { root ->
            relativeScorePaths.mapNotNull {
                VfsUtil.findRelativeFile(root, *it.split("/").toTypedArray())
            }
        }
    }

    fun getModule(mavenProject: MavenProject): Module? {
        return module2mavenProject
            .entries
            .firstOrNull { (_, project) -> project == mavenProject }
            ?.key
    }

    fun guessProject(sqlFile: VirtualFile): MavenProject? = runReadAction {
        val contentRoot2Module: MutableMap<VirtualFile, Module> = mutableMapOf()

        for ((module, _) in module2mavenProject) {
            val contentRoots = ModuleRootManager.getInstance(module).contentRoots
            for (contentRoot in contentRoots) {
                if (VfsUtil.isAncestor(contentRoot, sqlFile, true)) {
                    contentRoot2Module[contentRoot] = module
                }
            }
        }

        val nearestModule = contentRoot2Module.entries.sortedWith(Comparator.comparing({ it.key }) { rootA, rootB ->
            when {
                VfsUtil.isAncestor(rootA, rootB, true) -> 1
                VfsUtil.isAncestor(rootB, rootA, true) -> -1
                else -> 0
            }
        }).firstOrNull()?.value ?: return@runReadAction null

        return@runReadAction module2mavenProject[nearestModule]
    }

    private fun reloadGeneratedSources(projectWithChanges: Pair<MavenProject, MavenProjectChanges>) {
        ApplicationManager.getApplication().invokeLater {
            VfsUtil.markDirtyAndRefresh(true, true, true, File(projectWithChanges.first.buildDirectory))
        }
    }

    private fun updateProjects() {
        if (!ApplicationManager.getApplication().isDispatchThread) {
            ApplicationManager.getApplication().invokeLater { updateProjects() }
            return
        }

        runWriteAction {
            module2mavenProject.clear()

            val module2ContentRoots = ModuleManager.getInstance(project).modules.associate {
                it to ModuleRootManager.getInstance(it).contentRoots
            }

            for (mavenProject in mavenProjectsManager.projects) {
                modules@ for ((module, contentRoots) in module2ContentRoots) {
                    for (contentRoot in contentRoots) {
                        if (mavenProject.file.parent == contentRoot) {
                            module2mavenProject[module] = mavenProject
                            break@modules
                        }
                    }
                }
            }
        }

        disableInspectionsIfNeeded()
    }

    private fun disableInspectionsIfNeeded() {
        val hasCelesta = mavenProjectsManager.projects.any { mavenProject ->
            mavenProject.dependencies.any {
                it.groupId == "ru.curs" && it.artifactId == "celesta-core"
            }
        }

        if (!hasCelesta)
            return

        val propertiesComponent = PropertiesComponent.getInstance(project)

        if (!propertiesComponent.getBoolean(DATASOURCE_INSPECTION_DISABLED, false)) {

            val currentProfile = ProjectInspectionProfileManager.getInstance(project).currentProfile
            currentProfile.disableToolByDefault(listOf("SqlNoDataSourceInspection", "SqlDialectInspection"), project)

            propertiesComponent.setValue(DATASOURCE_INSPECTION_DISABLED, true)
        }
    }

    companion object {
        const val DATASOURCE_INSPECTION_DISABLED = "dataSourceInspectionDisabled"

        fun getInstance(project: Project) = project.service<CelestaMavenManager>()
    }

    class StartupActivity : com.intellij.openapi.startup.StartupActivity {
        override fun runActivity(project: Project) {
            val mavenProjectsManager = MavenProjectsManager.getInstance(project)
            val celestaMavenManager = project.service<CelestaMavenManager>()

            mavenProjectsManager.addProjectsTreeListener(celestaMavenManager.listener)

            celestaMavenManager.updateProjects()
        }
    }

}

