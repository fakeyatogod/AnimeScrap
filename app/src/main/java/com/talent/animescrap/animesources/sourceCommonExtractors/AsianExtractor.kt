package com.talent.animescrap.animesources.sourceCommonExtractors

import android.net.Uri
import android.util.Base64
import com.google.gson.JsonParser
import com.talent.animescrap.utils.Utils
import org.jsoup.Jsoup
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class AsianExtractor {
    private val key = "93422192433952489752342908585752"
    private val iv = "9262859232435825"

    public fun getAsianStreamLink(embedLink: String): String {
        val id = Uri.parse(embedLink).getQueryParameter("id")!!
        val embedDoc = Jsoup.parse(Utils.get(embedLink))
        val scriptValue = embedDoc.select("script[data-name='crypto']").attr("data-value")

        val encryptedKey = encryptAES(id, key, iv)
        val decryptedToken = decryptAES(scriptValue, key, iv)

        val url =
            "https://${Uri.parse(embedLink).host}/encrypt-ajax.php?id=$encryptedKey&alias=$decryptedToken"
        println(url)
        val data = JsonParser.parseString(Utils.get(url)).asJsonObject["data"].asString
        println(data)
        return JsonParser.parseString(
            decryptAES(
                data,
                key,
                iv
            )
        ).asJsonObject["source"].asJsonArray.first().asJsonObject["file"].asString
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