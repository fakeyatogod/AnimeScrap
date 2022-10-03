package com.talent.animescrap.ui.fragments

import android.app.UiModeManager
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate.*
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.snackbar.Snackbar
import com.talent.animescrap.R
import dagger.hilt.android.AndroidEntryPoint
import kotlin.system.exitProcess

@AndroidEntryPoint
class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
        val dynamicColorsPref = findPreference<SwitchPreferenceCompat>("dynamic_colors")
        dynamicColorsPref?.setOnPreferenceChangeListener { _, newValue ->
            view?.let {
                Snackbar.make(
                    it, "Dynamic colors colors ${
                        if (newValue.toString().toBoolean()) "enabled" else "disabled"
                    }, Restart App to take effect", Snackbar.LENGTH_SHORT
                ).setAction("Restart") {
                    requireContext().packageManager
                        .getLaunchIntentForPackage(requireContext().packageName)!!.apply {
                            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            startActivity(this)
                            exitProcess(0)
                        }
                }.show()
            }
            true
        }

        val sourcePref = findPreference<ListPreference>("source")
        sourcePref?.setOnPreferenceChangeListener { _, newValue ->
            startActivity(Intent.makeRestartActivityTask(activity?.intent?.component))
            Toast.makeText(
                requireContext(),
                "Source changed to ${
                    newValue.toString().uppercase().replace("_", " ")
                }",
                Toast.LENGTH_SHORT
            ).show()
            return@setOnPreferenceChangeListener true
        }

        findPreference<ListPreference>("dark_mode")?.setOnPreferenceChangeListener { _, newValue ->
            when (newValue.toString()) {
                "on" -> {
                    setDefaultNightMode(MODE_NIGHT_YES)
                }
                "off" -> {
                    setDefaultNightMode(MODE_NIGHT_NO)
                }
                else -> {
                    setDefaultNightMode(MODE_NIGHT_FOLLOW_SYSTEM)
                }
            }
            return@setOnPreferenceChangeListener true
        }

        // Hide theme section in versions that don't support dynamic colors.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            findPreference<SwitchPreferenceCompat>("dynamic_colors")?.isVisible = false
        }

        val uiModeManager = requireActivity().getSystemService(AppCompatActivity.UI_MODE_SERVICE) as UiModeManager
        if(uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION) {
            findPreference<SwitchPreferenceCompat>("pip")?.isVisible = false
        }

    }
}