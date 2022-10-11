package com.talent.animescrap.animesources

import com.talent.animescrap.model.AnimeDetails
import com.talent.animescrap.model.AnimeStreamLink
import com.talent.animescrap.model.SimpleAnime
import com.talent.animescrap.utils.Utils.getJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class KissKhSource : AnimeSource {

    private val mainUrl = "https://kisskh.me"

    override suspend fun animeDetails(contentLink: String): AnimeDetails =
        withContext(Dispatchers.IO) {
            val url = "$mainUrl/api/DramaList/Drama/$contentLink?isq=false"
            println(url)
            val res = getJson(url)!!.asJsonObject

            val animeCover = res["thumbnail"].asString
            println(animeCover)
            val animeName = res["title"].asString
            val animDesc = res["description"].asString
            val eps = res["episodes"].asJsonArray
            println(eps)
            val epMap = mutableMapOf<String, String>()
            eps.reversed().forEach { ep ->
                epMap[ep.asJsonObject["number"].asInt.toString()] =
                    ep.asJsonObject["id"].asString

            }
            return@withContext AnimeDetails(animeName, animDesc, animeCover, mapOf("SUB" to epMap))
        }


    override suspend fun searchAnime(searchedText: String) = withContext(Dispatchers.IO) {
        val animeList = arrayListOf<SimpleAnime>()
        val url = "$mainUrl/api/DramaList/Search?q=$searchedText&type=0"
        println(url)
        val res = getJson(url)!!.asJsonArray
        for (json in res) {
            val name = json.asJsonObject["title"].asString
            val image = json.asJsonObject["thumbnail"].asString
            val id = json.asJsonObject["id"].asString
            animeList.add(SimpleAnime(name, image, id))
        }
        return@withContext animeList
    }

    override suspend fun latestAnime(): ArrayList<SimpleAnime> =
        withContext(Dispatchers.IO) {
            val animeList = arrayListOf<SimpleAnime>()
            val url =
                "$mainUrl/api/DramaList/List?page=1&type=0&sub=0&country=0&status=0&order=2&pageSize=40"
            val res = getJson(url)!!.asJsonObject["data"].asJsonArray
            for (json in res) {
                val name = json.asJsonObject["title"].asString
                val image = json.asJsonObject["thumbnail"].asString
                val id = json.asJsonObject["id"].asString
                animeList.add(SimpleAnime(name, image, id))
            }
            return@withContext animeList
        }

    override suspend fun trendingAnime(): ArrayList<SimpleAnime> =
        withContext(Dispatchers.IO) {
            val animeList = arrayListOf<SimpleAnime>()
            val url =
                "$mainUrl/api/DramaList/List?page=1&type=0&sub=0&country=0&status=0&order=1&pageSize=40"
            val res = getJson(url)!!.asJsonObject["data"].asJsonArray
            for (json in res) {
                val name = json.asJsonObject["title"].asString
                val image = json.asJsonObject["thumbnail"].asString
                val id = json.asJsonObject["id"].asString
                animeList.add(SimpleAnime(name, image, id))
            }
            return@withContext animeList
        }

    override suspend fun streamLink(animeUrl: String, animeEpCode: String, extras: List<String>?): AnimeStreamLink =
        withContext(Dispatchers.IO) {

            println(animeUrl)
            println(animeEpCode)

            val url = "$mainUrl/api/DramaList/Episode/$animeEpCode.png?err=false&ts=&time="
            val res = getJson(url)!!.asJsonObject
            println(res)

            var subs = ""
            getJson("$mainUrl/api/Sub/$animeEpCode")?.asJsonArray
                .let {
                    if (it != null && !it.isJsonNull && !it.isEmpty) {
                        val subObj = it.first().asJsonObject
                        subs = if (subObj["default"].asBoolean) subObj["src"].asString else ""
                    }
                }

            return@withContext AnimeStreamLink(res["Video"].asString, subs, true)
        }

}