package com.talent.animescrap_common.model

data class AnimeStreamLink(
    val link: String,
    val subsLink: String,
    val isHls: Boolean,
    val extraHeaders: HashMap<String, String>? = null
)
