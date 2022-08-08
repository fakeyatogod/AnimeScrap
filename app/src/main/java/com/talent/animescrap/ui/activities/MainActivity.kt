package com.talent.animescrap.ui.activities

import android.os.Bundle
import android.transition.Slide
import android.transition.TransitionManager
import android.view.Gravity
import android.view.Menu
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.talent.animescrap.R
import com.talent.animescrap.databinding.ActivityMainBottomNavBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBottomNavBinding
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBottomNavBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main_bottom_nav)

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_favorite, R.id.navigation_latest, R.id.navigation_trending
            )
        )
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val transition = Slide(Gravity.BOTTOM).apply {
                duration = 200
                addTarget(navView)
            }
            TransitionManager.beginDelayedTransition(navView, transition)
            if (destination.id == R.id.navigation_anime || destination.id == R.id.navigation_search
                || destination.id == R.id.settingsFragment
            ) {
                navView.visibility = View.GONE
            } else {
                navView.visibility = View.VISIBLE
            }
        }
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.topbar_menu, menu)
        val search = menu.findItem(R.id.navigation_search)
        val settings = menu.findItem(R.id.navigation_settings)
        val navController = findNavController(R.id.nav_host_fragment_activity_main_bottom_nav)

        search.setOnMenuItemClickListener {
            navController.navigate(R.id.navigation_search)
            return@setOnMenuItemClickListener true
        }
        settings.setOnMenuItemClickListener {
            navController.navigate(R.id.settingsFragment)
            return@setOnMenuItemClickListener true
        }
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.navigation_anime || destination.id == R.id.navigation_search
                || destination.id == R.id.settingsFragment
            ) {
                search.isVisible = false
                settings.isVisible = false
            }
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_activity_main_bottom_nav)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }
}