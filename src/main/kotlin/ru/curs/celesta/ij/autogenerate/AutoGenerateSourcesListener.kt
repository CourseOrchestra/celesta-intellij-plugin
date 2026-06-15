package ru.curs.celesta.ij.autogenerate

import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
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
import com.intellij.util.concurrency.AppExecutorUtil
import ru.curs.celesta.ij.CELESTA_NOTIFICATIONS
import ru.curs.celesta.ij.CelestaConstants
import ru.curs.celesta.ij.cachedValue
import ru.curs.celesta.ij.maven.CelestaMavenManager
import ru.curs.celesta.ij.scores.CelestaGrain
import ru.curs.celesta.score.ParseException
import java.io.File
import java.util.concurrent.Callable

class AutoGenerateSourcesListener(private val project: Project) : FileEditorManagerListener, BulkFileListener {
    override fun after(events: MutableList<out VFileEvent>) {
        val suitableEvents = events.filter {
            it !is VFilePropertyChangeEvent && it !is VFileDeleteEvent
        }

        runIfCelestaProject {
            for (event in suitableEvents) {
                event.file?.let { processFile(it) }
            }
        }
    }

    override fun selectionChanged(event: FileEditorManagerEvent) {
        val file = event.oldFile ?: return

        runIfCelestaProject {
            processFile(file)
        }
    }

    /**
     * Verifies off the EDT that this is a Celesta project, then runs [action] on the EDT.
     * [CelestaConstants.isCelestaProject] resolves a class through the indexes, which must not happen
     * on the EDT (it triggers "Slow operations are prohibited on EDT").
     */
    private fun runIfCelestaProject(action: () -> Unit) {
        if (ApplicationManager.getApplication().isUnitTestMode)
            return

        ReadAction.nonBlocking(Callable { CelestaConstants.isCelestaProject(project) })
            .inSmartMode(project)
            .expireWhen { project.isDisposed }
            .finishOnUiThread(ModalityState.nonModal()) { isCelestaProject ->
                if (isCelestaProject) action()
            }
            .submit(AppExecutorUtil.getAppExecutorService())
    }

    private fun processFile(file: VirtualFile) {
        if (file.fileType != SqlFileType.INSTANCE || !file.isValid)
            return

        val psiFile = PsiManager.getInstance(project).findFile(file) as? SqlFile ?: return

        if (CelestaGrain(psiFile).grainName == null)
            return

        val module = ModuleUtilCore.findModuleForFile(file, project) ?: return
        val celestaManager = CelestaMavenManager.getInstance(project)

        val crc = psiFile.cachedValue { text.hashCode() }
        val propertiesComponent = PropertiesComponent.getInstance(project)
        val oldCrc = propertiesComponent.getInt(file.name + "_crc", -1)

        log.info("${file.path} crc is $crc. Last crc is $oldCrc")
        if (crc == oldCrc)
            return

        val buildDirectory = celestaManager.buildDirectory(module) ?: return
        val mainScoreRoots = celestaManager.getMainScoreRoots(module).map { File(it.path) }
        val testScoreRoots = celestaManager.getTestScoreRoots(module).map { File(it.path) }

        PsiDocumentManager.getInstance(project).getDocument(psiFile)?.let {
            FileDocumentManager.getInstance().saveDocument(it)
        }
        propertiesComponent.setValue(file.name + "_crc", crc, -1)

        log.info("Scheduling sources generation")

        ProgressManager.getInstance().runProcessWithProgressAsynchronously(
            GenerateSourcesTask(
                project,
                mainScoreRoots = mainScoreRoots,
                testScoreRoots = testScoreRoots,
                buildDirectory = buildDirectory,
                snakeToCamel = celestaManager.isSnakeToCamel(module)
            ),
            EmptyProgressIndicator()
        )
    }

    companion object {
        private val log = logger<AutoGenerateSourcesListener>()
    }
}

private class GenerateSourcesTask(
    project: Project,
    private val mainScoreRoots: List<File>,
    private val testScoreRoots: List<File>,
    private val buildDirectory: File,
    private val snakeToCamel: Boolean
) : Task.Backgroundable(project, "Generating Celesta cursors") {

    override fun run(indicator: ProgressIndicator) {
        try {
            // Mirror the Maven plugin's layout: cursors under generated-sources, score resources under
            // generated-resources/score (the "score" subdir is where Celesta's
            // ScoreByScoreResourceDiscovery looks once the resources reach the classpath).
            CelestaGenerator.generate(
                mainScoreRoots,
                File(buildDirectory, "generated-sources/celesta"),
                File(buildDirectory, "generated-resources/score"),
                snakeToCamel
            )
            CelestaGenerator.generate(
                testScoreRoots,
                File(buildDirectory, "generated-test-sources/celesta"),
                File(buildDirectory, "generated-test-resources/score"),
                snakeToCamel
            )
        } catch (e: ParseException) {
            // Parse errors are already shown in the editor by CelestaSqlParseAnnotator; surface a
            // short notification so the user knows generation was skipped.
            log.warn("Celesta score generation failed: ${e.message}")
            CELESTA_NOTIFICATIONS
                .createNotification(e.message ?: "Error parsing Celesta score", NotificationType.WARNING)
                .notify(project)
            return
            // A background task must not let any generation failure (e.g. CelestaException) escape.
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            log.warn("Celesta cursor generation failed", e)
            CELESTA_NOTIFICATIONS
                .createNotification("Error generating Celesta cursors: ${e.message}", NotificationType.ERROR)
                .notify(project)
            return
        }

        VfsUtil.markDirtyAndRefresh(true, true, true, buildDirectory)
    }

    companion object {
        private val log = logger<GenerateSourcesTask>()
    }
}
