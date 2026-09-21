package com.connecthub.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import com.connecthub.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    lateinit var container: AppContainer
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        container = (application as ConnectHubApp).container

        // Decide the start destination based on whether a user is already logged in,
        // so re-opening the app skips straight to the Chats List if a session exists,
        // and only shows Onboarding to a fresh/logged-out user.
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val navGraph = navController.navInflater.inflate(R.navigation.nav_graph)

        if (container.authRepository.currentUser() != null) {
            navGraph.setStartDestination(R.id.chatListFragment)
        }
        navController.graph = navGraph
    }
}
