package com.talent.animescrapsources.animesources

import com.talent.animescrap_common.model.AnimeDetails
import com.talent.animescrap_common.model.AnimeStreamLink
import com.talent.animescrap_common.model.SimpleAnime
import com.talent.animescrap_common.source.AnimeSource
import com.talent.animescrap_common.utils.Utils.getJson
import com.talent.animescrap_common.utils.Utils.getJsoup
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
            val animDesc =
                if (data["description"].isJsonNull) "No Description" else Jsoup.parseBodyFragment(
                    data["description"].asString
                ).text()

            val eps = data["episodes"].asJsonArray
            val epMapMixed = mutableMapOf<String, String>()
            for (ep in eps) {
                if (!ep.asJsonObject["sources"].isJsonArray) continue
                epMapMixed[ep.asJsonObject["number"].asString] =
                    ep.asJsonObject["sources"].asJsonArray.first().asJsonObject["id"].asString
            }
            val epMapZoro = mutableMapOf<String, String>()
            val epMapGogo = mutableMapOf<String, String>()

            for (ep in eps) {
                if (!ep.asJsonObject["sources"].isJsonArray) continue
                if (ep.asJsonObject["sources"].asJsonArray.first().asJsonObject["target"].asString.contains(
                        "/watch/"
                    )
                ) {
                    epMapZoro[ep.asJsonObject["number"].asString] =
                        ep.asJsonObject["sources"].asJsonArray.first().asJsonObject["id"].asString
                } else {
                    epMapGogo[ep.asJsonObject["number"].asString] =
                        ep.asJsonObject["sources"].asJsonArray.first().asJsonObject["id"].asString
                }
                if (ep.asJsonObject["sources"].asJsonArray.size() > 1) {
                    if (ep.asJsonObject["sources"].asJsonArray[1].asJsonObject["target"].asString.contains(
                            "/watch/"
                        )
                    ) {
                        epMapZoro[ep.asJsonObject["number"].asString] =
                            ep.asJsonObject["sources"].asJsonArray[1].asJsonObject["id"].asString
                    } else {
                        epMapGogo[ep.asJsonObject["number"].asString] =
                            ep.asJsonObject["sources"].asJsonArray[1].asJsonObject["id"].asString
                    }
                }
            }

            return@withContext AnimeDetails(
                animeName, animDesc, animeCover,
                mapOf("ANY" to epMapMixed, "ZORO" to epMapZoro, "GOGO" to epMapGogo)
            )
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

    override suspend fun streamLink(
        animeUrl: String,
        animeEpCode: String,
        extras: List<String>?
    ): AnimeStreamLink =
        withContext(Dispatchers.IO) {
            println(extras)
            val url = "$mainUrl/source/$animeEpCode"
            println(url)
            val res = getJson(url)!!.asJsonObject
            println(res)

            val subs = if (res["subtitle"] != null) res["subtitle"].asString else ""
            val streamLink =
                if (subs == "") getJsoup("https://cdn.nade.me/generate?url=${res["url"].asString}").text()
                else res["url"].asString
            return@withContext AnimeStreamLink(
                streamLink,
                subs,
                true,
                if (res["referer"] != null) hashMapOf("referer" to res["referer"].asString) else null
            )
        }


}