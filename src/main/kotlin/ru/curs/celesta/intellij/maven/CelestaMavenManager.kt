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

    fun getCelestaSourcesRoot(mavenProject: MavenProject): VirtualFile? {
        val module = getModule(mavenProject) ?: return null

//        todo parse properties

        return ModuleRootManager.getInstance(module)
            .contentRoots
            .mapNotNull {
                VfsUtil.findRelativeFile(it, "src", "main", "celestasql")
            }
            .firstOrNull()
    }

    fun getModule(mavenProject: MavenProject): Module? {
        return module2mavenProject
            .entries
            .firstOrNull { (_, project) -> project == mavenProject }
            ?.key
    }

    fun guessProject(sqlFile: VirtualFile): MavenProject? = runReadAction {
        for ((module, mavenProject) in module2mavenProject) {
            val contentRoots = ModuleRootManager.getInstance(module).contentRoots
            for (contentRoot in contentRoots) {
                if (VfsUtil.isAncestor(contentRoot, sqlFile, true)) {
                    return@runReadAction mavenProject
                }
            }
        }
        return@runReadAction null
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

