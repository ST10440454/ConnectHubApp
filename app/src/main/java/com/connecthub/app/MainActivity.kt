package com.connecthub.app

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.navigation.fragment.NavHostFragment
import com.connecthub.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    lateinit var container: AppContainer
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must be registered before super.onCreate() so fragments restored after a
        // recreate() (e.g. a language switch) are translated too.
        val languageManager = (application as ConnectHubApp).container.languageManager
        supportFragmentManager.registerFragmentLifecycleCallbacks(
            object : FragmentManager.FragmentLifecycleCallbacks() {
                override fun onFragmentViewCreated(
                    fm: FragmentManager, f: Fragment, v: View, savedInstanceState: Bundle?
                ) {
                    languageManager.translateViewTree(v)
                }
            },
            true
        )
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        container = (application as ConnectHubApp).container

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
        val navController = navHostFragment?.navController

        if (navController != null && savedInstanceState == null) {
            val navGraph = navController.navInflater.inflate(R.navigation.nav_graph)
            if (container.authRepository.currentUser() != null) {
                navGraph.setStartDestination(R.id.chatListFragment)
            }
            navController.graph = navGraph
        }
    }
}
