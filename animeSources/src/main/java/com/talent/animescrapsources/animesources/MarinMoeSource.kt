package com.talent.animescrapsources.animesources

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.talent.animescrap_common.sourceutils.DdosGuardInterceptor
import com.talent.animescrap_common.model.AnimeDetails
import com.talent.animescrap_common.model.AnimeStreamLink
import com.talent.animescrap_common.model.SimpleAnime
import com.talent.animescrap_common.utils.Utils.httpClient
import com.talent.animescrapsources.animesources.AnimeSource
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup

class MarinMoeSource : AnimeSource {
    private val mainUrl = "https://marin.moe"


    init {
        httpClient = httpClient.newBuilder().addInterceptor(DdosGuardInterceptor(httpClient))
            .build()
    }

    override suspend fun searchAnime(searchedText: String): ArrayList<SimpleAnime> {
        return getAnimeList("$mainUrl/anime?sort=rel-d&search=$searchedText")
    }

    override suspend fun latestAnime(): ArrayList<SimpleAnime> {
        return getEpisodeList("$mainUrl/episode?sort=rel-d&page=1")
    }

    override suspend fun trendingAnime(): ArrayList<SimpleAnime> {
        return getAnimeList("$mainUrl/anime?sort=vwk-d&page=1")
    }

    private fun getAnimeList(url: String): ArrayList<SimpleAnime> {
        val res = get(url)
        val resJson = parseJson(res)
        val animeList = arrayListOf<SimpleAnime>()
        resJson.asJsonObject["props"].asJsonObject["anime_list"].asJsonObject["data"].asJsonArray
            .forEach { e ->
                val anime = e.asJsonObject
                animeList.add(
                    SimpleAnime(
                        anime["title"].asString,
                        anime["cover"].asString,
                        "/anime/${anime["slug"].asString}"
                    )
                )

            }
        return animeList

    }

    private fun getEpisodeList(url: String): ArrayList<SimpleAnime> {
        val res = get(url)
        val resJson = parseJson(res)
        val animeList = arrayListOf<SimpleAnime>()
        resJson.asJsonObject["props"].asJsonObject["episode_list"].asJsonObject["data"].asJsonArray
            .forEach { e ->
                val anime = e.asJsonObject["anime"].asJsonObject
                animeList.add(
                    SimpleAnime(
                        anime["title"].asString,
                        e.asJsonObject["cover"].asString,
                        "/anime/${anime["slug"].asString}"
                    )
                )

            }
        return animeList

    }

    override suspend fun animeDetails(contentLink: String): AnimeDetails {
        val res = get("$mainUrl$contentLink?sort=srt-d")
        val resJson = parseJson(res)

        val animeObj = resJson.asJsonObject["props"].asJsonObject["anime"].asJsonObject

        val name = animeObj["title"].asString
        val cover = animeObj["cover"].asString
        val desc = Jsoup.parseBodyFragment(animeObj["description"].asString).text()

        val totalEps =
            resJson.asJsonObject["props"].asJsonObject["episode_list"].asJsonObject["meta"].asJsonObject["total"].asInt
        val subEpMap = (1..totalEps).associate { it.toString() to it.toString() }

        return AnimeDetails(
            name,
            desc,
            cover,
            mapOf("SUB" to subEpMap)
        )
    }

    override suspend fun streamLink(
        animeUrl: String,
        animeEpCode: String,
        extras: List<String>?
    ): AnimeStreamLink {
        val epUrl = "$mainUrl$animeUrl/$animeEpCode"
        println(epUrl)
        val res = getResponse(epUrl)
        val resJson = parseJson(res.body!!.string())
        val mirrors =
            resJson.asJsonObject["props"].asJsonObject["video"].asJsonObject["data"].asJsonObject["mirror"].asJsonArray
        val firstMirror = mirrors.first().asJsonObject["code"]
        val videoLink = firstMirror.asJsonObject["file"].asString
        val videoSubs = firstMirror.asJsonObject["vtt"].asString

        println(firstMirror)
        println(videoLink)
        println(videoSubs)

        val ddosCookies = httpClient.cookieJar.loadForRequest(res.request.url).filter {
            it.name !in arrayOf("__ddgid_", "__ddgmark_")
        }.joinToString(";") { "${it.name}=${it.value}" }

        val videoHeaders = hashMapOf(
            "X-XSRF-TOKEN" to
                    ddosCookies.substringAfter("XSRF-TOKEN=").substringBefore(";")
                        .replace("%3D", "="),
        )
        videoHeaders.apply {
            put("Cookie", ddosCookies)
            put(
                "Accept",
                "video/webm,video/ogg,video/*;q=0.9,application/ogg;q=0.7,audio/*;q=0.6,*/*;q=0.5"
            )
            put("Referer", res.request.url.toString())
            put("Accept-Language", "en-US,en;q=0.5")
        }
        return AnimeStreamLink(videoLink, "", false, videoHeaders)
    }

    private fun parseJson(res: String): JsonElement {
        val dataPage = Jsoup.parse(res).select("div#app").attr("data-page").replace("&quot;", "\"")
        return JsonParser.parseString(dataPage)

    }

    private fun get(url: String): String {
        val requestBuilder = Request.Builder().url(url)
        return httpClient.newCall(requestBuilder.build())
            .execute().body!!.string()
    }

    private fun getResponse(url: String): Response {
        val requestBuilder = Request.Builder().url(url)
        return httpClient.newCall(requestBuilder.build())
            .execute()
    }
}