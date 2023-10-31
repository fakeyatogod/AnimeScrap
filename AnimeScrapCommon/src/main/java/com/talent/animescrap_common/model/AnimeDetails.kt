package com.talent.animescrap_common.model

data class AnimeDetails(
    val animeName: String,
    val animeDesc: String,
    val animeCover: String,
    val animeEpisodes: Map<String, Map<String, String>>
)