package com.talent.animescrap.ui.activities

import android.content.res.Configuration
import android.os.Bundle
import android.transition.Slide
import android.transition.TransitionManager
import android.view.Gravity
import android.view.Menu
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigationrail.NavigationRailView
import com.talent.animescrap.R
import com.talent.animescrap.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val bottomNavView: BottomNavigationView = binding.navView
        val railView: NavigationRailView = binding.navRail

        val navController = findNavController(R.id.nav_host_fragment_activity_main_bottom_nav)

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_favorite, R.id.navigation_latest, R.id.navigation_trending
            )
        )
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val transition = Slide(Gravity.BOTTOM).apply {
                duration = 200
                addTarget(bottomNavView)
            }
            TransitionManager.beginDelayedTransition(bottomNavView, transition)
            val transition2 = Slide(Gravity.START).apply {
                duration = 200
                addTarget(railView)
            }
            TransitionManager.beginDelayedTransition(railView, transition2)
            if (destination.id == R.id.navigation_anime) {
                if (!isLandscape)
                    bottomNavView.visibility = View.GONE
                else
                    railView.isVisible = false
            } else if (destination.id == R.id.navigation_search
                || destination.id == R.id.navigation_settings
            ) {
                if (!isLandscape)
                    bottomNavView.visibility = View.GONE
            } else {
                if (!isLandscape)
                    bottomNavView.visibility = View.VISIBLE
                else
                    railView.isVisible = true
            }
        }
        setupActionBarWithNavController(navController, appBarConfiguration)
        bottomNavView.setupWithNavController(navController)
        railView.setupWithNavController(navController)

        if (isLandscape) {
            railView.isVisible = true
            bottomNavView.isVisible = false
            binding.toolbar.isVisible = false
        } else {
            railView.isVisible = false
            bottomNavView.isVisible = true
            binding.toolbar.isVisible = true
        }
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
            navController.navigate(R.id.navigation_settings)
            return@setOnMenuItemClickListener true
        }
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.navigation_anime || destination.id == R.id.navigation_search
                || destination.id == R.id.navigation_settings
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