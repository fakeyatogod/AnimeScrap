package com.talent.animescrap.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.kittinunf.fuel.Fuel
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

class SomeRepository {

    private fun getAnimeEmbedLink(animeEpUrl: String): String {
        var yugenEmbedLink = Jsoup.connect(animeEpUrl)
            .get().getElementById("main-embed")!!.attr("src")
        if (!yugenEmbedLink.contains("https:")) {
            yugenEmbedLink = "https:$yugenEmbedLink"
        }

        return yugenEmbedLink

    }

    suspend fun getStreamLink(animeEpUrl: String): String = withContext(Dispatchers.IO) {

        val yugenEmbedLink = getAnimeEmbedLink(animeEpUrl)

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

        val fuel = Fuel.post(apiRequest, dataMap.toList()).header(mapOfHeaders)
        val res = fuel.response().third
        val (bytes, _) = res
        println("hi new repo")
        if (bytes != null) {
            val linkDetails = JsonParser.parseString(String(bytes)).asJsonObject
            val link = linkDetails.get("hls")
            return@withContext link.asString
        }

        return@withContext "No Link Found"

    }
}

class AnimeStreamViewModel : ViewModel() {
    private val animeLink: MutableLiveData<String> = MutableLiveData()
    val liveData: LiveData<String> = animeLink

    fun setAnimeLink(animeEpUrl: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                animeLink.postValue(SomeRepository().getStreamLink(animeEpUrl))
            }
        }

    }
}