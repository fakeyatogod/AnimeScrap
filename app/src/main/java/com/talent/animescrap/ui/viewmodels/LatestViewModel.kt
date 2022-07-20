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

class LatestViewModel : ViewModel() {
    private val _animeList1 = MutableLiveData<ArrayList<Photos>>().apply {
        CoroutineScope(Dispatchers.IO).launch {
            getLatestAnime()
        }
    }

    fun getLatestAnime() {
        Log.i("$javaClass", "Getting the latest anime")
        val picInfo = arrayListOf<Photos>()
        val url = "https://yugen.to/latest/"

        val doc = Jsoup.connect(url).get()
        val allInfo = doc.getElementsByClass("ep-card")
        for (item in allInfo) {
            val itemImage = item.getElementsByTag("img").attr("data-src")
            val itemName = item.getElementsByClass("ep-origin-name").text()
            val itemLink = item.getElementsByClass("ep-details").attr("href")
            val picObject = Photos(itemName, itemImage, itemLink)
            picInfo.add(picObject)
        }

        CoroutineScope(Dispatchers.Main).launch {
            _animeList1.value = picInfo
        }
    }

    val animeLatestList: LiveData<ArrayList<Photos>> = _animeList1
}