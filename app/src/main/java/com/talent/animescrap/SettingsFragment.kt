package com.talent.animescrap

import android.content.Intent
import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.snackbar.Snackbar
import kotlin.system.exitProcess

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
                    val intent =
                        context!!.packageManager.getLaunchIntentForPackage(context!!.packageName)!!
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    startActivity(intent)
                    exitProcess(0)
                }.show()
            }
            true
        }

    }
}