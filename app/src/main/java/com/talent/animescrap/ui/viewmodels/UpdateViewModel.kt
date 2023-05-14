package com.talent.animescrap.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talent.animescrap.BuildConfig
import com.talent.animescrap.model.UpdateDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

class UpdateViewModel : ViewModel() {
    private val githubReleaseLink = "https://github.com/fakeyatogod/AnimeScrap/releases/latest"
    private val githubAPKLink =
        "https://github.com/fakeyatogod/AnimeScrap/releases/download/TAG/AnimeScrap-vTAG.apk"

    private val _isUpdateAvailable = MutableLiveData<UpdateDetails>().apply {
        checkForNewUpdate()
    }
    val isUpdateAvailable: LiveData<UpdateDetails> = _isUpdateAvailable

    fun checkForNewUpdate() {
        val currentVersion = BuildConfig.VERSION_NAME
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val doc = Jsoup.connect(githubReleaseLink).get()
                    val updateDesc = try {
                        doc.select(".markdown-body").text()
                    } catch (_: Exception) {
                        "No Update Description found"
                    }
                    println("Update desc = $updateDesc")
                    val latestVersion =
                        Regex("\\d+\\.\\d+\\.\\d+").find(doc.toString())?.value
                    println("$currentVersion == $latestVersion = ${latestVersion == currentVersion}")
                    _isUpdateAvailable.postValue(
                        UpdateDetails(
                            latestVersion != currentVersion,
                            githubAPKLink.replace("TAG", latestVersion ?: currentVersion),
                            updateDesc
                        )
                    )
                } catch (_: Exception) {
                }
            }
        }
    }
}