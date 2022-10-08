package com.talent.animescrap.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talent.animescrap.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

class MainViewModel : ViewModel() {
    private val githubReleaseLink = "https://github.com/fakeyatogod/AnimeScrap/releases/latest"
    private val githubAPKLink =
        "https://github.com/fakeyatogod/AnimeScrap/releases/download/TAG/AnimeScrap-vTAG.apk"

    private val _isUpdateAvailable = MutableLiveData<Pair<Boolean, String>>().apply {
        getRelease()
    }
    val isUpdateAvailable: LiveData<Pair<Boolean, String>> = _isUpdateAvailable

    private fun getRelease() {
        val currentVersion = BuildConfig.VERSION_NAME
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val doc = Jsoup.connect(githubReleaseLink).get()
                    val latestVersion =
                        Regex("[0-9]+\\.[0-9]+\\.[0-9]+").find(doc.toString())?.value
                    println("$currentVersion == $latestVersion = ${latestVersion == currentVersion}")
                    _isUpdateAvailable.postValue(
                        Pair(
                            latestVersion != currentVersion,
                            githubAPKLink.replace("TAG", latestVersion ?: currentVersion)
                        )
                    )
                } catch (_: Exception) {
                }
            }
        }
    }
}