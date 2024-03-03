package ru.curs.celesta.ij

import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationGroupManager

val CELESTA_NOTIFICATIONS: NotificationGroup = NotificationGroupManager.getInstance()
    .getNotificationGroup("celesta.balloon")