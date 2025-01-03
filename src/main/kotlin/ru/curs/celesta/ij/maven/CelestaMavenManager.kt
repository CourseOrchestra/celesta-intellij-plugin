package ru.curs.celesta.ij.maven

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.ModuleListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.util.Pair
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.profile.codeInspection.ProjectInspectionProfileManager
import com.intellij.psi.search.scope.ProjectFilesScope
import com.intellij.util.Function
import org.jetbrains.annotations.NotNull
import org.jetbrains.idea.maven.project.MavenProject
import org.jetbrains.idea.maven.project.MavenProjectChanges
import org.jetbrains.idea.maven.project.MavenProjectsManager
import org.jetbrains.idea.maven.project.MavenProjectsTree
import org.jetbrains.idea.maven.server.NativeMavenProjectHolder
import ru.curs.celesta.ij.CelestaConstants
import java.io.File

class CelestaMavenManager(private val project: Project) : @NotNull Disposable {

    private val module2mavenProject: MutableMap<Module, MavenProject> = mutableMapOf()

    private val mavenProjectsManager = MavenProjectsManager.getInstance(project)

    private val modulesListener = object : ModuleListener {
        override fun modulesAdded(project: Project, modules: MutableList<out Module>) {
            scheduleUpdate()
        }

        override fun beforeModuleRemoved(project: Project, module: Module) {
            scheduleUpdate()
        }

        override fun moduleRemoved(project: Project, module: Module) {
            scheduleUpdate()
        }

        override fun modulesRenamed(
            project: Project,
            modules: MutableList<out Module>,
            oldNameProvider: Function<in Module, String>
        ) {
            scheduleUpdate()
        }
    }

    private val mavenListener = object : MavenProjectsTree.Listener {
        override fun projectResolved(
            projectWithChanges: Pair<MavenProject, MavenProjectChanges>,
            nativeMavenProject: NativeMavenProjectHolder?
        ) {
            scheduleUpdate()
        }

        override fun projectsUpdated(
            updated: MutableList<Pair<MavenProject, MavenProjectChanges>>,
            deleted: MutableList<MavenProject>
        ) {
            scheduleUpdate()
        }

        override fun pluginsResolved(project: MavenProject) {
            scheduleUpdate()
        }

        override fun foldersResolved(projectWithChanges: Pair<MavenProject, MavenProjectChanges>) {
            invokeLater {
                updateProjects()
                reloadGeneratedSources(projectWithChanges)
            }
        }

        override fun projectsIgnoredStateChanged(
            ignored: MutableList<MavenProject>,
            unignored: MutableList<MavenProject>,
            fromImport: Boolean
        ) {
            scheduleUpdate()
        }

        override fun profilesChanged() {
            scheduleUpdate()
        }
    }

    private fun scheduleUpdate() {
        invokeLater {
            updateProjects()
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
                    it.getChild("path")?.text?.replace('\\', '/')
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

            val module2ContentRoots = ModuleManager.getInstance(project).modules.associateWith {
                ModuleRootManager.getInstance(it).contentRoots
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
            currentProfile.disableTools(
                listOf("SqlNoDataSourceInspection", "SqlDialectInspection"),
                ProjectFilesScope(),
                project
            )
            currentProfile.disableToolByDefault(listOf("SqlNoDataSourceInspection", "SqlDialectInspection"), project)

            propertiesComponent.setValue(DATASOURCE_INSPECTION_DISABLED, true)
        }
    }

    override fun dispose() {}

    companion object {

        const val DATASOURCE_INSPECTION_DISABLED = "dataSourceInspectionDisabled2"
        fun getInstance(project: Project) = project.service<CelestaMavenManager>()
    }

    class StartupActivity : ProjectActivity {
        override suspend fun execute(project: Project) {
            val mavenProjectsManager = MavenProjectsManager.getInstance(project)
            val celestaMavenManager = project.service<CelestaMavenManager>()

            mavenProjectsManager.addProjectsTreeListener(celestaMavenManager.mavenListener)
            celestaMavenManager.updateProjects()

            project.messageBus.connect(celestaMavenManager)
                .subscribe(ModuleListener.TOPIC, celestaMavenManager.modulesListener)
        }
    }
}

