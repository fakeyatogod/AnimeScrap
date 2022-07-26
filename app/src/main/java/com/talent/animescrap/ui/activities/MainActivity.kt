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
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.talent.animescrap.R
import com.talent.animescrap.databinding.ActivityMainBottomNavBinding


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBottomNavBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBottomNavBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main_bottom_nav)

        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_favorite, R.id.navigation_latest, R.id.navigation_trending
            )
        )
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val transition = Slide(Gravity.BOTTOM).apply {
                duration = 200
                addTarget(navView)
            }
            TransitionManager.beginDelayedTransition(navView,transition)
            when (destination.id) {
                R.id.navigation_anime -> {

                    navView.visibility = View.GONE
                }
                R.id.navigation_search -> {
                    navView.visibility = View.GONE
                }
                else -> {
                    navView.visibility = View.VISIBLE
                }
            }
        }
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.topbar_menu, menu)
        val search = menu.findItem(R.id.navigation_search)
        val navController = findNavController(R.id.nav_host_fragment_activity_main_bottom_nav)

        search.setOnMenuItemClickListener {
            search.isVisible = false
            navController.popBackStack(R.id.navigation_latest,false)
            navController.navigate(R.id.navigation_search)
            return@setOnMenuItemClickListener true
        }
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.navigation_anime) {
                search.isVisible = false
            }
        }
        return super.onCreateOptionsMenu(menu)
    }


}