package com.talent.animescrap.ui.activities

import android.app.AlertDialog
import android.app.DownloadManager
import android.app.PictureInPictureParams
import android.app.UiModeManager
import android.content.SharedPreferences
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment.DIRECTORY_DOWNLOADS
import android.transition.Slide
import android.transition.TransitionManager
import android.view.Gravity
import android.view.Menu
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigationrail.NavigationRailView
import com.google.android.material.snackbar.Snackbar
import com.talent.animescrap.R
import com.talent.animescrap.databinding.ActivityMainBinding
import com.talent.animescrap.model.UpdateDetails
import com.talent.animescrap.ui.viewmodels.UpdateViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var settingsPreferenceManager: SharedPreferences
    private var isPipEnabled: Boolean = true
    private var isTV: Boolean = false

    private val updateViewModel: UpdateViewModel by viewModels()
    private var updateMessageIgnored = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updateMessageIgnored = savedInstanceState?.getBoolean("updateMessageIgnored") ?: false
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        settingsPreferenceManager = PreferenceManager.getDefaultSharedPreferences(this)

        // Check TV
        val uiModeManager = getSystemService(UI_MODE_SERVICE) as UiModeManager
        isTV = uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
        isPipEnabled = settingsPreferenceManager.getBoolean("pip", true)

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
            val bottomNavTransition = Slide(Gravity.BOTTOM).apply {
                duration = 200
                addTarget(bottomNavView)
            }
            TransitionManager.beginDelayedTransition(
                bottomNavView.parent as ViewGroup,
                bottomNavTransition
            )
            val railViewNavTransition = Slide(Gravity.START).apply {
                duration = 200
                addTarget(railView)
            }
            TransitionManager.beginDelayedTransition(
                railView.parent as ViewGroup,
                railViewNavTransition
            )
            when (destination.id) {
                R.id.navigation_anime, R.id.navigation_player -> {
                    bottomNavView.isVisible = false
                    railView.isVisible = false
                }
                R.id.navigation_search, R.id.navigation_settings -> {
                    bottomNavView.isVisible = false
                    railView.isVisible = isLandscape
                }
                else -> {
                    railView.isVisible = isLandscape
                    bottomNavView.isVisible = !isLandscape
                }
            }
            binding.toolbar.isVisible = destination.id != R.id.navigation_player
            binding.toolbar.isVisible = !isLandscape
            isPipEnabled = destination.id == R.id.navigation_player
            println("Destination is player = ${destination.id == R.id.navigation_player}")
            preparePip()
        }
        setupActionBarWithNavController(navController, appBarConfiguration)
        bottomNavView.setupWithNavController(navController)
        railView.setupWithNavController(navController)

        binding.toolbar.isVisible =
            !isLandscape && navController.currentDestination?.id != R.id.navigation_player

        updateViewModel.isUpdateAvailable.observe(this) { updateDetails ->
            if (updateDetails.isUpdateAvailable && !updateMessageIgnored) {
                showUpdateAlertDialog(updateDetails)
            }
        }
    }

    fun showUpdateAlertDialog(updateDetails: UpdateDetails) {
        val alertBuilder = AlertDialog.Builder(this)
        alertBuilder.setTitle(getString(R.string.update_available))
        alertBuilder.setMessage(updateDetails.description)
        alertBuilder.setPositiveButton("Update") { _, _ ->
            val request = DownloadManager.Request(Uri.parse(updateDetails.link))
            val downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
            val downloadTitle = updateDetails.link.replaceBeforeLast("/", "").replace("/", "")
            request.apply {
                setTitle(downloadTitle)
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(DIRECTORY_DOWNLOADS, downloadTitle)
            }
            downloadManager.enqueue(request)
            Snackbar.make(
                binding.container,
                getString(R.string.downloading_update),
                Snackbar.LENGTH_SHORT
            ).show()
        }
        alertBuilder.setNegativeButton("Ignore") { _, _ ->
            updateMessageIgnored = true
        }
        val dialog = alertBuilder.create()
        dialog.show()
    }

    fun showNoUpdateSnackBar() {
        Snackbar.make(
            binding.container, getString(R.string.update_not_available), Snackbar.LENGTH_LONG
        ).show()
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
                || destination.id == R.id.navigation_settings || destination.id == R.id.navigation_player
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

    private fun preparePip() {
        if (isTV || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        if (isPipEnabled) {
            println("PIP enabled")
            setPictureInPictureParams(
                PictureInPictureParams.Builder()
                    .setAutoEnterEnabled(true)
                    .build()
            )

        } else {
            println("PIP disabled")
            setPictureInPictureParams(
                PictureInPictureParams.Builder()
                    .setAutoEnterEnabled(false)
                    .build()
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (isPipEnabled && !isTV && Build.VERSION.SDK_INT < Build.VERSION_CODES.S)
            enterPictureInPictureMode(PictureInPictureParams.Builder().build())
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("updateMessageIgnored", updateMessageIgnored)
    }
}