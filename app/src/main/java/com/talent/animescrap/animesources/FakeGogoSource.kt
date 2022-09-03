package com.talent.animescrap.animesources

import com.github.kittinunf.fuel.Fuel
import com.google.gson.JsonParser
import com.talent.animescrap.model.AnimeDetails
import com.talent.animescrap.model.SimpleAnime
import com.talent.animescrap.utils.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FakeGogoSource : AnimeSource {
    private val mainUrl = "https://gogoanime.nl"
    override suspend fun animeDetails(contentLink: String): AnimeDetails =
        withContext(Dispatchers.IO) {
            val url = "$mainUrl${contentLink}/ep-1"
            println(url)
            val doc = Utils().getJsoup(url)
            println(doc)
            val animeContent = doc.getElementById("w-info")!!
            val animeCover = animeContent.getElementsByTag("img").attr("src")
            val animeName = animeContent.getElementsByClass("title").text()
            val animDesc = animeContent.getElementsByClass("synopsis").text()

            val num = doc.getElementsByClass("dropdown-item").last()!!.text().substringAfter("-")
            println(num)
            val animeEpContent = (1..num.toInt()).associate { it.toString() to it.toString() }

            return@withContext AnimeDetails(animeName, animDesc, animeCover, animeEpContent)
        }


    override suspend fun searchAnime(searchedText: String) = withContext(Dispatchers.IO) {
        val animeList = arrayListOf<SimpleAnime>()

        val searchUrl = "$mainUrl/filter?keyword=${searchedText}"

        val doc = Utils().getJsoup(searchUrl)
        val allInfo = doc.getElementsByClass("item")
        for (item in allInfo) {
            val itemImage = item.getElementsByTag("img").attr("src")
            val itemName = item.getElementsByClass("name").text()
            val itemLink = item.getElementsByClass("name").attr("href")
            animeList.add(SimpleAnime(itemName, itemImage, itemLink))
        }
        return@withContext animeList
    }

    override suspend fun latestAnime(): ArrayList<SimpleAnime> =
        withContext(Dispatchers.IO) {
            val animeList = arrayListOf<SimpleAnime>()
            val doc = Utils().getJsoup(url = "$mainUrl/updated")
            val allInfo = doc.getElementsByClass("item")
            for (item in allInfo) {
                val itemImage = item.getElementsByTag("img").attr("src")
                val itemName = item.getElementsByClass("name").text()
                val itemLink = item.getElementsByClass("name").attr("href")
                animeList.add(SimpleAnime(itemName, itemImage, itemLink))
            }
            return@withContext animeList
        }

    override suspend fun trendingAnime(): ArrayList<SimpleAnime> =
        withContext(Dispatchers.IO) {
            val animeList = arrayListOf<SimpleAnime>()
            val doc = Utils().getJsoup(url = "$mainUrl/filter?keyword=&sort=trending&vrf=")
            val allInfo = doc.getElementsByClass("item")
            for (item in allInfo) {
                val itemImage = item.getElementsByTag("img").attr("src")
                val itemName = item.getElementsByClass("name").text()
                val itemLink = item.getElementsByClass("name").attr("href")
                animeList.add(SimpleAnime(itemName, itemImage, itemLink))
            }
            return@withContext animeList
        }

    override suspend fun streamLink(animeUrl: String, animeEpCode: String): Pair<String, String?> =
        withContext(Dispatchers.IO) {
            // Get the link of episode
            val watchLink = animeUrl.replace("anime", "watch")
            val animeEpUrl = "https://yugen.to$watchLink$animeEpCode"
            println(animeEpUrl)
            var yugenEmbedLink =
                Utils().getJsoup(animeEpUrl).getElementById("main-embed")!!.attr("src")
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
            if (bytes != null) {
                val linkDetails = JsonParser.parseString(String(bytes)).asJsonObject
                val link = linkDetails.get("hls")
                return@withContext Pair(link.asString, null)
            }

            return@withContext Pair("", "")

        }
}