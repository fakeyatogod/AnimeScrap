package com.talent.animescrapsources.animesources

import android.util.Base64
import com.talent.animescrap_common.model.AnimeDetails
import com.talent.animescrap_common.model.AnimeStreamLink
import com.talent.animescrap_common.model.SimpleAnime
import com.talent.animescrap_common.source.AnimeSource
import com.talent.animescrap_common.utils.Utils.get
import com.talent.animescrap_common.utils.Utils.getJson
import com.talent.animescrap_common.utils.Utils.getJsoup
import com.talent.animescrap_common.utils.Utils.postJson
import org.jsoup.Jsoup
import java.net.URLDecoder
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class AniWaveSource : AnimeSource {
    private val mainUrl = "https://aniwave.to"
    private val url = "https://vidplay.online/"
    private val apiUrl = "https://9anime.eltik.net/"

    override suspend fun animeDetails(contentLink: String): AnimeDetails {
        val doc = getJsoup("$mainUrl$contentLink")
        val cover = doc.select("#w-info").first()!!.getElementsByTag("img").attr("src")
        val desc = doc.select("#w-info .info .content").text()
        val title = doc.select("#w-info .info .title").attr("data-jp")

        val dataId = doc.getElementsByAttribute("data-id").first()!!.attr("data-id")
        val vrf = vrfEncrypt(dataId)
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

/*    private fun getVrf(dataId: String): String {
        val json = getJson("$apiUrl/vrf?query=${dataId}&apikey=chayce")
        return json!!.asJsonObject["url"].asString
    }

    private fun decodeVrf(dataId: String): String {
        val json = getJson("$apiUrl/decrypt?query=${dataId}&apikey=chayce")
        return json!!.asJsonObject["url"].asString
    }*/

    private fun getAnimeList(url: String): ArrayList<SimpleAnime> {
        val animeList = arrayListOf<SimpleAnime>()
        val doc = getJsoup(url)
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

    override suspend fun searchAnime(searchedText: String): ArrayList<SimpleAnime> =
        getAnimeList("$mainUrl/filter?keyword=$searchedText")

    override suspend fun latestAnime(): ArrayList<SimpleAnime> =
        getAnimeList(
            "$mainUrl/filter?keyword=&country%5B%5D=120822&language%5B%5D=sub&sort=recently_updated"
        )

    override suspend fun trendingAnime(): ArrayList<SimpleAnime> = getAnimeList(
        "$mainUrl/filter?keyword=&country%5B%5D=120822&language%5B%5D=sub&sort=trending"
    )

    override suspend fun streamLink(
        animeUrl: String,
        animeEpCode: String,
        extras: List<String>?
    ): AnimeStreamLink {
        // 0=sub , 1=soft-sub, 2=dub
        var index = 0
        if(extras?.first() == "Dub") index = 2

        val dataId = animeEpCode.split(",")[index]
        val vrf = vrfEncrypt(dataId)
        val servers =
            Jsoup.parseBodyFragment(getJson("$mainUrl/ajax/server/list/$dataId?vrf=$vrf")!!.asJsonObject["result"].asString)
        val dataLinkId = servers.select(".servers .type ul li")[0]!!.attr("data-link-id")
        val vrf2 = vrfEncrypt(dataLinkId)
        val linkEncoded =
            getJson("$mainUrl/ajax/server/$dataLinkId?vrf=$vrf2")!!.asJsonObject["result"].asJsonObject["url"].asString
        val embedLink = vrfDecrypt(linkEncoded)
//        println(embedLink)
        val fileURL = getFileUrl(embedLink)
//        println(fileURL)
        val link = getJson(
            fileURL,
            mapOf("referer" to embedLink)
        )!!.asJsonObject["result"].asJsonObject["sources"].asJsonArray.first().asJsonObject["file"].asString
//        println(link)
        return AnimeStreamLink(link, "", true)
    }

    private fun getFuToken(referer: String): String {
        val response = get("$url/futoken", mapOf("referer" to referer))
        return response.replace(Regex("""/\*[\s\S]*?\*/|//.*"""), "").replace("\n", "")
    }

    private fun getFileUrl(sourceUrl: String): String {
        val fuToken = getFuToken(sourceUrl)
        val k = sourceUrl.split("/e/")[1].split('?')[0]
        val a = mutableListOf(k)
        for (i in sourceUrl.indices) {
            a.add((k[i % k.length].code + sourceUrl[i].code).toString())
        }
        val u = "$mainUrl/mediainfo/${a.joinToString(",")}?${url.substringAfter("?")}"
        val response = postJson(
            url = "$apiUrl/rawVizcloud?query=$k&apikey=lagrapps",
            payload = mapOf("query" to id, "futoken" to fuToken)
        )
        val rawURL = response!!.asJsonObject["rawURL"].asString
        return "$rawURL?${sourceUrl.split('?')[1]}"
    }

    private fun vrfEncrypt(input: String): String {
        val rc4Key = SecretKeySpec("ysJhV6U27FVIjjuk".toByteArray(), "RC4")
        val cipher = Cipher.getInstance("RC4")
        cipher.init(Cipher.DECRYPT_MODE, rc4Key, cipher.parameters)

        var vrf = cipher.doFinal(input.toByteArray())
        vrf = Base64.encode(vrf, Base64.URL_SAFE or Base64.NO_WRAP)
        vrf = Base64.encode(vrf, Base64.DEFAULT or Base64.NO_WRAP)
        vrf = vrfShift(vrf)
        vrf = Base64.encode(vrf, Base64.DEFAULT)
        vrf = rot13(vrf)
        val stringVrf = vrf.toString(Charsets.UTF_8)
        return java.net.URLEncoder.encode(stringVrf, "utf-8")
    }

    private fun vrfDecrypt(input: String): String {
        var vrf = input.toByteArray()
        vrf = Base64.decode(vrf, Base64.URL_SAFE)

        val rc4Key = SecretKeySpec("hlPeNwkncH0fq9so".toByteArray(), "RC4")
        val cipher = Cipher.getInstance("RC4")
        cipher.init(Cipher.DECRYPT_MODE, rc4Key, cipher.parameters)
        vrf = cipher.doFinal(vrf)

        return URLDecoder.decode(vrf.toString(Charsets.UTF_8), "utf-8")
    }

    private fun rot13(vrf: ByteArray): ByteArray {
        for (i in vrf.indices) {
            val byte = vrf[i]
            if (byte in 'A'.code..'Z'.code) {
                vrf[i] = ((byte - 'A'.code + 13) % 26 + 'A'.code).toByte()
            } else if (byte in 'a'.code..'z'.code) {
                vrf[i] = ((byte - 'a'.code + 13) % 26 + 'a'.code).toByte()
            }
        }
        return vrf
    }

    private fun vrfShift(vrf: ByteArray): ByteArray {
        for (i in vrf.indices) {
            val shift = arrayOf(-3, 3, -4, 2, -2, 5, 4, 5)[i % 8]
            vrf[i] = vrf[i].plus(shift).toByte()
        }
        return vrf
    }
}