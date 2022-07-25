package com.talent.animescrap.ui.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.talent.animescrap.model.Photos
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jsoup.Jsoup

class SearchViewModel : ViewModel() {
    private val _animeList1 = MutableLiveData<ArrayList<Photos>>()

    fun searchAnime(searchUrl: String) {
        Log.i("$javaClass", "Getting to search anime")
        val picInfo = arrayListOf<Photos>()
        val doc = Jsoup.connect(searchUrl).get()
        val allInfo = doc.getElementsByClass("anime-meta")
        for (item in allInfo) {
            val itemImage = item.getElementsByTag("img").attr("data-src")
            val itemName = item.getElementsByClass("anime-name").text()
            val itemLink = item.attr("href")
            val picObject = Photos(itemName, itemImage, itemLink)
            picInfo.add(picObject)
        }

        CoroutineScope(Dispatchers.Main).launch {
            _animeList1.value = picInfo
        }
    }

    val animeLatestList: LiveData<ArrayList<Photos>> = _animeList1
}