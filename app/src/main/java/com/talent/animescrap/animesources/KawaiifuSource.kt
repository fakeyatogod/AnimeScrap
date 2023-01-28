package com.talent.animescrap.animesources

import android.content.Context
import com.talent.animescrap.animesources.sourceutils.AndroidCookieJar
import com.talent.animescrap.animesources.sourceutils.CloudflareInterceptor
import com.talent.animescrap.model.AnimeDetails
import com.talent.animescrap.model.AnimeStreamLink
import com.talent.animescrap.model.SimpleAnime
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.util.concurrent.TimeUnit

class KawaiifuSource(context: Context) : AnimeSource {
    private val mainUrl = "https://kawaiifu.com"
    private val streamUrl = "https://domdom.stream"

    private val client = OkHttpClient.Builder()
        .cookieJar(AndroidCookieJar())
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(2, TimeUnit.MINUTES)
        .addInterceptor(CloudflareInterceptor(context))
        .build()

    override suspend fun animeDetails(contentLink: String): AnimeDetails {
        val url = "$streamUrl/anime${
            contentLink.replace("https://kawaiifu.com", "").replaceBefore("/", "")
        }".removeSuffix(".html")
        println(url)
        val res = Jsoup.parse(get(url))

        val title = res.selectFirst(".desc .title")!!.text()
        val desc = res.select(".desc .wrap-desc").text()
        val image = res.selectFirst(".section .thumb img")!!.attr("src")


        val epItems = res.selectFirst(".list-ep")!!.select("a")
//        println(epItems)
        val map = mutableMapOf<String, String>()
        for (epItem in epItems) {
            map[epItem.text().replace("Ep ", "")] = epItem.selectFirst("a")!!.attr("href")
        }
        return AnimeDetails(title, desc, image, mapOf("Default" to map))
    }

    override suspend fun searchAnime(searchedText: String): ArrayList<SimpleAnime> {
        val url = "$mainUrl?s=$searchedText"
        val res = Jsoup.parse(get(url))
        val items = res.select(".today-update .item")
        val animeList = arrayListOf<SimpleAnime>()
        for (item in items) {
            animeList.add(
                SimpleAnime(
                    item.selectFirst("img")!!.attr("alt"),
                    item.selectFirst("img")!!.attr("src"),
                    item.selectFirst("a")!!.attr("href")
                )
            )
        }
        println(animeList.first())
        return animeList
    }

    override suspend fun latestAnime(): ArrayList<SimpleAnime> {
        val res = Jsoup.parse(get())
        val items = res.select(".today-update .item")
        val animeList = arrayListOf<SimpleAnime>()
        for (item in items) {
            animeList.add(
                SimpleAnime(
                    item.selectFirst("img")!!.attr("alt"),
                    item.selectFirst("img")!!.attr("src"),
                    item.selectFirst("a")!!.attr("href")
                )
            )
        }
        println(animeList.first())
        return animeList
    }

    override suspend fun trendingAnime(): ArrayList<SimpleAnime> {
        val res = Jsoup.parse(get())
        val items = res.select(".section .list-film .item")
        val animeList = arrayListOf<SimpleAnime>()
        for (item in items) {
            animeList.add(
                SimpleAnime(
                    item.selectFirst("img")!!.attr("alt"),
                    item.selectFirst("img")!!.attr("src"),
                    item.selectFirst("a")!!.attr("href")
                )
            )
        }
        println(animeList.first())
        return animeList
    }

    private fun get(url: String = mainUrl): String {
        return client.newCall(Request.Builder().url(url).header("referer", mainUrl).build())
            .execute().body!!.string()
    }

    override suspend fun streamLink(
        animeUrl: String,
        animeEpCode: String,
        extras: List<String>?
    ): AnimeStreamLink {
        println(animeUrl)
        println(animeEpCode) // link
        val resDoc = Jsoup.parse(get(animeEpCode))
        val link = resDoc.select(".section video source").attr("src")
        return AnimeStreamLink(link, "", link.contains(".m3u"))
    }


}