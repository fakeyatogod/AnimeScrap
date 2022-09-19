package com.talent.animescrap.animesources

import com.talent.animescrap.model.AnimeDetails
import com.talent.animescrap.model.AnimeStreamLink
import com.talent.animescrap.model.SimpleAnime
import com.talent.animescrap.utils.Utils.getJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

class EnimeSource : AnimeSource {

    private val mainUrl = "https://api.enime.moe"

    override suspend fun animeDetails(contentLink: String): AnimeDetails =
        withContext(Dispatchers.IO) {
            val url = "$mainUrl/anime/$contentLink"
            val res = getJson(url)!!
            val data = res.asJsonObject

            val animeCover = data["coverImage"].asString
            val animeName = data["title"].asJsonObject["romaji"].asString
            val animDesc = Jsoup.parseBodyFragment(data["description"].asString).text()

            val eps = data["episodes"].asJsonArray
            val epMap = mutableMapOf<String, String>()
            eps.forEach { ep ->
                epMap[ep.asJsonObject["number"].asString] =
                    ep.asJsonObject["sources"].asJsonArray.first().asJsonObject["id"].asString
            }

            return@withContext AnimeDetails(animeName, animDesc, animeCover, epMap)
        }


    override suspend fun searchAnime(searchedText: String) = withContext(Dispatchers.IO) {
        val animeList = arrayListOf<SimpleAnime>()
        val url = "$mainUrl/search/$searchedText"
        val res = getJson(url)!!.asJsonObject["data"].asJsonArray
        for (anime in res) {
            val image = anime.asJsonObject["coverImage"].asString
            val name = anime.asJsonObject["title"].asJsonObject["romaji"].asString
            val id = anime.asJsonObject["id"].asString
            animeList.add(SimpleAnime(name, image, id))
        }
        return@withContext animeList
    }

    override suspend fun latestAnime(): ArrayList<SimpleAnime> =
        withContext(Dispatchers.IO) {
            val animeList = arrayListOf<SimpleAnime>()
            val url = "$mainUrl/recent"
            val res = getJson(url)!!.asJsonObject["data"].asJsonArray
            for (animeRecent in res) {
                val anime = animeRecent.asJsonObject["anime"]
                val image = anime.asJsonObject["coverImage"].asString
                val name = anime.asJsonObject["title"].asJsonObject["romaji"].asString
                val id = anime.asJsonObject["id"].asString
                animeList.add(SimpleAnime(name, image, id))
            }
            return@withContext animeList
        }

    override suspend fun trendingAnime(): ArrayList<SimpleAnime> =
        withContext(Dispatchers.IO) {
            val animeList = arrayListOf<SimpleAnime>()
            val url = "$mainUrl/popular"
            val res = getJson(url)!!.asJsonObject["data"].asJsonArray
            for (anime in res) {
                val image = anime.asJsonObject["coverImage"].asString
                val name = anime.asJsonObject["title"].asJsonObject["romaji"].asString
                val id = anime.asJsonObject["id"].asString
                animeList.add(SimpleAnime(name, image, id))
            }
            return@withContext animeList
        }

    override suspend fun streamLink(animeUrl: String, animeEpCode: String): AnimeStreamLink =
        withContext(Dispatchers.IO) {
            val url = "$mainUrl/source/$animeEpCode"
            val res = getJson(url)!!.asJsonObject
            val streamLink = res["url"].asString
            val subs = if (res["subtitle"] != null) res["subtitle"].asString else ""
            return@withContext AnimeStreamLink(streamLink, subs, true, null)
        }


}