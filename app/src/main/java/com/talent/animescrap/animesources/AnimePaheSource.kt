package com.talent.animescrap.animesources

import android.content.Context
import com.google.gson.JsonParser
import com.talent.animescrap.animesources.sourceutils.AndroidCookieJar
import com.talent.animescrap.animesources.sourceutils.CloudflareInterceptor
import com.talent.animescrap.model.AnimeDetails
import com.talent.animescrap.model.AnimeStreamLink
import com.talent.animescrap.model.SimpleAnime
import com.talent.animescrap.utils.Utils.getJson
import com.talent.animescrap.utils.Utils.getJsoup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import java.util.concurrent.TimeUnit
import kotlin.math.pow

class AnimePaheSource(context: Context) : AnimeSource {

    private val client = OkHttpClient.Builder()
        .cookieJar(AndroidCookieJar())
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(2, TimeUnit.MINUTES)
        .addInterceptor(CloudflareInterceptor(context))
        .build()

    private var cookies: String = ""
    private val kwikParamsRegex = Regex("""\("(\w+)",\d+,"(\w+)",(\d+),(\d+),\d+\)""")
    private val kwikDUrl = Regex("action=\"([^\"]+)\"")
    private val kwikDToken = Regex("value=\"([^\"]+)\"")

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
            val r = JsonParser.parseString(client.newCall(Request.Builder().url(urlForLinks).build())
                .execute().body!!.string())
            println("r = $r")
            val data = r.asJsonObject["data"].asJsonArray.last().asJsonObject

            val kwikLink =
                if(data.has("1080")) data["1080"].asJsonObject["kwik_pahewin"].asString
                else if(data.has("720")) data["720"].asJsonObject["kwik_pahewin"].asString
                else if(data.has("480")) data["480"].asJsonObject["kwik_pahewin"].asString
                else if (data.has("360")) data["360"].asJsonObject["kwik_pahewin"].asString
                else ""

            println(kwikLink)
            val hlsLink = getStreamUrlFromKwik(kwikLink)
            println(hlsLink)
            return@withContext AnimeStreamLink(
                hlsLink, "", false,
                extraHeaders = hashMapOf("referer" to "https://kwik.cx")
            )
        }


    private fun getStreamUrlFromKwik(paheUrl: String): String {

        val noRedirects = client.newBuilder()
            .followRedirects(false)
            .followSslRedirects(false)
            .build()
        val kwikUrl =
            "https://" + noRedirects.newCall(Request.Builder().url("$paheUrl/i").build()).execute()
                .header("location")!!.substringAfterLast("https://")
        println(kwikUrl)
        val fContent =
            client.newCall(
                Request.Builder().url(kwikUrl).header("referer", "https://kwik.cx/").build()
            ).execute()
        cookies += (fContent.header("set-cookie")!!)
        val fContentString = fContent.body!!.string()

        val (fullString, key, v1, v2) = kwikParamsRegex.find(fContentString)!!.destructured
        val decrypted = decrypt(fullString, key, v1.toInt(), v2.toInt())
        val uri = kwikDUrl.find(decrypted)!!.destructured.component1()
        val tok = kwikDToken.find(decrypted)!!.destructured.component1()
        var content: Response? = null

        println(uri)
        println(tok)

        var code = 419
        var tries = 0

        val noRedirectClient = OkHttpClient().newBuilder()
            .followRedirects(false)
            .followSslRedirects(false)
            .cookieJar(client.cookieJar)
            .build()

        while (code != 302 && tries < 20) {

            content = noRedirectClient.newCall(
                Request.Builder()
                    .url(uri)
                    .headers(
                    Headers.headersOf(
                        "referer", fContent.request.url.toString(),
                        "cookie", fContent.header("set-cookie")!!.replace("path=/;", "")
                    ))
                    .post(FormBody.Builder().add("_token", tok).build())
                    .cacheControl( CacheControl.Builder().maxAge(10, TimeUnit.MINUTES).build())
                    .build()
                /*POST(
                    uri,
                    ,
                    FormBody.Builder().add("_token", tok).build()
                )*/
            ).execute()
            code = content.code
            println(code)
            ++tries
        }
        if (tries > 19) {
            throw Exception("Failed to extract the stream uri from kwik.")
        }
        val location = content?.header("location").toString()
        content?.close()
        return location
    }

    private fun decrypt(fullString: String, key: String, v1: Int, v2: Int): String {
        var r = ""
        var i = 0

        while (i < fullString.length) {
            var s = ""

            while (fullString[i] != key[v2]) {
                s += fullString[i]
                ++i
            }
            var j = 0

            while (j < key.length) {
                s = s.replace(key[j].toString(), j.toString())
                ++j
            }
            r += (getString(s, v2).toInt() - v1).toChar()
            ++i
        }
        return r
    }

    private fun getString(content: String, s1: Int): String {
        val s2 = 10
        val characterMap = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ+/"

        val slice2 = characterMap.slice(0 until s2)
        var acc: Long = 0

        for ((n, i) in content.reversed().withIndex()) {
            acc += (
                    when (("$i").toIntOrNull() != null) {
                        true -> "$i".toLong()
                        false -> "0".toLong()
                    }
                    ) * s1.toDouble().pow(n.toDouble()).toInt()
        }

        var k = ""

        while (acc > 0) {
            k = slice2[(acc % s2).toInt()] + k
            acc = (acc - (acc % s2)) / s2
        }

        return when (k != "") {
            true -> k
            false -> "0"
        }
    }
}
