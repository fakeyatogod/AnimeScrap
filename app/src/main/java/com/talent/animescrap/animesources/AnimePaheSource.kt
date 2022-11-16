package com.talent.animescrap.animesources

import com.talent.animescrap.model.AnimeDetails
import com.talent.animescrap.model.AnimeStreamLink
import com.talent.animescrap.model.SimpleAnime
import com.talent.animescrap.utils.Utils.getJson
import com.talent.animescrap.utils.Utils.getJsoup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AnimePaheSource : AnimeSource {
    private val mainUrl = "https://animepahe.com"
    override suspend fun animeDetails(contentLink: String): AnimeDetails =
        withContext(Dispatchers.IO) {
            /*  val id =
                  contentLink.replaceAfter("AnimePaheSession", "").replace("AnimePaheSession", "")
                      .replace("AnimePaheId=", "")*/
            val session =
                contentLink.replaceBefore("AnimePaheSession", "").replace("AnimePaheSession=", "")

            // For Anime Details
            val detailsUrl = "$mainUrl/anime/$session"
            val doc = getJsoup(detailsUrl)
            val name = doc.selectFirst("div.title-wrapper > h1 > span")!!.text()
            val cover = doc.selectFirst("div.anime-poster a")!!.attr("href")
            val synonyms = doc.select("div.col-sm-4.anime-info p:contains(Synonyms:)")
                .firstOrNull()?.text()
            val description = doc.select("div.anime-summary").text() +
                    if (synonyms.isNullOrEmpty()) "" else "\n\n$synonyms"

            // For EP List
            var currentPage = 1
            val epMap = mutableMapOf<String, String>()
            do {
                val episodeEndpoint =
                    "$mainUrl/api?m=release&id=$session&sort=episode_desc&page=$currentPage"
                val res = getJson(episodeEndpoint)!!.asJsonObject
                val episodes = res["data"].asJsonArray
                for (ep in episodes) {
                    val epSession = ep.asJsonObject["session"].asString
//                    val epId = ep.asJsonObject["id"].asString
                    val epNumber = ep.asJsonObject["episode"].asString
                    epMap[epNumber] = epSession
                }
                val lastPage = res["last_page"].asInt
                currentPage++
            } while (currentPage < lastPage)

            return@withContext AnimeDetails(
                name,
                description,
                cover,
                mapOf("Default" to epMap)
            )
        }

    override suspend fun searchAnime(searchedText: String): ArrayList<SimpleAnime> =
        withContext(Dispatchers.IO) {
            val animeList = arrayListOf<SimpleAnime>()
            val url = "$mainUrl/api?m=search&q=${searchedText.replace("+", "%20")}"
            val res =
                getJson(url)!!.asJsonObject["data"].asJsonArray
            for (json in res) {
                val animeCard = json.asJsonObject
                val name = animeCard["title"].asString
                val image = animeCard["poster"].asString
                val id = animeCard["id"].asString
                val session = animeCard["session"].asString
                animeList.add(
                    SimpleAnime(
                        name,
                        image,
                        "AnimePaheId=${id}AnimePaheSession=${session}"
                    )
                )
            }
            return@withContext animeList
        }

    override suspend fun latestAnime(): ArrayList<SimpleAnime> =
        withContext(Dispatchers.IO) {
            return@withContext getAnimeList("$mainUrl/api?m=airing&page=1")
        }

    override suspend fun trendingAnime(): ArrayList<SimpleAnime> =
        withContext(Dispatchers.IO) {
            return@withContext getAnimeList("$mainUrl/api?m=airing&page=2")
        }

    private fun getAnimeList(url: String): ArrayList<SimpleAnime> {
        val animeList = arrayListOf<SimpleAnime>()
        val res = getJson(url)!!.asJsonObject["data"].asJsonArray
        for (json in res) {
            val animeCard = json.asJsonObject
            val name = animeCard["anime_title"].asString
            val image = animeCard["snapshot"].asString
            val id = animeCard["anime_id"].asString
            val session = animeCard["anime_session"].asString
            animeList.add(SimpleAnime(name, image, "AnimePaheId=${id}AnimePaheSession=${session}"))
        }
        return animeList
    }

    override suspend fun streamLink(
        animeUrl: String,
        animeEpCode: String,
        extras: List<String>?
    ): AnimeStreamLink =
        withContext(Dispatchers.IO) {
            /*val animeId =
                animeUrl.replaceAfter("AnimePaheSession", "").replace("AnimePaheSession", "")
                    .replace("AnimePaheId=", "")
            val animeSession =
                animeUrl.replaceBefore("AnimePaheSession", "").replace("AnimePaheSession=", "")
*/
            val urlForLinks = "https://animepahe.com/api?m=links&id=$animeEpCode&p=kwik"
            val kwikLink =
                getJson(urlForLinks)!!.asJsonObject["data"].asJsonArray.last().asJsonObject["1080"].asJsonObject["kwik"].asString
            println(kwikLink)
            val hlsLink = getHlsStreamUrl(kwikLink)
            println(hlsLink)
            return@withContext AnimeStreamLink(
                hlsLink,
                "",
                true,
                extraHeaders = hashMapOf("referer" to "https://kwik.cx")
            )
        }

    private fun getHlsStreamUrl(kwikUrl: String): String {
        val eContent =
            getJsoup(kwikUrl, mapOfHeaders = mapOf("referer" to mainUrl)).body().toString()
        println(eContent)
        val substring = eContent.substringAfterLast("m3u8|uwu|").substringBefore("'")
        println(substring)
        val urlParts = substring.split("|").reversed()
        assert(urlParts.lastIndex == 8)
        return urlParts[0] + "://" + urlParts[1] + "-" + urlParts[2] + "." + urlParts[3] + "." +
                urlParts[4] + "." + urlParts[5] + "/" + urlParts[6] + "/" + urlParts[7] + "/" +
                urlParts[8] + "/uwu.m3u8"
    }
}