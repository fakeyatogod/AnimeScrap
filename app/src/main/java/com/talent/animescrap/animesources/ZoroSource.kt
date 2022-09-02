package com.talent.animescrap.animesources

import com.talent.animescrap.model.AnimeDetails
import com.talent.animescrap.model.SimpleAnime
import com.talent.animescrap.utils.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

class ZoroSource : AnimeSource, Utils() {
    override suspend fun animeDetails(contentLink: String): AnimeDetails =
        withContext(Dispatchers.IO) {

            // Get Ep List
            val animeCode = contentLink.split("-").last()
            val url = "https://zoro.to/ajax/v2/episode/list/${animeCode}"
            val html = getJson(url)!!.get("html")!!.asString
            val eps = Jsoup.parse(html).select(".ss-list > a[href].ssl-item.ep-item")
            val epMap = mutableMapOf<String, String>()
            eps.forEach { ep ->
                epMap[ep.attr("data-number")] = ep.attr("data-id")
            }
            println(epMap)

            val animeUrl = "http://zoro.to/${contentLink}"
            val doc = getJsoup(animeUrl)

            val animeCover = doc.selectFirst(".anisc-poster img")?.attr("src").toString()
            val animeName = doc.selectFirst(".anisc-detail > .film-name")?.text().toString()
            val animDesc = doc.selectFirst(".film-description.m-hide > .text")?.text().toString()

            val details = AnimeDetails(animeName, animDesc, animeCover, epMap)
            println(details)

            return@withContext details
        }


    override suspend fun searchAnime(searchedText: String) = withContext(Dispatchers.IO) {
        val url = "https://zoro.to/search?keyword=${searchedText}"
        val simpleAnimeList = arrayListOf<SimpleAnime>()

        val doc = getJsoup(url)
        val animeList = doc.select("div.flw-item")
        for (item in animeList) {
            val animeImageURL = item.getElementsByTag("img").attr("data-src")
            val animeName = item.getElementsByClass("dynamic-name").text()
            val animeLink = item.getElementsByClass("dynamic-name").attr("href")
            simpleAnimeList.add(SimpleAnime(animeName, animeImageURL, animeLink))
        }
        return@withContext simpleAnimeList
    }

    override suspend fun latestAnime(): ArrayList<SimpleAnime> =
        withContext(Dispatchers.IO) {
            val simpleAnimeList = arrayListOf<SimpleAnime>()
            val url = "https://zoro.to/recently-updated"

            val doc = getJsoup(url)
            val animeList = doc.select("div.flw-item")
            for (item in animeList) {
                val animeImageURL = item.getElementsByTag("img").attr("data-src")
                val animeName = item.getElementsByClass("dynamic-name").text()
                val animeLink = item.getElementsByClass("dynamic-name").attr("href")
                simpleAnimeList.add(SimpleAnime(animeName, animeImageURL, animeLink))
            }
            return@withContext simpleAnimeList
        }

    override suspend fun trendingAnime(): ArrayList<SimpleAnime> =
        withContext(Dispatchers.IO) {
            val simpleAnimeList = arrayListOf<SimpleAnime>()
            val url = "https://zoro.to/top-airing"

            val doc = getJsoup(url)
            val animeList = doc.select("div.flw-item")
            for (item in animeList) {
                val animeImageURL = item.getElementsByTag("img").attr("data-src")
                val animeName = item.getElementsByClass("dynamic-name").text()
                val animeLink = item.getElementsByClass("dynamic-name").attr("href")
                simpleAnimeList.add(SimpleAnime(animeName, animeImageURL, animeLink))
            }
            return@withContext simpleAnimeList
        }

    override suspend fun streamLink(animeUrl: String, animeEpCode: String): Pair<String,String?> =
        withContext(Dispatchers.IO) {
            println(animeUrl)
            println(animeEpCode)
            val url = "https://zoro.to/ajax/v2/episode/servers?episodeId=$animeEpCode"
            val html = getJson(url)!!.get("html")!!.asString
            val items = Jsoup.parse(html).select(".server-item[data-type][data-id]")
            println(items.first()?.text())
            val servers = items.map {
                Pair(
                    if (it.attr("data-type") == "sub") "sub" else "dub",
                    it.attr("data-id")
                )
            }
            println(items)
            println(servers)

            val rapidLinks = arrayListOf<String>()
            servers.distinctBy { it.second }.map {
                val link =
                    "https://zoro.to/ajax/v2/episode/sources?id=${it.second}"
                if (it.first == "sub")
                    rapidLinks.add(getJson(link)!!.get("link")!!.asString)
            }
            val rapidUrl = rapidLinks.firstOrNull { it.contains("rapid") }
                ?: ""
            val jsonLink = "https://rapid-cloud.co/ajax/embed-6/getSources?id=${
                rapidUrl.replaceBeforeLast(
                    "/embed-6/",
                    ""
                ).replace("/embed-6/", "").replace("?z=", "")
            }"
            println(rapidUrl)
            println(jsonLink)
            val json = getJson(
                jsonLink, mapOfHeaders = mapOf(
                    "Referer" to "https://zoro.to/",
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:94.0) Gecko/20100101 Firefox/94.0"
                )
            )!!
            println(json)
            val m3u8 = json["sources"]!!.asJsonArray[0].asJsonObject["file"].toString().trim('"')
            val subtitle = mutableMapOf<String, String>()

            json["tracks"]!!.asJsonArray.forEach {
                if (it.asJsonObject["kind"].toString().trim('"') == "captions")
                    subtitle[it.asJsonObject["label"].toString().trim('"')] =
                        it.asJsonObject["file"].toString().trim('"')
            }

            println(m3u8)
            println(subtitle)

            return@withContext Pair(m3u8, subtitle["English"])

        }
}