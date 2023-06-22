package ru.curs.celesta.intellij.autogenerate

import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.JavaSdkVersion
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.intellij.sql.SqlFileType
import com.intellij.sql.psi.SqlFile
import org.jetbrains.annotations.Nullable
import org.jetbrains.idea.maven.buildtool.MavenSyncConsole
import org.jetbrains.idea.maven.execution.MavenExecutionOptions
import org.jetbrains.idea.maven.execution.MavenRunConfigurationType
import org.jetbrains.idea.maven.execution.MavenRunnerParameters
import org.jetbrains.idea.maven.project.*
import org.jetbrains.idea.maven.server.MavenServerExecutionResult
import org.jetbrains.idea.maven.utils.MavenProgressIndicator
import ru.curs.celesta.intellij.CELESTA_NOTIFICATIONS
import ru.curs.celesta.intellij.CelestaConstants
import ru.curs.celesta.intellij.cachedValue
import ru.curs.celesta.intellij.castSafelyTo
import ru.curs.celesta.intellij.maven.CelestaMavenManager
import ru.curs.celesta.intellij.scores.CelestaGrain
import java.io.File

class AutoGenerateSourcesListener(private val project: Project) : FileEditorManagerListener, BulkFileListener {
    override fun after(events: MutableList<out VFileEvent>) {
        if (ApplicationManager.getApplication().isUnitTestMode)
            return

        invokeLater {
            if (!CelestaConstants.isCelestaProject(project)) return@invokeLater

            val suitableEvents = events.filter {
                it !is VFilePropertyChangeEvent && it !is VFileDeleteEvent
            }

            for (event in suitableEvents) {
                event.file?.let { processFile(it) }
            }
        }
    }

    override fun selectionChanged(event: FileEditorManagerEvent) {
        if (ApplicationManager.getApplication().isUnitTestMode)
            return

        invokeLater {
            if (!CelestaConstants.isCelestaProject(project)) return@invokeLater

            val file = event.oldFile ?: return@invokeLater

            processFile(file)
        }
    }

    private fun processFile(file: @Nullable VirtualFile) {
        if (file.fileType != SqlFileType.INSTANCE || !file.isValid)
            return

        val celestaGrain =
            PsiManager.getInstance(project).findFile(file)
                ?.castSafelyTo<SqlFile>()?.let { CelestaGrain(it) }
                ?: return

        if (celestaGrain.grainName != null) {
            log.info("Unselect ${file.path}")

            val psiFile = PsiManager.getInstance(project).findFile(file) ?: return

            val mavenProject = CelestaMavenManager.getInstance(project).guessProject(file) ?: return

            val crc = psiFile.cachedValue {
                text.hashCode()
            }

            val propertiesComponent = PropertiesComponent.getInstance(project)
            val oldCrc = propertiesComponent.getInt(file.name + "_crc", -1)

            log.info("${file.path} crc is $crc. Last crc is $oldCrc")
            if (crc != oldCrc) {
                val documentManager = PsiDocumentManager.getInstance(project)
                documentManager.getDocument(psiFile)?.let {
                    FileDocumentManager.getInstance().saveDocument(it)
                }

                propertiesComponent.setValue(file.name + "_crc", crc, -1)

                log.info("Scheduling sources generation")

                ProgressManager.getInstance().runProcessWithProgressAsynchronously(
                    GenerateSourcesTask(project, mavenProject),
                    EmptyProgressIndicator()
                )
            }
        }
    }

    companion object {
        private val log = logger<AutoGenerateSourcesListener>()
    }
}

private class RunMavenAction(
    private val project: Project,
    private val mavenProject: MavenProject,
    private val projectsManager: MavenProjectsManager
) : NotificationAction("Re-run Maven Goal") {

    override fun actionPerformed(e: AnActionEvent, notification: Notification) {
        val params = MavenRunnerParameters(
            true,
            mavenProject.directory,
            mavenProject.file.name,
            listOf("generate-sources", "generate-test-sources"),
            projectsManager.explicitProfiles.enabledProfiles,
            projectsManager.explicitProfiles.disabledProfiles
        )

        MavenRunConfigurationType.runConfiguration(project, params, null)
    }
}

private var jdkProblemNotified: Boolean = false

private class GenerateSourcesTask(project: Project, val mavenProject: MavenProject) :
    Task.Backgroundable(project, "Generate Sources") {
    private val projectsManager = MavenProjectsManager.getInstance(project)

    override fun run(indicator: ProgressIndicator) {
        val jdkChanged: Boolean

        val projectSdk: Sdk? = ProjectRootManager.getInstance(project).projectSdk
        if (projectSdk != null) {
            if (!JavaSdk.getInstance().isOfVersionOrHigher(projectSdk, JavaSdkVersion.JDK_1_8)) {
                if (!jdkProblemNotified) {
                    jdkProblemNotified = true

                    CELESTA_NOTIFICATIONS
                        .createNotification(
                            "Only JDK 1.8 is supported now",
                            NotificationType.ERROR
                        )
                        .notify(project)
                }
                return
            }

            jdkProblemNotified = false

            val importingSettings = MavenWorkspaceSettingsComponent.getInstance(project).settings.getImportingSettings()

            jdkChanged = importingSettings.jdkForImporter != projectSdk.name

            importingSettings.jdkForImporter = projectSdk.name
        } else {
            if (!jdkProblemNotified) {
                jdkProblemNotified = true

                CELESTA_NOTIFICATIONS
                    .createNotification(
                        "Specify project JDK to generate celesta sql sources",
                        NotificationType.ERROR
                    )
                    .notify(project)
            }
            return
        }

        fun getEmbedder() = projectsManager.embeddersManager.getEmbedder(mavenProject, FOR_SOURCE_GENERATION)

        if (jdkChanged)
            projectsManager.embeddersManager.release(getEmbedder())

        val embedder = getEmbedder()

        val logsCollector: MutableList<String> = mutableListOf()

        val mavenProgressIndicator = MavenProgressIndicator(project) { MavenSyncConsole(project) }

        embedder.customizeForResolve(object : MavenConsole(MavenExecutionOptions.LoggingLevel.INFO, true) {
            override fun canPause(): Boolean = false

            override fun isOutputPaused(): Boolean = false

            override fun setOutputPaused(outputPaused: Boolean) {}

            override fun doPrint(text: String, type: OutputType?) {
                logsCollector.add(text)
            }
        }, mavenProgressIndicator)

        val profiles = mavenProject.activatedProfilesIds

        val result: MavenServerExecutionResult = embedder.execute(
            mavenProject.file,
            profiles.enabledProfiles,
            profiles.disabledProfiles,
            listOf("celesta:gen-cursors", "celesta:gen-score-resources", "celesta:gen-test-cursors", "celesta:gen-test-score-resources")
        )

        if (result.problems.isEmpty()) {
            VfsUtil.markDirtyAndRefresh(true, true, true, File(mavenProject.buildDirectory))
            return
        }

        logger<GenerateSourcesTask>().info(
            """Source generation failed: 
#========START MAVEN LOGS========
${logsCollector.joinToString(separator = "")} 
#========END MAVEN LOGS========
"""
        )

        result.problems
            .takeIf {
                it.isNotEmpty()
            }?.joinToString(separator = "\n") {
                it.description ?: ""
            }?.let {
                logger<GenerateSourcesTask>().warn("Errors during sources generation: $it")
            }

        for (problem in result.problems) {
            if (problem.description?.contains("celesta-maven-plugin") == true) {
                CELESTA_NOTIFICATIONS
                    .createNotification(
                        "Error during generation in celesta",
                        NotificationType.ERROR
                    )
                    .addAction(RunMavenAction(project, mavenProject, projectsManager))
                    .notify(project)
                return
            }
        }

        CELESTA_NOTIFICATIONS
            .createNotification(
                "Error during generation",
                NotificationType.ERROR
            )
            .addAction(RunMavenAction(project, mavenProject, projectsManager))
            .notify(project)
    }

    companion object {
        private val FOR_SOURCE_GENERATION: Key<*> =
            Key.create<Any>(AutoGenerateSourcesListener::class.java.toString() + ".FOR_SOURCE_GENERATION")
    }

}