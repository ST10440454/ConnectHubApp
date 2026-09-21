package com.connecthub.app

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.connecthub.app.data.local.SettingsDataStore
import com.connecthub.app.data.remote.FirebaseAuthService
import com.connecthub.app.data.remote.FirestoreService
import com.connecthub.app.data.repository.AuthRepositoryImpl
import com.connecthub.app.data.repository.ContactRepositoryImpl
import com.connecthub.app.data.repository.MessageRepositoryImpl
import com.connecthub.app.domain.repository.AuthRepository
import com.connecthub.app.domain.repository.ContactRepository
import com.connecthub.app.domain.repository.MessageRepository
import com.connecthub.app.domain.usecase.LoginUserUseCase
import com.connecthub.app.domain.usecase.ObserveConversationsUseCase
import com.connecthub.app.domain.usecase.ObserveMessagesUseCase
import com.connecthub.app.domain.usecase.RegisterUserUseCase
import com.connecthub.app.domain.usecase.SendMessageUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AppContainer(application: Application) {

    private val authService = FirebaseAuthService()
    private val firestoreService = FirestoreService()

    val settingsDataStore = SettingsDataStore(application)

    val authRepository: AuthRepository = AuthRepositoryImpl(authService, firestoreService)
    val messageRepository: MessageRepository = MessageRepositoryImpl(firestoreService, authService)
    val contactRepository: ContactRepository = ContactRepositoryImpl(firestoreService, authService)

    val registerUserUseCase = RegisterUserUseCase(authRepository)
    val loginUserUseCase = LoginUserUseCase(authRepository)
    val sendMessageUseCase = SendMessageUseCase(messageRepository)
    val observeMessagesUseCase = ObserveMessagesUseCase(messageRepository)
    val observeConversationsUseCase = ObserveConversationsUseCase(contactRepository, messageRepository, authRepository)
}

class ConnectHubApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // Apply the user's saved dark-mode preference before any Activity is created,
        // so there's no flash of the wrong theme on cold start.
        CoroutineScope(Dispatchers.Main).launch {
            val settings = container.settingsDataStore.settingsFlow.first()
            AppCompatDelegate.setDefaultNightMode(
                if (settings.darkModeEnabled) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
        }
    }
}
