package com.talent.animescrap.ui.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.kittinunf.fuel.Fuel
import com.talent.animescrap.model.AnimeDetails
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jsoup.Jsoup

class AnimeDetailsViewModel : ViewModel() {
    private val _animeDetails = MutableLiveData<AnimeDetails>()
    private val _animeStreamLink = MutableLiveData<String>()

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

    fun getStreamLink(animeEpUrl: String) {

        var yugenEmbedLink = Jsoup.connect(animeEpUrl)
            .get().getElementById("main-embed")!!.attr("src")
        if (!yugenEmbedLink.contains("https:")) {
            yugenEmbedLink = "https:$yugenEmbedLink"
        }

        val mapOfHeaders = mutableMapOf(
            "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
            "Accept-Encoding" to "gzip, deflate",
            "Accept-Language" to "en-US,en;q=0.5",
            "Connection" to "keep-alive",
            "Upgrade-Insecure-Requests" to "1",
            "User-Agent" to "Mozilla/5.0 (X11; Linux x86_64; rv:89.0) Gecko/20100101 Firefox/89.0",
            "Host" to "yugen.to",
            "TE" to "Trailers",
            "Origin" to "https://yugen.to",
            "X-Requested-With" to "XMLHttpRequest",
            "Referer" to yugenEmbedLink
        )

        val apiRequest = "https://yugen.to/api/embed/"
        val id = yugenEmbedLink.split("/")
        val dataMap = mapOf("id" to id[id.size - 2], "ac" to "0")

        println(dataMap)

        try {
            Fuel.post(apiRequest, dataMap.toList()).header(mapOfHeaders)
                .response { _, _, results ->
                    val (bytes, _) = results
                    println("hi")
                    if (bytes != null) {
                        val json = ObjectMapper().readTree(String(bytes))
                        val link = json.get("hls").asText()
                        CoroutineScope(Dispatchers.Main).launch {
                            _animeStreamLink.value = link
                        }
                    }
                }

        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    val animeDetails: LiveData<AnimeDetails> = _animeDetails
    val animeStreamLink: LiveData<String> = _animeStreamLink
}