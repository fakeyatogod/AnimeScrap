package com.talent.animescrap.ui.fragments

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.PreferenceCategory
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
            view?.let {
                Snackbar.make(
                    it,
                    "Source changed to $newValue, Restart App to take effect",
                    Snackbar.LENGTH_SHORT
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

        // Hide theme section in versions that don't support dynamic colors.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            findPreference<PreferenceCategory>("theme")?.isVisible = false
        }

    }
}