package com.talent.animescrap.ui.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talent.animescrap.model.AnimeDetails
import com.talent.animescrap.utils.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AnimeDetailsViewModel : ViewModel() {
    private val _animeDetails = MutableLiveData<AnimeDetails>()

    private suspend fun getAnimeDetailsFromSite(contentLink: String) = withContext(Dispatchers.IO) {
        val url = "https://yugen.to${contentLink}watch/?sort=episode"
        val doc = Utils().getJsoup(url)
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

        Log.i("AnimeDetailsViewModel", animeModel.toString())
        return@withContext animeModel
    }

    fun getAnimeDetails(contentLink: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                getAnimeDetailsFromSite(contentLink).apply {
                    withContext(Dispatchers.Main) {
                        _animeDetails.value = this@apply
                    }
                }
            }
        }
    }

    val animeDetails: LiveData<AnimeDetails> = _animeDetails
}