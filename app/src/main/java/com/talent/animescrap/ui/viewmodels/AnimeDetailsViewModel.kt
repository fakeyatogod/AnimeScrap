package com.talent.animescrap.ui.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.talent.animescrap.model.AnimeDetails
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jsoup.Jsoup

class AnimeDetailsViewModel : ViewModel() {
    private val _animeDetails = MutableLiveData<AnimeDetails>()

    fun getAnimeDetails(contentLink: String) {
        val url = "https://yugen.to${contentLink}watch/?sort=episode"
        val doc = Jsoup.connect(url).get()
        val animeContent = doc.getElementsByClass("p-10-t")
        val animeEpContent = doc.getElementsByClass("box p-10 p-15 m-15-b anime-metadetails")
            .select("div:nth-child(6)").select("span").text()
        val animeCover =
            doc.getElementsByClass("page-cover-inner").first()!!.getElementsByTag("img")
                .attr("data-src")
        val animeName = animeContent.first()!!.text()
        val animDesc = animeContent[1].text()

        val animeModel =
            AnimeDetails(animeName, animDesc, animeCover, animeEpContent)

        Log.i("$javaClass", animeModel.toString())

        CoroutineScope(Dispatchers.Main).launch {
            _animeDetails.value = animeModel
        }
    }

    val animeDetails: LiveData<AnimeDetails> = _animeDetails
}