package com.talent.animescrap.model

data class AnimeDetails(
    val animeName: String,
    val animeDesc: String,
    val animeCover: String,
    val animeEpisodes: Map<String, String>
)