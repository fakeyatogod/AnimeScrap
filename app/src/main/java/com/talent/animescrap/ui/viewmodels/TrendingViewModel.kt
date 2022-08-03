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

class TrendingViewModel : ViewModel() {

    private val _trendingAnimeList = MutableLiveData<ArrayList<Photos>>().apply {
        getTrendingAnimeList()
    }

    private suspend fun getTrendingAnimeFromSite(): ArrayList<Photos> =
        withContext(Dispatchers.IO) {
            Log.i("TrendingViewModel", "Getting the trending anime")
            val animeList = arrayListOf<Photos>()
            val doc = Utils().getJsoup(url = "https://yugen.to/trending/")
            val allInfo = doc.getElementsByClass("anime-meta")
            for (item in allInfo) {
                val itemImage = item.getElementsByTag("img").attr("data-src")
                val itemName = item.getElementsByClass("anime-name").attr("title")
                val itemLink = item.attr("href")
                animeList.add(Photos(itemName, itemImage, itemLink))
            }

            return@withContext animeList
        }

    fun getTrendingAnimeList() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                getTrendingAnimeFromSite().apply {
                    withContext(Dispatchers.Main) {
                        _trendingAnimeList.value = this@apply
                    }
                }

            }
        }
    }

    val trendingAnimeList: LiveData<ArrayList<Photos>> = _trendingAnimeList
}