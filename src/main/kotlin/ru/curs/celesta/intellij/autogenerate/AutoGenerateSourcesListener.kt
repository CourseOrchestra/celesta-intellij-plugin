package ru.curs.celesta.intellij.autogenerate

import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import org.jetbrains.idea.maven.execution.MavenRunConfigurationType
import org.jetbrains.idea.maven.execution.MavenRunnerParameters
import org.jetbrains.idea.maven.execution.SoutMavenConsole
import org.jetbrains.idea.maven.project.MavenProject
import org.jetbrains.idea.maven.project.MavenProjectReader
import org.jetbrains.idea.maven.project.MavenProjectsManager
import ru.curs.celesta.intellij.CELESTA_NOTIFICATIONS
import ru.curs.celesta.intellij.maven.CelestaMavenManager
import java.io.File

class AutoGenerateSourcesListener : com.intellij.openapi.fileEditor.FileEditorManagerListener {
    override fun selectionChanged(event: FileEditorManagerEvent) {
        super.selectionChanged(event)

        val project = event.manager.project

        val sqlFile = event.oldFile ?: return

        if (sqlFile.name.contains("sql")) {
            val psiFile = PsiManager.getInstance(project).findFile(sqlFile) ?: return

            val mavenProject = CelestaMavenManager.getInstance(project).guessProject(sqlFile) ?: return

            val crc = psiFile.text.hashCode()

            val propertiesComponent = PropertiesComponent.getInstance(project)
            if (crc != propertiesComponent.getInt(sqlFile.name + "_crc", -1)) {

                val documentManager = PsiDocumentManager.getInstance(project)
                documentManager.getDocument(psiFile)?.let {
                    FileDocumentManager.getInstance().saveDocument(it)
                }

                propertiesComponent.setValue(sqlFile.name + "_crc", crc, -1)

                ProgressManager.getInstance().runProcessWithProgressAsynchronously(
                    GenerateSourcesTask(project, mavenProject),
                    EmptyProgressIndicator()
                )
            }
        }
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
            listOf("generate-sources"),
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
        val mavenGeneralSettings = projectsManager.generalSettings

        val result = MavenProjectReader.generateSources(
            projectsManager.embeddersManager.getEmbedder(
                mavenProject,
                Key.create<Any>("Foo")
            ),
            projectsManager.importingSettings,
            mavenProject.file,
            mavenProject.activatedProfilesIds,
            SoutMavenConsole(
                mavenGeneralSettings.outputLevel,
                mavenGeneralSettings.isPrintErrorStackTraces
            )
        ) ?: return

        if (result.readingProblems.isEmpty()) {
            VfsUtil.markDirtyAndRefresh(true, true, true, File(mavenProject.buildDirectory))
            return
        }

        for (problem in result.readingProblems) {
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