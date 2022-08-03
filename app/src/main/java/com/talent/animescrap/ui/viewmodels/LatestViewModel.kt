package com.talent.animescrap.ui.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talent.animescrap.model.Photos
import com.talent.animescrap.utils.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LatestViewModel : ViewModel() {
    private val _latestAnimeList = MutableLiveData<ArrayList<Photos>>().apply {
        getLatestAnimeList()
    }

    private suspend fun getLatestAnimeFromSite(): ArrayList<Photos> = withContext(Dispatchers.IO) {
        Log.i("LatestViewModel", "Getting the latest anime")
        val picInfo = arrayListOf<Photos>()
        val url = "https://yugen.to/latest/"

        val doc = Utils().getJsoup(url)
        val allInfo = doc.getElementsByClass("ep-card")
        for (item in allInfo) {
            val itemImage = item.getElementsByTag("img").attr("data-src")
            val itemName = item.getElementsByClass("ep-origin-name").text()
            val itemLink = item.getElementsByClass("ep-details").attr("href")
            val picObject = Photos(itemName, itemImage, itemLink)
            picInfo.add(picObject)
        }
        return@withContext picInfo
    }

    fun getLatestAnimeList() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                getLatestAnimeFromSite().apply {
                    withContext(Dispatchers.Main) {
                        _latestAnimeList.value = this@apply
                    }
                }
            }
        }
    }

    val latestAnimeList: LiveData<ArrayList<Photos>> = _latestAnimeList
}