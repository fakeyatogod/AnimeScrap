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

class SearchViewModel : ViewModel() {
    private val _searchedAnimeList = MutableLiveData<ArrayList<Photos>>()

    private suspend fun searchAnimeFromSite(searchUrl: String) = withContext(Dispatchers.IO) {
        Log.i("SearchViewModel", "Getting to search anime")
        val animeList = arrayListOf<Photos>()
        val doc = Utils().getJsoup(searchUrl)
        val allInfo = doc.getElementsByClass("anime-meta")
        for (item in allInfo) {
            val itemImage = item.getElementsByTag("img").attr("data-src")
            val itemName = item.getElementsByClass("anime-name").text()
            val itemLink = item.attr("href")
            val picObject = Photos(itemName, itemImage, itemLink)
            animeList.add(picObject)
        }

        return@withContext animeList
    }

    fun searchAnime(searchUrl: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                searchAnimeFromSite(searchUrl).apply {
                    withContext(Dispatchers.Main) {
                        _searchedAnimeList.value = this@apply
                    }
                }
            }
        }
    }

    val searchedAnimeList: LiveData<ArrayList<Photos>> = _searchedAnimeList
}