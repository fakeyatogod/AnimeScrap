package com.talent.animescrap.repo

import android.util.Log
import com.talent.animescrap.R
import com.talent.animescrap.animesources.YugenSource
import com.talent.animescrap.model.AnimeDetails
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
    suspend fun getStreamLink(animeEpUrl: String): String

    // Room Operations
    suspend fun getFavoritesFromRoom(): Flow<List<SimpleAnime>>
    suspend fun checkFavoriteFromRoom(animeLink: String): Boolean
    suspend fun removeFavFromRoom(animeLink: String)
    suspend fun addFavToRoom(animeLink: String, animeName: String, animeCoverLink: String)
}


class AnimeRepositoryImpl @Inject constructor(
    private val linkDao: LinkDao
) : AnimeRepository {
    private val animeSource = YugenSource()
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

    override suspend fun getStreamLink(animeEpUrl: String): String =
        withContext(Dispatchers.IO) {
            Log.i(TAG, "Getting the anime stream Link")
            try {
                return@withContext animeSource.streamLink(animeEpUrl)
            } catch (e: Exception) {
                Log.e(TAG, e.toString())
                return@withContext "No Link Found"
            }

    }

    override suspend fun getFavoritesFromRoom() = withContext(Dispatchers.IO) {
        return@withContext linkDao.getLinks().map { animeList ->
            animeList.map { SimpleAnime(it.nameString, it.picLinkString, it.linkString) }
        }
    }

    override suspend fun checkFavoriteFromRoom(animeLink: String): Boolean =
        withContext(Dispatchers.IO) {
            return@withContext linkDao.isItFav(animeLink)
        }

    override suspend fun removeFavFromRoom(animeLink: String) =
        withContext(Dispatchers.IO) {
            val foundFav = linkDao.getFav(animeLink)
            linkDao.deleteOne(foundFav)
        }

    override suspend fun addFavToRoom(
        animeLink: String, animeName: String, animeCoverLink: String
    ) = withContext(Dispatchers.IO) {
        linkDao.insert(FavRoomModel(animeLink, animeCoverLink, animeName))
    }

    companion object {
        const val TAG = R.string.app_name.toString()
    }
}