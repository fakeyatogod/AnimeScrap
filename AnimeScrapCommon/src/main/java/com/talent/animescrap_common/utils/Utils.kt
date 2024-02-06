package com.talent.animescrap_common.utils

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.talent.animescrap_common.sourceutils.AndroidCookieJar
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ConnectionSpec
import java.util.*
import android.util.Log
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.util.concurrent.TimeUnit
import java.security.cert.X509Certificate
import org.conscrypt.Conscrypt
import java.security.Security

object Utils {  
     
    var httpClient = OkHttpClient.Builder()
    .cookieJar(AndroidCookieJar())
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .callTimeout(2, TimeUnit.MINUTES)
    .build()
    
    fun get(url: String,
            mapOfHeaders: Map<String, String>? = null
    ): String {
        val requestBuilder = Request.Builder().url(url)
        if (!mapOfHeaders.isNullOrEmpty()) {
            mapOfHeaders.forEach{
                requestBuilder.addHeader(it.key, it.value)
            }
        }
        return httpClient.newCall(requestBuilder.build())
            .execute().body!!.string()
    }

    fun post(url: String, mapOfHeaders: Map<String, String>? = null, payload: Map<String, String>? = null): String {
        val requestBuilder = Request.Builder().url(url)

        if (!mapOfHeaders.isNullOrEmpty()) {
            mapOfHeaders.forEach {
                requestBuilder.addHeader(it.key, it.value)
            }
        }

        val requestBody = payload?.let {
            FormBody.Builder().apply {
                it.forEach { (key, value) ->
                    add(key, value)
                }
            }.build()
        }

        if (requestBody != null) {
            requestBuilder.post(requestBody)
        }

        val response = httpClient.newCall(requestBuilder.build()).execute()
        return response.body?.string() ?: ""
    }

    fun getJsoup(
        url: String,
        mapOfHeaders: Map<String, String>? = null
    ): Document {
        return Jsoup.parse(get(url, mapOfHeaders))
    }

    fun getJson(
        url: String,
        mapOfHeaders: Map<String, String>? = null
    ): JsonElement? {
        return JsonParser.parseString(get(url, mapOfHeaders))
    }   
    fun getJsonFromAnime(animeName: String , episodeNumber: String): JsonElement? {
        Security.insertProviderAt(Conscrypt.newProvider(), 1);
        val customHttpClient = OkHttpClient.Builder()
            .connectionSpecs(Arrays.asList(ConnectionSpec.RESTRICTED_TLS))
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .callTimeout(2, TimeUnit.MINUTES)
            .build()
        val jikanRequestBuilder = Request.Builder()
            .url("https://api.jikan.moe/v4/anime?q=$animeName")
            .addHeader("Accept" , "application/json")
            .method("GET" , null)
        val jikanResult = customHttpClient.newCall(jikanRequestBuilder.build()).execute().body!!.string()
        val malId = JsonParser.parseString(jikanResult).asJsonObject["data"].asJsonArray[0].asJsonObject["mal_id"]
        val requestBuilder = Request.Builder()
            .url("https://api.aniskip.com/v2/skip-times/$malId/$episodeNumber?types=op&episodeLength=0")
            .addHeader("Accept" , "application/json")
            .method("GET" , null)
        val result = customHttpClient.newCall(requestBuilder.build()).execute().body!!.string()
        return JsonParser.parseString(result)
    }

    fun postJson(
        url: String,
        mapOfHeaders: Map<String, String>? = null,
        payload: Map<String, String>? = null
    ): JsonElement? {
        val res = post(url, mapOfHeaders, payload)
        return JsonParser.parseString(res)
    }
}