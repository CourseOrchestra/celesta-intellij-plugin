package ru.curs.celesta.intellij.linemarkers

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import ru.curs.celesta.intellij.CELESTA_NOTIFICATIONS

abstract class BaseNavigator<T : PsiElement>(protected val project: Project) : GutterIconNavigationHandler<T> {
    fun notificationOnFail(search: () -> PsiElement?): PsiElement? {
        return try {
            search()
        } catch (e: SearchException) {
            CELESTA_NOTIFICATIONS.createNotification(
                e.text,
                NotificationType.WARNING
            ).notify(project)
            null
        }
    }

    fun fail(text: String): Nothing = throw SearchException(text)

    class SearchException(val text: String) : Exception()
}