package com.talent.animescrapsources.animesources

import com.talent.animescrapsources.animesources.sourceCommonExtractors.AsianExtractor
import com.talent.animescrap_common.model.AnimeDetails
import com.talent.animescrap_common.model.AnimeStreamLink
import com.talent.animescrap_common.model.SimpleAnime
import com.talent.animescrap_common.utils.Utils.get
import org.jsoup.Jsoup
import org.jsoup.select.Elements

class MyAsianTvSource : AnimeSource {
    private val mainUrl = "https://myasiantv.cx"
    override suspend fun animeDetails(contentLink: String): AnimeDetails {
        val url = "$mainUrl${contentLink}"
        val doc = Jsoup.parse(get(url))
        val animeCover = doc.selectFirst(".poster")!!.getElementsByTag("img").attr("src")
        val animeName = doc.selectFirst(".movie h1")!!.text()
        val animDesc = doc.selectFirst(".info")!!.text()

        val lastEpUrl = doc.selectFirst(".list-episode a")!!.attr("href")
        val lastEp = lastEpUrl.substringAfterLast("episode-").toInt()
        val epPrefix = lastEpUrl.replaceAfterLast("episode-", "")
        val subMap = mutableMapOf<String, String>()

        for (ep in 1..lastEp) {
            subMap["$ep"] = epPrefix + ep
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
        val searchUrl = "$mainUrl/search.html?key=${searchedText}"
        println(searchUrl)
        val allInfo = Jsoup.parse(get(searchUrl)).select(".items > li")
        println(allInfo)
        return getItems(allInfo)
    }

    private fun getItems(allInfo: Elements): ArrayList<SimpleAnime> {
        val animeList = arrayListOf<SimpleAnime>()
        for (item in allInfo) {
            val itemImage = item.getElementsByTag("img").attr("src")
            val itemName = item.getElementsByTag("img").attr("alt")
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
        return getItems(
            Jsoup.parse(get("$mainUrl/show/goblin")).select("#sidebarlist-2 div > a")
        )
    }

    override suspend fun trendingAnime(): ArrayList<SimpleAnime> {
        return getItems(
            Jsoup.parse(get("$mainUrl/anclytic.html?id=3"))
                .getElementsByTag("div")
        )

    }

    override suspend fun streamLink(
        animeUrl: String,
        animeEpCode: String,
        extras: List<String>?
    ): AnimeStreamLink {
        // Get the link of episode
        val animeEpUrl = "$mainUrl$animeEpCode"
        println(animeEpUrl)
        val doc = Jsoup.parse(get(animeEpUrl))
        val embedLink =
            "https:" + doc.getElementsByAttribute("data-video").first()!!.attr("data-video")
        println(embedLink)
        val link = AsianExtractor().getAsianStreamLink(embedLink)
        println(link)
        return AnimeStreamLink(link, "", true)

    }


}
