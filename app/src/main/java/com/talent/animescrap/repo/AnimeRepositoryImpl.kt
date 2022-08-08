package com.talent.animescrap.repo

import android.util.Log
import com.github.kittinunf.fuel.Fuel
import com.google.gson.JsonParser
import com.talent.animescrap.R
import com.talent.animescrap.model.AnimeDetails
import com.talent.animescrap.model.SimpleAnime
import com.talent.animescrap.room.LinkDao
import com.talent.animescrap.utils.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

interface AnimeRepository {
    suspend fun getAnimeDetailsFromSite(contentLink: String): AnimeDetails
    suspend fun getFavoritesFromRoom(): Flow<List<SimpleAnime>>
    suspend fun searchAnimeFromSite(searchUrl: String): ArrayList<SimpleAnime>
    suspend fun getLatestAnimeFromSite(): ArrayList<SimpleAnime>
    suspend fun getTrendingAnimeFromSite(): ArrayList<SimpleAnime>
    suspend fun getStreamLink(animeEpUrl: String): String
}


class AnimeRepositoryImpl @Inject constructor(
    private val linkDao: LinkDao
) : AnimeRepository {
    override suspend fun getAnimeDetailsFromSite(contentLink: String) =
        withContext(Dispatchers.IO) {
            val url = "https://yugen.to${contentLink}watch/?sort=episode"
            val doc = Utils().getJsoup(url)
            val animeContent = doc.getElementsByClass("p-10-t")
            val animeEpContent = doc.getElementsByClass("box p-10 p-15 m-15-b anime-metadetails")
                .select("div:nth-child(6)").select("span").text()
            val animeCover =
                doc.getElementsByClass("page-cover-inner").first()!!.getElementsByTag("img")
                    .attr("data-src")
            val animeName = animeContent.first()!!.text()
            val animDesc = animeContent[1].text()

            val animeModel =
                AnimeDetails(animeName, animDesc, animeCover, animeEpContent)

            Log.i("AnimeDetailsViewModel", animeModel.toString())
            return@withContext animeModel
        }


    override suspend fun getFavoritesFromRoom() = withContext(Dispatchers.IO) {
        return@withContext linkDao.getLinks().map {
            it.map { SimpleAnime(it.nameString, it.picLinkString, it.linkString) }
        }
    }

    override suspend fun searchAnimeFromSite(searchUrl: String) = withContext(Dispatchers.IO) {
        Log.i("SearchViewModel", "Getting to search anime")
        val animeList = arrayListOf<SimpleAnime>()
        val doc = Utils().getJsoup(searchUrl)
        val allInfo = doc.getElementsByClass("anime-meta")
        for (item in allInfo) {
            val itemImage = item.getElementsByTag("img").attr("data-src")
            val itemName = item.getElementsByClass("anime-name").text()
            val itemLink = item.attr("href")
            val picObject = SimpleAnime(itemName, itemImage, itemLink)
            animeList.add(picObject)
        }

        return@withContext animeList
    }

    override suspend fun getLatestAnimeFromSite(): ArrayList<SimpleAnime> =
        withContext(Dispatchers.IO) {
            Log.i(TAG, "Getting the latest anime")
            val picInfo = arrayListOf<SimpleAnime>()
            val url = "https://yugen.to/latest/"

            val doc = Utils().getJsoup(url)
            val allInfo = doc.getElementsByClass("ep-card")
            for (item in allInfo) {
                val itemImage = item.getElementsByTag("img").attr("data-src")
                val itemName = item.getElementsByClass("ep-origin-name").text()
                val itemLink = item.getElementsByClass("ep-details").attr("href")
                val picObject = SimpleAnime(itemName, itemImage, itemLink)
                picInfo.add(picObject)
            }
            return@withContext picInfo
        }

    override suspend fun getTrendingAnimeFromSite(): ArrayList<SimpleAnime> =
        withContext(Dispatchers.IO) {
            Log.i("TrendingViewModel", "Getting the trending anime")
            val animeList = arrayListOf<SimpleAnime>()
            val doc = Utils().getJsoup(url = "https://yugen.to/trending/")
            val allInfo = doc.getElementsByClass("anime-meta")
            for (item in allInfo) {
                val itemImage = item.getElementsByTag("img").attr("data-src")
                val itemName = item.getElementsByClass("anime-name").attr("title")
                val itemLink = item.attr("href")
                animeList.add(SimpleAnime(itemName, itemImage, itemLink))
            }

            return@withContext animeList
        }

    override suspend fun getStreamLink(animeEpUrl: String): String = withContext(Dispatchers.IO) {

        var yugenEmbedLink = Utils().getJsoup(animeEpUrl).getElementById("main-embed")!!.attr("src")
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
        println("hi new repo")
        if (bytes != null) {
            val linkDetails = JsonParser.parseString(String(bytes)).asJsonObject
            val link = linkDetails.get("hls")
            return@withContext link.asString
        }

        return@withContext "No Link Found"

    }

    companion object {
        const val TAG = R.string.app_name.toString()
    }
}