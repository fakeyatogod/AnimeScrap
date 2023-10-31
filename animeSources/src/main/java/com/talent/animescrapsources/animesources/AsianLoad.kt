package com.talent.animescrapsources.animesources

import com.talent.animescrapsources.animesources.sourceCommonExtractors.AsianExtractor
import com.talent.animescrap_common.model.AnimeDetails
import com.talent.animescrap_common.model.AnimeStreamLink
import com.talent.animescrap_common.model.SimpleAnime
import com.talent.animescrap_common.source.AnimeSource
import com.talent.animescrap_common.utils.Utils.get
import org.jsoup.Jsoup

class AsianLoad : AnimeSource {
    private val mainUrl = "https://asianload.cc"
    override suspend fun animeDetails(contentLink: String): AnimeDetails {
        val url = "$mainUrl${contentLink}"
        val doc = Jsoup.parse(get(url))
        val animeCover = doc.selectFirst(".video-block")!!.getElementsByTag("img").attr("src")
        val animeName = doc.selectFirst(".video-details .date")!!.text()
        val animDesc = doc.selectFirst(".video-details .post-entry")!!.text()

        val eps = doc.selectFirst(".listing")!!.select("li")
        val subMap = mutableMapOf<String, String>()
        var totalEp = eps.size
        eps.forEach { epLi ->
            val link = epLi.getElementsByTag("a").attr("href")
//            val name = epLi.select(".name").text().replace(animeName,"")
            subMap[totalEp.toString()] = link
            totalEp--
        }

        val epMap = mutableMapOf("DEFAULT" to subMap)

        return AnimeDetails(
            animeName,
            animDesc,
            animeCover,
            epMap
        )
    }


    override suspend fun searchAnime(searchedText: String): ArrayList<SimpleAnime> {
        val searchUrl = "$mainUrl/search.html?keyword=${searchedText}"
        return getItems(searchUrl)
    }

    private fun getItems(url: String): ArrayList<SimpleAnime> {
        val animeList = arrayListOf<SimpleAnime>()
        val doc = Jsoup.parse(get(url))
        val allInfo = doc.getElementsByClass("video-block")
        for (item in allInfo) {
            val itemImage = item.getElementsByTag("img").attr("src")
            val itemName = item.getElementsByClass("name").text().substringBefore("Episode ")
            val itemLink = item.getElementsByTag("a").attr("href")
            animeList.add(
                SimpleAnime(
                    itemName,
                    itemImage,
                    itemLink
                )
            )
        }
        return animeList
    }

    override suspend fun latestAnime(): ArrayList<SimpleAnime> {
        return getItems(mainUrl)
    }

    override suspend fun trendingAnime(): ArrayList<SimpleAnime> {
        return getItems("$mainUrl/popular")

    }

    override suspend fun streamLink(
        animeUrl: String,
        animeEpCode: String,
        extras: List<String>?
    ): AnimeStreamLink {
        // Get the link of episode
        val animeEpUrl = "$mainUrl$animeEpCode"
        val doc = Jsoup.parse(get(animeEpUrl))

        val embedLink = "https:" + doc.selectFirst(".play-video")!!.getElementsByTag("iframe")
            .attr("src")
        println(embedLink)
        val link = AsianExtractor().getAsianStreamLink(embedLink)
        println(link)
        return AnimeStreamLink(link, "", true)

    }

}
