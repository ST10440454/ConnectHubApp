package com.connecthub.app.data.messaging

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.connecthub.app.ConnectHubApp
import com.connecthub.app.MainActivity
import com.connecthub.app.R
import com.connecthub.app.data.remote.PushTokenService
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class ConnectHubMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        PushTokenService().saveToken(token)
    }

    @SuppressLint("MissingPermission") // permission is checked below
    override fun onMessageReceived(message: RemoteMessage) {
        val container = (application as ConnectHubApp).container

        val enabled = runBlocking { container.settingsDataStore.settingsFlow.first().notificationsEnabled }
        if (!enabled) return

        // The chat screen already updates live while the app is visible.
        val appVisible = ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        if (appVisible) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        NotificationChannels.create(this)
        val openApp = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(this, NotificationChannels.MESSAGES)
            .setSmallIcon(R.drawable.ic_new_chat) // swap for a white-on-transparent icon later
            .setContentTitle(message.data["title"] ?: getString(R.string.app_name))
            .setContentText(message.data["body"].orEmpty())
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .build()

        // One notification per conversation: a new message replaces the previous one.
        NotificationManagerCompat.from(this)
            .notify(message.data["conversationId"].hashCode(), notification)
    }
}