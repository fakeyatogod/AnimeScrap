package com.talent.animescrapsources.animesources

import com.talent.animescrap_common.model.AnimeDetails
import com.talent.animescrap_common.model.AnimeStreamLink
import com.talent.animescrap_common.model.SimpleAnime
import com.talent.animescrap_common.source.AnimeSource
import com.talent.animescrap_common.utils.Utils.get
import com.talent.animescrap_common.utils.Utils.getJson
import com.talent.animescrap_common.utils.Utils.getJsoup
import com.talent.animescrap_common.utils.Utils.postJson
import org.jsoup.Jsoup

class AniWaveSource : AnimeSource {
    private val mainUrl = "https://aniwave.to"
    private val url = "https://vidplay.site/"
    private val apiUrl = "https://9anime.eltik.net/"

    override suspend fun animeDetails(contentLink: String): AnimeDetails {
        val doc = getJsoup("$mainUrl$contentLink")
        val cover = doc.select("#w-info").first()!!.getElementsByTag("img").attr("src")
        val desc = doc.select("#w-info .info .content").text()
        val title = doc.select("#w-info .info .title").attr("data-jp")

        val dataId = doc.getElementsByAttribute("data-id").first()!!.attr("data-id")
        val vrf = getVrf(dataId)
        val eps =
            Jsoup.parseBodyFragment(getJson("$mainUrl/ajax/episode/list/$dataId?vrf=$vrf")!!.asJsonObject["result"].asString)
                .select("li a")
        val subMap = mutableMapOf<String, String>()
        val dubMap = mutableMapOf<String, String>()
        eps.forEach {
            val epNum = it.attr("data-num")
            val epIds = it.attr("data-ids")
            val isSub = it.attr("data-sub").toInt() == 1
            val isDub = it.attr("data-dub").toInt() == 1
            if (isSub) subMap[epNum] = epIds
            if (isDub) dubMap[epNum] = epIds
        }

        return AnimeDetails(title, desc, cover, mapOf("Sub" to subMap, "Dub" to dubMap))
    }

    private fun getVrf(dataId: String): String {
        val json = getJson("$apiUrl/vrf?query=${dataId}&apikey=chayce")
        return json!!.asJsonObject["url"].asString
    }
    private fun decodeVrf(dataId: String): String {
        val json = getJson("$apiUrl/decrypt?query=${dataId}&apikey=chayce")
        return json!!.asJsonObject["url"].asString
    }


    override suspend fun searchAnime(searchedText: String): ArrayList<SimpleAnime> {

        val animeList = arrayListOf<SimpleAnime>()
        val doc = getJsoup("$mainUrl/filter?keyword=$searchedText")
        doc.select("#list-items .item").forEach { item ->
            animeList.add(
                SimpleAnime(
                    item.select(".info a").attr("data-jp"),
                    item.getElementsByTag("img").attr("src"),
                    item.getElementsByTag("a").attr("href")
                )
            )
        }
        return animeList
    }

    override suspend fun latestAnime(): ArrayList<SimpleAnime> {
        val animeList = arrayListOf<SimpleAnime>()
        val url = "$mainUrl/ajax/home/widget/updated-sub?page=1"
        val latestHtml = getJson(url)!!.asJsonObject["result"].asString
        val doc = Jsoup.parseBodyFragment(latestHtml)
        doc.getElementsByClass("item").forEach { item ->
            animeList.add(
                SimpleAnime(
                    item.select(".info a").attr("data-jp"),
                    item.getElementsByTag("img").attr("src"),
                    item.getElementsByTag("a").attr("href")
                )
            )
        }
        return animeList
    }

    override suspend fun trendingAnime(): ArrayList<SimpleAnime> {
        val animeList = arrayListOf<SimpleAnime>()
        val url = "$mainUrl/home/"
        val doc = getJsoup(url)
        doc.select("#top-anime .tab-content").first()!!.select(".item").forEach { item ->
            animeList.add(
                SimpleAnime(
                    item.select(".name").attr("data-jp"),
                    item.getElementsByTag("img").attr("src"),
                    item.attr("href")
                )
            )
        }
        return animeList
    }

    override suspend fun streamLink(
        animeUrl: String,
        animeEpCode: String,
        extras: List<String>?
    ): AnimeStreamLink {
//        0=sub , 1=softsub, 3=dub
        val dataId = animeEpCode.split(",").first()
        val vrf = getVrf(dataId)
        val servers =
            Jsoup.parseBodyFragment(getJson("$mainUrl/ajax/server/list/$dataId?vrf=$vrf")!!.asJsonObject["result"].asString)
        val dataLinkId = servers.select(".servers .type ul li")[0]!!.attr("data-link-id")
        val vrf2 = getVrf(dataLinkId)
        val linkEncoded = getJson("$mainUrl/ajax/server/$dataLinkId?vrf=$vrf2")!!.asJsonObject["result"].asJsonObject["url"].asString
        val embedLink = decodeVrf(linkEncoded)
//        println(embedLink)
        val fileURL = getFileUrl(embedLink)
//        println(fileURL)
        val link = getJson(fileURL, mapOf("referer" to embedLink))!!.asJsonObject["result"].asJsonObject["sources"].asJsonArray.first().asJsonObject["file"].asString
//        println(link)
        return AnimeStreamLink(link, "", true)
    }

    private fun getFuToken(referer: String): String {
        val response = get("$url/futoken", mapOf("referer" to referer))
        return response.replace(Regex("""/\*[\s\S]*?\*/|//.*"""), "").replace("\n", "")
    }

    private fun getFileUrl(sourceUrl: String): String {
        val fuToken = getFuToken(sourceUrl)
        val id = sourceUrl.split("/e/")[1].split('?')[0]

        val response = postJson(url = "$apiUrl/rawVizcloud?query=$id&apikey=lagrapps",
            payload = mapOf("query" to id,"futoken" to fuToken)
        )
        val rawURL = response!!.asJsonObject["rawURL"].asString
        return "$rawURL?${sourceUrl.split('?')[1]}"
    }
}