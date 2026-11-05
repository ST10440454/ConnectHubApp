package com.connecthub.app.data.messaging

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationChannels {
    const val MESSAGES = "messages"

    /** Safe to call repeatedly; Android ignores an existing channel. */
    fun create(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(MESSAGES, "Messages", NotificationManager.IMPORTANCE_HIGH)
            )
        }
    }
}