package com.talent.animescrap.animesources

import com.talent.animescrap.model.AnimeDetails
import com.talent.animescrap.model.AnimeStreamLink
import com.talent.animescrap.model.SimpleAnime
import com.talent.animescrap.utils.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

class AllAnimeSource : AnimeSource {

    private val mainUrl = "https://allanime.site"

    override suspend fun animeDetails(contentLink: String): AnimeDetails =
        withContext(Dispatchers.IO) {
            val url =
                "$mainUrl/graphql?variables=%7B%22_id%22%3A%22${contentLink}%22%7D&extensions=%7B%22persistedQuery%22%3A%7B%22version%22%3A1%2C%22sha256Hash%22%3A%22afcdaedfd46f36448916b5f7db84d2bdbb72fded428ad8755179a03845c57b96%22%7D%7D"
            println(url)
            val res = Utils().getJson(url)!!

            val data = res.asJsonObject["data"].asJsonObject["show"].asJsonObject
            val animeCover = allAnimeImage(data["thumbnail"].asString)
            println(animeCover)
            val animeName = data["name"].asString
            val animDesc =
                if (!data["description"].isJsonNull) Jsoup.parseBodyFragment(data["description"].asString)
                    .text() else "No Description"

            val num =
                data["lastEpisodeInfo"].asJsonObject["sub"].asJsonObject["episodeString"].asString
            val animeEpContent = (1..num.toInt()).associate { it.toString() to it.toString() }

            return@withContext AnimeDetails(animeName, animDesc, animeCover, animeEpContent)
        }


    override suspend fun searchAnime(searchedText: String) = withContext(Dispatchers.IO) {
        val animeList = arrayListOf<SimpleAnime>()

        val url =
            """$mainUrl/graphql?variables=%7B%22search%22%3A%7B%22allowAdult%22%3Afalse%2C%22allowUnknown%22%3Afalse%2C%22query%22%3A%22${
                searchedText.replace(
                    "+",
                    "%20"
                )
            }%22%7D%2C%22limit%22%3A26%2C%22page%22%3A1%2C%22translationType%22%3A%22sub%22%2C%22countryOrigin%22%3A%22ALL%22%7D&extensions=%7B%22persistedQuery%22%3A%7B%22version%22%3A1%2C%22sha256Hash%22%3A%22d2670e3e27ee109630991152c8484fce5ff5e280c523378001f9a23dc1839068%22%7D%7D"""
        val res =
            Utils().getJson(url)!!["data"].asJsonObject["shows"].asJsonObject["edges"].asJsonArray
        for (json in res) {
            val name = json.asJsonObject["name"].asString
            val image = allAnimeImage(json.asJsonObject["thumbnail"].asString)
            val id = json.asJsonObject["_id"].asString
            animeList.add(SimpleAnime(name, image, id))
        }
        return@withContext animeList
    }

    override suspend fun latestAnime(): ArrayList<SimpleAnime> =
        withContext(Dispatchers.IO) {
            val animeList = arrayListOf<SimpleAnime>()
            val url =
                """$mainUrl/graphql?variables=%7B%22search%22%3A%7B%22sortBy%22%3A%22Recent%22%2C%22allowAdult%22%3Afalse%2C%22allowUnknown%22%3Afalse%7D%2C%22limit%22%3A26%2C%22page%22%3A1%2C%22translationType%22%3A%22sub%22%7D&extensions=%7B%22persistedQuery%22%3A%7B%22version%22%3A1%2C%22sha256Hash%22%3A%229c7a8bc1e095a34f2972699e8105f7aaf9082c6e1ccd56eab99c2f1a971152c6%22%7D%7D"""
            val res =
                Utils().getJson(url)!!["data"].asJsonObject["shows"].asJsonObject["edges"].asJsonArray
            for (json in res) {
                val name = json.asJsonObject["name"].asString
                val image = allAnimeImage(json.asJsonObject["thumbnail"].asString)
                val id = json.asJsonObject["_id"].asString
                animeList.add(SimpleAnime(name, image, id))
            }
            return@withContext animeList
        }

    override suspend fun trendingAnime(): ArrayList<SimpleAnime> =
        withContext(Dispatchers.IO) {
            val animeList = arrayListOf<SimpleAnime>()
            val url =
                """$mainUrl/graphql?variables=%7B%22type%22%3A%22anime%22%2C%22size%22%3A30%2C%22dateRange%22%3A1%2C%22page%22%3A1%7D&extensions=%7B%22persistedQuery%22%3A%7B%22version%22%3A1%2C%22sha256Hash%22%3A%226f6fe5663e3e9ea60bdfa693f878499badab83e7f18b56acdba5f8e8662002aa%22%7D%7D"""
            val res =
                Utils().getJson(url)!!["data"].asJsonObject["queryPopular"].asJsonObject["recommendations"].asJsonArray
            for (json in res) {
                val animeCard = json.asJsonObject["anyCard"].asJsonObject
                val name = animeCard["name"].asString
                val image = allAnimeImage(animeCard["thumbnail"].asString)
                val id = animeCard["_id"].asString
                animeList.add(SimpleAnime(name, image, id))
            }
            return@withContext animeList
        }

    override suspend fun streamLink(animeUrl: String, animeEpCode: String): AnimeStreamLink =
        withContext(Dispatchers.IO) {

            println(animeUrl)
            println(animeEpCode)

            val url =
                """$mainUrl/graphql?variables=%7B%22showId%22%3A%22$animeUrl%22%2C%22translationType%22%3A%22sub%22%2C%22episodeString%22%3A%22$animeEpCode%22%7D&extensions=%7B%22persistedQuery%22%3A%7B%22version%22%3A1%2C%22sha256Hash%22%3A%2229f49ce1a69320b2ab11a475fd114e5c07b03a7dc683f77dd502ca42b26df232%22%7D%7D"""
            val res =
                Utils().getJson(url)!!["data"].asJsonObject["episode"].asJsonObject["sourceUrls"].asJsonArray

            val sortedSources =
                res.sortedBy { if (!it.asJsonObject["priority"].isJsonNull) it.asJsonObject["priority"].asDouble else 0.0 }
                    .reversed()
            println(sortedSources)
            println("sorted")
            for (sourceUrlHolder in sortedSources) {
                println(sourceUrlHolder)
                val sourceUrl = sourceUrlHolder.asJsonObject["sourceUrl"].asString
                if (isThese(sourceUrl)) continue
                if (sourceUrl.contains("apivtwo")) {
                    val apiUrl =
                        Utils().getJson("$mainUrl/getVersion")!!.asJsonObject["episodeIframeHead"].asString

                    val resSource = Utils().getJson(
                        "$apiUrl/${
                            sourceUrl.replace("clock", "clock.json")
                        }"
                    )!!.asJsonObject["links"].asJsonArray
                    println(resSource)
                    println()
                    val firstLink = resSource.first().asJsonObject
                    val isHls = firstLink.has("hls") && firstLink["hls"].asBoolean

                    return@withContext AnimeStreamLink(firstLink["link"].asString, "", isHls)
                }

                return@withContext AnimeStreamLink(
                    sourceUrl,
                    "",
                    res.first().toString().contains("hls")
                )
            }

            return@withContext AnimeStreamLink("", "", false)
        }

    private fun isThese(url: String): Boolean {
        val unwantedSources = listOf("goload", "streamsb", "ok.ru", "streamlare", "mp4upload")
        unwantedSources.forEach { source ->
            if (url.contains(source)) return true
        }
        return false
    }

    private fun allAnimeImage(imageUrl: String) =
        if (imageUrl.contains("kickassanime")) "https://wp.youtube-anime.com/${
            imageUrl.replace(
                "https://",
                ""
            )
        }?w=250"
        else if (imageUrl.contains("_Show_")) "https://wp.youtube-anime.com/aln.youtube-anime.com/images/${
            imageUrl.replaceAfterLast(
                ".",
                "css"
            )
        }"
        else imageUrl
}