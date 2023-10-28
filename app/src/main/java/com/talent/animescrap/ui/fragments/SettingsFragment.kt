package com.talent.animescrap.ui.fragments

import android.app.UiModeManager
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
import androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode
import androidx.lifecycle.ViewModelProvider
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.snackbar.Snackbar
import com.talent.animescrap.R
import com.talent.animescrap.ui.activities.MainActivity
import com.talent.animescrap.ui.viewmodels.UpdateViewModel
import com.talent.animescrap_common.utils.Utils.httpClient
import com.talent.animescrapsources.SourceSelector
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import kotlin.system.exitProcess

@AndroidEntryPoint
class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        val sourceCategory = preferenceScreen.findPreference<PreferenceCategory>("source_pref_category")

        val sourcePreference = ListPreference(requireContext())
        sourcePreference.key = "source"
        sourcePreference.title = getString(R.string.source_list_pref_title)
        val sourceList = SourceSelector(requireContext()).sourceMap.keys
        sourcePreference.entryValues = sourceList.toTypedArray()
        sourcePreference.entries = sourceList.map {
            it.replaceFirstChar { str -> if (str.isLowerCase()) str.titlecase(Locale.ROOT) else str.toString() }
                .replace("_", " ")
        }.toTypedArray()
        sourcePreference.setDefaultValue("yugen")
        sourcePreference.summaryProvider = Preference.SummaryProvider<ListPreference> { preference ->
            val newValue = preference.value
            "Source set to ${newValue?.uppercase()?.replace("_", " ")}"
        }
//        preferenceScreen.addPreference(sourceCategory)
        sourceCategory?.addPreference(sourcePreference)


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
            httpClient = httpClient.newBuilder().build()
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

        val uiModeManager =
            requireActivity().getSystemService(AppCompatActivity.UI_MODE_SERVICE) as UiModeManager
        if (uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION) {
            findPreference<SwitchPreferenceCompat>("pip")?.isVisible = false
        }

        findPreference<Preference>("check_update")?.setOnPreferenceClickListener {
            if (activity != null) {
                val updateViewModel = (activity as MainActivity).run {
                    ViewModelProvider(this)[UpdateViewModel::class.java]
                }
                updateViewModel.checkForNewUpdate()
                updateViewModel.isUpdateAvailable.observe(viewLifecycleOwner) { updateDetails ->
                    if (updateDetails.isUpdateAvailable) {
                        (activity as MainActivity).showUpdateAlertDialog(updateDetails)
                    } else {
                        (activity as MainActivity).showNoUpdateSnackBar()

                    }
                }
            }
            return@setOnPreferenceClickListener true
        }

    }
}