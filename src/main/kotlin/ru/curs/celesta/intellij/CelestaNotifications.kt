package ru.curs.celesta.intellij

import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationGroupManager

val CELESTA_NOTIFICATIONS: NotificationGroup = NotificationGroupManager.getInstance()
    .getNotificationGroup("celesta.balloon")