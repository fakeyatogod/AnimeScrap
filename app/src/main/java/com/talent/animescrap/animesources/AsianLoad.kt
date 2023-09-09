package com.talent.animescrap.animesources

import android.net.Uri
import android.util.Base64
import com.google.gson.JsonParser
import com.talent.animescrap.model.AnimeDetails
import com.talent.animescrap.model.AnimeStreamLink
import com.talent.animescrap.model.SimpleAnime
import com.talent.animescrap.utils.Utils.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class AsianLoad : AnimeSource {
    private val mainUrl = "https://asianload.cc"
    override suspend fun animeDetails(contentLink: String): AnimeDetails {
        val url = "$mainUrl${contentLink}"
        val doc = Jsoup.parse(get(url))
        val animeCover = doc.selectFirst(".video-block")!!.getElementsByTag("img").attr("src")
        val animeName = doc.selectFirst(".video-details .date")!!.text()
        val animDesc = doc.selectFirst(".video-details .post-entry")!!.text()

        val eps = doc.selectFirst(".listing")!!.select("li")
        val subMap = mutableMapOf<String, String>()
        var totalEp = eps.size
        eps.forEach { epLi ->
            val link = epLi.getElementsByTag("a").attr("href")
//            val name = epLi.select(".name").text().replace(animeName,"")
            subMap[totalEp.toString()] = link
            totalEp--
        }

        val epMap = mutableMapOf("DEFAULT" to subMap)

        return AnimeDetails(animeName, animDesc, animeCover, epMap)
    }


    override suspend fun searchAnime(searchedText: String): ArrayList<SimpleAnime> {
        val searchUrl = "$mainUrl/search.html?keyword=${searchedText}"
        return getItems(searchUrl)
    }

    private fun getItems(url: String): ArrayList<SimpleAnime> {
        val animeList = arrayListOf<SimpleAnime>()
        val doc = Jsoup.parse(get(url))
        val allInfo = doc.getElementsByClass("video-block")
        for (item in allInfo) {
            val itemImage = item.getElementsByTag("img").attr("src")
            val itemName = item.getElementsByClass("name").text().substringBefore("Episode ")
            val itemLink = item.getElementsByTag("a").attr("href")
            animeList.add(SimpleAnime(itemName, itemImage, itemLink))
        }
        return animeList
    }

    override suspend fun latestAnime(): ArrayList<SimpleAnime> {
        return getItems(mainUrl)
    }

    override suspend fun trendingAnime(): ArrayList<SimpleAnime> {
        return getItems("$mainUrl/popular")

    }

    override suspend fun streamLink(
        animeUrl: String,
        animeEpCode: String,
        extras: List<String>?
    ): AnimeStreamLink =
        withContext(Dispatchers.IO) {
            // Get the link of episode
            val animeEpUrl = "$mainUrl$animeEpCode"
            val doc = Jsoup.parse(get(animeEpUrl))

            val embedLink = "https:"+doc.selectFirst(".play-video")!!.getElementsByTag("iframe")
                    .attr("src")
            println(embedLink)
            val id = Uri.parse(embedLink).getQueryParameter("id")!!
            val embedDoc = Jsoup.parse(get(embedLink))
            val scriptValue = embedDoc.select("script[data-name='crypto']").attr("data-value")
            val key = "93422192433952489752342908585752"
            val iv = "9262859232435825"
            val encryptedKey = encryptAES(id, key, iv)
            val decryptedToken = decryptAES(scriptValue, key, iv)

            val url = "https://${Uri.parse(embedLink).host}/encrypt-ajax.php?id=$encryptedKey&alias=$decryptedToken"
            println(url)
            val data = JsonParser.parseString(get(url)).asJsonObject["data"].asString
            println(data)
            val link = JsonParser.parseString(decryptAES(data, key, iv)).asJsonObject["source"].asJsonArray.first().asJsonObject["file"].asString
            println(link)
            return@withContext AnimeStreamLink(link, "", true)

        }

    private fun encryptAES(data: String, key: String, iv: String): String {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val secretKey = SecretKeySpec(key.toByteArray(), "AES")
        val ivSpec = IvParameterSpec(iv.toByteArray())
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)
        val encryptedBytes = cipher.doFinal(data.toByteArray())
        return Base64.encodeToString(encryptedBytes, Base64.DEFAULT)
    }

    private fun decryptAES(encryptedData: String, key: String, iv: String): String {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val secretKey = SecretKeySpec(key.toByteArray(), "AES")
        val ivSpec = IvParameterSpec(iv.toByteArray())
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
        val decryptedBytes = cipher.doFinal(Base64.decode(encryptedData, Base64.DEFAULT))
        return String(decryptedBytes)
    }
}
