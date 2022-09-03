package com.talent.animescrap.animesources

import com.talent.animescrap.model.AnimeDetails
import com.talent.animescrap.model.AnimeStreamLink
import com.talent.animescrap.model.SimpleAnime
import com.talent.animescrap.utils.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FakeGogoSource : AnimeSource {
    private val mainUrl = "https://gogoanime.nl"
    override suspend fun animeDetails(contentLink: String): AnimeDetails =
        withContext(Dispatchers.IO) {
            val url = "$mainUrl${contentLink}/ep-1"
            println(url)
            val doc = Utils().getJsoup(url)
            println(doc)
            val animeContent = doc.getElementById("w-info")!!
            val animeCover = animeContent.getElementsByTag("img").attr("src")
            val animeName = animeContent.getElementsByClass("title").text()
            val animDesc = animeContent.getElementsByClass("synopsis").text()

            val num = doc.getElementsByClass("dropdown-item").last()!!.text().substringAfter("-")
            println(num)
            val animeEpContent = (1..num.toInt()).associate { it.toString() to it.toString() }

            return@withContext AnimeDetails(animeName, animDesc, animeCover, animeEpContent)
        }


    override suspend fun searchAnime(searchedText: String) = withContext(Dispatchers.IO) {
        val animeList = arrayListOf<SimpleAnime>()

        val searchUrl = "$mainUrl/filter?keyword=${searchedText}"

        val doc = Utils().getJsoup(searchUrl)
        val allInfo = doc.getElementsByClass("item")
        for (item in allInfo) {
            val itemImage = item.getElementsByTag("img").attr("src")
            val itemName = item.getElementsByClass("name").text()
            val itemLink = item.getElementsByClass("name").attr("href")
            animeList.add(SimpleAnime(itemName, itemImage, itemLink))
        }
        return@withContext animeList
    }

    override suspend fun latestAnime(): ArrayList<SimpleAnime> =
        withContext(Dispatchers.IO) {
            val animeList = arrayListOf<SimpleAnime>()
            val doc = Utils().getJsoup(url = "$mainUrl/updated")
            val allInfo = doc.getElementsByClass("item")
            for (item in allInfo) {
                val itemImage = item.getElementsByTag("img").attr("src")
                val itemName = item.getElementsByClass("name").text()
                val itemLink = item.getElementsByClass("name").attr("href")
                animeList.add(SimpleAnime(itemName, itemImage, itemLink))
            }
            return@withContext animeList
        }

    override suspend fun trendingAnime(): ArrayList<SimpleAnime> =
        withContext(Dispatchers.IO) {
            val animeList = arrayListOf<SimpleAnime>()
            val doc = Utils().getJsoup(url = "$mainUrl/filter?keyword=&sort=trending&vrf=")
            val allInfo = doc.getElementsByClass("item")
            for (item in allInfo) {
                val itemImage = item.getElementsByTag("img").attr("src")
                val itemName = item.getElementsByClass("name").text()
                val itemLink = item.getElementsByClass("name").attr("href")
                animeList.add(SimpleAnime(itemName, itemImage, itemLink))
            }
            return@withContext animeList
        }

    override suspend fun streamLink(animeUrl: String, animeEpCode: String): AnimeStreamLink =
        withContext(Dispatchers.IO) {
            return@withContext AnimeStreamLink("", "",true)

        }
}