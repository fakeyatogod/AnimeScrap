package com.talent.animescrap.repo

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager
import com.talent.animescrap.R
import com.talent.animescrap.animesources.*
import com.talent.animescrap.model.AnimeDetails
import com.talent.animescrap.model.AnimeStreamLink
import com.talent.animescrap.model.SimpleAnime
import com.talent.animescrap.room.FavRoomModel
import com.talent.animescrap.room.LinkDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

interface AnimeRepository {
    // API operations
    suspend fun getAnimeDetailsFromSite(contentLink: String): AnimeDetails?
    suspend fun searchAnimeFromSite(searchUrl: String): ArrayList<SimpleAnime>
    suspend fun getLatestAnimeFromSite(): ArrayList<SimpleAnime>
    suspend fun getTrendingAnimeFromSite(): ArrayList<SimpleAnime>
    suspend fun getStreamLink(
        animeUrl: String,
        animeEpCode: String,
        extras: List<String>
    ): AnimeStreamLink

    // Room Operations
    suspend fun getFavoritesFromRoom(): Flow<List<SimpleAnime>>
    suspend fun checkFavoriteFromRoom(animeLink: String, sourceName: String): Boolean
    suspend fun removeFavFromRoom(animeLink: String, sourceName: String)
    suspend fun addFavToRoom(favRoomModel: FavRoomModel)
}


class AnimeRepositoryImpl @Inject constructor(
    private val linkDao: LinkDao,
    application: Application
) : AnimeRepository {

    private val selectedSource = PreferenceManager
        .getDefaultSharedPreferences(application)
        .getString("source", "yugen")

    private val animeSource: AnimeSource =
        when (selectedSource) {
            "yugen" -> {
                YugenSource()
            }
//            "zoro" -> {
//                ZoroSource()
//            }
            "allanime" -> {
                AllAnimeSource()
            }
            "enime" -> {
                EnimeSource()
            }
            "kiss_kh" -> {
                KissKhSource()
            }
            "animepahe" -> {
                AnimePaheSource(application as Context)
            }
            "kawaiifu" -> {
                KawaiifuSource(application as Context)
            }
            "marin_moe" -> {
                MarinMoeSource()
            }
            else -> {
                YugenSource()
            }
        }

    override suspend fun getAnimeDetailsFromSite(contentLink: String) =
        withContext(Dispatchers.IO) {
            Log.i(TAG, "Getting anime details")
            try {
                return@withContext animeSource.animeDetails(contentLink)
            } catch (e: Exception) {
                Log.e(TAG, e.toString())
                return@withContext null
            }

        }

    override suspend fun searchAnimeFromSite(searchUrl: String) = withContext(Dispatchers.IO) {
        Log.i(TAG, "Getting to search anime")
        try {
            return@withContext animeSource.searchAnime(searchUrl)
        } catch (e: Exception) {
            Log.e(TAG, e.toString())
            return@withContext arrayListOf()
        }

    }

    override suspend fun getLatestAnimeFromSite(): ArrayList<SimpleAnime> =
        withContext(Dispatchers.IO) {
            Log.i(TAG, "Getting the latest anime")
            try {
                return@withContext animeSource.latestAnime()
            } catch (e: Exception) {
                Log.e(TAG, e.toString())
                return@withContext arrayListOf()
            }
        }

    override suspend fun getTrendingAnimeFromSite(): ArrayList<SimpleAnime> =
        withContext(Dispatchers.IO) {
            Log.i(TAG, "Getting the trending anime")
            try {
                return@withContext animeSource.trendingAnime()
            } catch (e: Exception) {
                Log.e(TAG, e.toString())
                return@withContext arrayListOf()
            }
        }

    override suspend fun getStreamLink(
        animeUrl: String,
        animeEpCode: String,
        extras: List<String>
    ): AnimeStreamLink =
        withContext(Dispatchers.IO) {
            Log.i(TAG, "Getting the anime stream Link")
            try {
                return@withContext animeSource.streamLink(animeUrl, animeEpCode, extras)
            } catch (e: Exception) {
                Log.e(TAG, e.toString())
                return@withContext AnimeStreamLink("", "", false)
            }

        }

    override suspend fun getFavoritesFromRoom() = withContext(Dispatchers.IO) {
        return@withContext linkDao.getLinks(selectedSource).map { animeList ->
            animeList.map { SimpleAnime(it.nameString, it.picLinkString, it.linkString) }
        }
    }

    override suspend fun checkFavoriteFromRoom(animeLink: String, sourceName: String): Boolean =
        withContext(Dispatchers.IO) {
            return@withContext linkDao.isItFav(animeLink, sourceName)
        }

    override suspend fun removeFavFromRoom(animeLink: String, sourceName: String) =
        withContext(Dispatchers.IO) {
            val foundFav = linkDao.getFav(animeLink, sourceName)
            linkDao.deleteOne(foundFav)
        }

    override suspend fun addFavToRoom(
        favRoomModel: FavRoomModel
    ) = withContext(Dispatchers.IO) {
        linkDao.insert(favRoomModel)
    }

    companion object {
        const val TAG = R.string.app_name.toString()
    }
}