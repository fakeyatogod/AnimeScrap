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

class TrendingViewModel : ViewModel() {

    private var aList1 = arrayListOf<Photos>()

    private val _animeList1 = MutableLiveData<ArrayList<Photos>>().apply {

        CoroutineScope(Dispatchers.IO).launch {
            getTrendingAnime()
            withContext(Dispatchers.Main) {
                value = aList1

            }
        }
    }

    private fun getTrendingAnime() {
        val picInfo = arrayListOf<Photos>()
        val url = "https://yugen.to/trending/"

        val doc = Jsoup.connect(url).get()
        val allInfo = doc.getElementsByClass("anime-meta")
        for (item in allInfo) {
            val itemImage = item.getElementsByTag("img").attr("data-src")
            val itemName = item.getElementsByClass("anime-name").attr("title")
            val itemLink = item.attr("href")
            val picObject = Photos(itemName, itemImage, itemLink)
            picInfo.add(picObject)
        }


        aList1 = picInfo
    }

    val animeTrendingList: LiveData<ArrayList<Photos>> = _animeList1
}