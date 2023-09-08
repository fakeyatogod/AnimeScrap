package com.talent.animescrap.animesources

import com.talent.animescrap.model.AnimeDetails
import com.talent.animescrap.model.AnimeStreamLink
import com.talent.animescrap.model.SimpleAnime
import com.talent.animescrap.utils.Utils.getJsoup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AsianHDPlaySource : AnimeSource {
    private val mainUrl = "https://asianhdplay.pro"
    override suspend fun animeDetails(contentLink: String): AnimeDetails {
        val url = "$mainUrl${contentLink}"
        val doc = getJsoup(url)
        val animeCover = doc.selectFirst(".video-block")!!.getElementsByTag("img").attr("src")
        val animeName = doc.selectFirst(".video-details .date")!!.text()
        val animDesc = doc.selectFirst(".video-details .post-entry")!!.text()

        println(animeCover)
        println(animeName)
        println(animDesc)
        val eps = doc.selectFirst(".listing")!!.select("li")
        val subMap = mutableMapOf<String, String>()
        var totalEp = eps.size
        eps.forEach { epLi ->
            val link = epLi.getElementsByTag("a").attr("href")
//            val name = epLi.select(".name").text().replace(animeName,"")
            subMap[totalEp.toString()] = link
            totalEp--
        }
        println(eps)

//            val epMapSub = (1..subsEpCount.toInt()).associate { it.toString() to it.toString() }
        val epMap = mutableMapOf("DEFAULT" to subMap)

        return AnimeDetails(animeName, animDesc, animeCover, epMap)
    }


    override suspend fun searchAnime(searchedText: String): ArrayList<SimpleAnime> {
        val searchUrl = "$mainUrl/search.html?keyword=${searchedText}"
        return getItems(searchUrl)
    }

    private fun getItems(url: String): ArrayList<SimpleAnime> {
        val animeList = arrayListOf<SimpleAnime>()
        val doc = getJsoup(url)
        val allInfo = doc.getElementsByClass("video-block")
        for (item in allInfo) {
            val itemImage = item.getElementsByTag("img").attr("src")
            val itemName = item.getElementsByClass("name").text()
            val itemLink = item.getElementsByTag("a").attr("href")
            animeList.add(SimpleAnime(itemName, itemImage, itemLink))
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
    ): AnimeStreamLink =
        withContext(Dispatchers.IO) {
            // Get the link of episode
            val animeEpUrl = "$mainUrl$animeEpCode"

            var embedLink =
                getJsoup(animeEpUrl).selectFirst(".play-video")!!.getElementsByTag("iframe")
                    .attr("src")
            embedLink = embedLink.replaceBefore("streaming.php", "$mainUrl/download")
                .replace("streaming.php", "")
            println(embedLink)
            val link = getJsoup(embedLink).selectFirst(".mirror_link")!!.select("dowload").last()!!
                .getElementsByTag("a").attr("href")
            println(link)
            return@withContext AnimeStreamLink(link, "", true)

        }
}
