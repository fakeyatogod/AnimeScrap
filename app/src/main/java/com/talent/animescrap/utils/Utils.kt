package com.talent.animescrap.utils

import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class Utils {
    fun getJsoup(url: String): Document {
        return Jsoup.connect(url).get()
    }
}