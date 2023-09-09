package com.talent.animescrap.utils

import com.github.kittinunf.fuel.Fuel
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.talent.animescrap.animesources.sourceutils.AndroidCookieJar
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.util.concurrent.TimeUnit

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

    fun getJsoup(
        url: String,
        mapOfHeaders: Map<String, String>? = null
    ): Document {
        return Jsoup.connect(url).ignoreContentType(true).apply {
            if (mapOfHeaders != null) {
                headers(mapOfHeaders)
            }
        }.get()
    }

    fun getJson(
        url: String,
        mapOfHeaders: Map<String, String>? = null
    ): JsonElement? {
        val fuel = Fuel.get(url)
        if (mapOfHeaders != null)
            fuel.header(mapOfHeaders)
        val res = fuel.response().third
        val (bytes, _) = res
        if (bytes != null) {
            return JsonParser.parseString(String(bytes))
        }
        return null
    }

    fun postJson(
        url: String,
        mapOfHeaders: Map<String, String>? = null,
        payload: Map<String, String>? = null
    ): JsonElement? {
        val fuel = if (payload == null) Fuel.post(url) else Fuel.post(url, payload.toList())
        if (mapOfHeaders != null) fuel.header(mapOfHeaders)
        val res = fuel.response().third
        val (bytes, _) = res
        if (bytes != null) {
            return JsonParser.parseString(String(bytes))
        }
        return null
    }
}