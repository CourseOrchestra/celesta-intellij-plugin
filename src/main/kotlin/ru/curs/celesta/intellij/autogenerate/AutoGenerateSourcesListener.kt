package ru.curs.celesta.intellij.autogenerate

import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
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
import com.intellij.util.castSafelyTo
import org.jetbrains.annotations.Nullable
import org.jetbrains.idea.maven.execution.MavenRunConfigurationType
import org.jetbrains.idea.maven.execution.MavenRunnerParameters
import org.jetbrains.idea.maven.project.MavenEmbeddersManager
import org.jetbrains.idea.maven.project.MavenProject
import org.jetbrains.idea.maven.project.MavenProjectsManager
import org.jetbrains.idea.maven.server.MavenServerExecutionResult
import ru.curs.celesta.intellij.CELESTA_NOTIFICATIONS
import ru.curs.celesta.intellij.CelestaConstants
import ru.curs.celesta.intellij.cachedValue
import ru.curs.celesta.intellij.maven.CelestaMavenManager
import ru.curs.celesta.intellij.scores.CelestaGrain
import java.io.File

class AutoGenerateSourcesListener(private val project: Project) : FileEditorManagerListener, BulkFileListener {
    override fun after(events: MutableList<out VFileEvent>) {
        if (!CelestaConstants.isCelestaProject(project)) return

        val suitableEvents = events.filter {
            it !is VFilePropertyChangeEvent && it !is VFileDeleteEvent
        }

        for (event in suitableEvents) {
            event.file?.let { processFile(it) }
        }
    }

    override fun selectionChanged(event: FileEditorManagerEvent) {
        if (!CelestaConstants.isCelestaProject(project)) return

        val file = event.oldFile ?: return

        processFile(file)
    }

    private fun processFile(file: @Nullable VirtualFile) {
        if (file.fileType != SqlFileType.INSTANCE)
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

private class GenerateSourcesTask(project: Project, val mavenProject: MavenProject) :
    Task.Backgroundable(project, "Generate Sources") {
    private val projectsManager = MavenProjectsManager.getInstance(project)

    override fun run(indicator: ProgressIndicator) {
        val embedder = projectsManager.embeddersManager.getEmbedder(
            mavenProject,
            MavenEmbeddersManager.FOR_PLUGINS_RESOLVE
        )

        val profiles = mavenProject.activatedProfilesIds

        val result: MavenServerExecutionResult = embedder.execute(
            mavenProject.file,
            profiles.enabledProfiles,
            profiles.disabledProfiles,
            listOf("generate-sources", "generate-test-sources")
        )

        if (result.problems.isEmpty()) {
            VfsUtil.markDirtyAndRefresh(true, true, true, File(mavenProject.buildDirectory))
            return
        }

        result.problems
            .takeIf {
                it.isNotEmpty()
            }?.joinToString(separator = "\n") {
                it.description
            }?.let {
                logger<GenerateSourcesTask>().warn("Errors during sources generation: $it")
            }

        for (problem in result.problems) {
            if (problem.description.contains("celesta-maven-plugin")) {
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

}