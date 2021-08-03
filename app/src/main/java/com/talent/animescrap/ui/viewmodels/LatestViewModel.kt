package com.talent.animescrap.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.talent.animescrap.model.Photos
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

class LatestViewModel : ViewModel() {

    private var aList1 = arrayListOf<Photos>()

    private val _animeList1 = MutableLiveData<ArrayList<Photos>>().apply {

        CoroutineScope(Dispatchers.IO).launch {
            getLatestAnime()
            withContext(Dispatchers.Main) {
                value = aList1

            }
        }
    }

    private fun getLatestAnime() {
        val picInfo = arrayListOf<Photos>()
        val url = "https://yugenani.me/latest/"

        val doc = Jsoup.connect(url).get()
        val allInfo = doc.getElementsByClass("ep-card")
        for (item in allInfo) {
            val itemImage = item.getElementsByClass("ep-origin-img")[0].getElementsByTag("img").attr("data-src")
            val itemName = item.getElementsByClass("ep-details").attr("alt")
            val itemLink = item.getElementsByClass("ep-details").attr("href")
            val picObject = Photos(itemName, itemImage, itemLink)
            picInfo.add(picObject)
        }


        aList1 = picInfo
    }

    val animeLatestList: LiveData<ArrayList<Photos>> = _animeList1
}