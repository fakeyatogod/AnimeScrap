package com.talent.animescrap.animesources

import com.talent.animescrap.model.AnimeDetails
import com.talent.animescrap.model.AnimeStreamLink
import com.talent.animescrap.model.SimpleAnime
import com.talent.animescrap.utils.Utils.getJsoup
import com.talent.animescrap.utils.Utils.postJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class YugenSource : AnimeSource {
    private val mainUrl = "https://yugenanime.ro"
    override suspend fun animeDetails(contentLink: String): AnimeDetails =
        withContext(Dispatchers.IO) {
            val url = "$mainUrl${contentLink}watch/?sort=episode"
            val doc = getJsoup(url)
            val animeContent = doc.getElementsByClass("p-10-t")
            val animeCover =
                doc.getElementsByClass("page-cover-inner").first()!!.getElementsByTag("img")
                    .attr("src")
            val animeName = animeContent.first()!!.text()
            val animDesc = animeContent[1].text()

            val subsEpCount = doc.getElementsByClass("box p-10 p-15 m-15-b anime-metadetails")
                .select("div:nth-child(6)").select("span").text()
            val epMapSub = (1..subsEpCount.toInt()).associate { it.toString() to it.toString() }
            val epMap = mutableMapOf("SUB" to epMapSub)

            try {
                val dubsEpCount = doc.getElementsByClass("box p-10 p-15 m-15-b anime-metadetails")
                    .select("div:nth-child(7)").select("span").text()
                println(dubsEpCount)
                val epMapDub = (1..dubsEpCount.toInt()).associate { it.toString() to it.toString() }
                epMap["DUB"] = epMapDub
            } catch (_: Exception) {
            }

            return@withContext AnimeDetails(animeName, animDesc, animeCover, epMap)
        }


    override suspend fun searchAnime(searchedText: String) = withContext(Dispatchers.IO) {
        val animeList = arrayListOf<SimpleAnime>()
        val searchUrl = "$mainUrl/search/?q=${searchedText}"

        val doc = getJsoup(searchUrl)
        val allInfo = doc.getElementsByClass("anime-meta")
        for (item in allInfo) {
            val itemImage = item.getElementsByTag("img").attr("data-src")
            val itemName = item.getElementsByClass("anime-name").text()
            val itemLink = item.attr("href")
            val picObject = SimpleAnime(itemName, itemImage, itemLink)
            animeList.add(picObject)
        }

        return@withContext animeList
    }

    override suspend fun latestAnime(): ArrayList<SimpleAnime> =
        withContext(Dispatchers.IO) {
            val animeList = arrayListOf<SimpleAnime>()
            val doc = getJsoup(url = "$mainUrl/latest/")
            val allInfo = doc.getElementsByClass("ep-card")
            for (item in allInfo) {
                val itemImage = item.getElementsByTag("img").attr("data-src")
                val itemName = item.getElementsByClass("ep-origin-name").text()
                val itemLink = item.getElementsByClass("ep-details").attr("href")
                animeList.add(SimpleAnime(itemName, itemImage, itemLink))
            }
            return@withContext animeList
        }

    override suspend fun trendingAnime(): ArrayList<SimpleAnime> =
        withContext(Dispatchers.IO) {
            val animeList = arrayListOf<SimpleAnime>()
            val doc = getJsoup(url = "$mainUrl/trending/")
            val allInfo = doc.getElementsByClass("series-item")
            for (item in allInfo) {
                val itemImage = item.getElementsByTag("img").attr("src")
                val itemName = item.getElementsByClass("series-title").text()
                val itemLink = item.attr("href")
                animeList.add(SimpleAnime(itemName, itemImage, itemLink))
            }
            return@withContext animeList
        }

    override suspend fun streamLink(
        animeUrl: String,
        animeEpCode: String,
        extras: List<String>?
    ): AnimeStreamLink =
        withContext(Dispatchers.IO) {
            // Get the link of episode
            val watchLink = animeUrl.replace("anime", "watch")

            val animeEpUrl =
                if (extras?.first() == "DUB")
                    "$mainUrl${
                        watchLink.dropLast(1)
                    }-dub/$animeEpCode"
                else "$mainUrl$watchLink$animeEpCode"

            var yugenEmbedLink =
                getJsoup(animeEpUrl).getElementById("main-embed")!!.attr("src")
            if (!yugenEmbedLink.contains("https:")) yugenEmbedLink = "https:$yugenEmbedLink"

            val mapOfHeaders = mutableMapOf(
                "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
                "Accept-Encoding" to "gzip, deflate",
                "Accept-Language" to "en-US,en;q=0.5",
                "Connection" to "keep-alive",
                "Upgrade-Insecure-Requests" to "1",
                "User-Agent" to "Mozilla/5.0 (X11; Linux x86_64; rv:89.0) Gecko/20100101 Firefox/89.0",
                "Host" to "yugen.to",
                "TE" to "Trailers",
                "Origin" to "$mainUrl",
                "X-Requested-With" to "XMLHttpRequest",
                "Referer" to yugenEmbedLink
            )

            val apiRequest = "$mainUrl/api/embed/"
            val id = yugenEmbedLink.split("/")
            val dataMap = mapOf("id" to id[id.size - 2], "ac" to "0")

            val linkDetails = postJson(apiRequest, mapOfHeaders, dataMap)!!.asJsonObject
            val link = linkDetails["hls"].asJsonArray.first().asString
            return@withContext AnimeStreamLink(link, "", true)

        }
}
