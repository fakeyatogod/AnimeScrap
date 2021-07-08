package com.talent.animescrap.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.room.Room
import com.talent.animescrap.model.Photos
import com.talent.animescrap.room.LinksRoomDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FavoriteViewModel(application: Application) : AndroidViewModel(application) {

    private var aList1 = arrayListOf<Photos>()

    private val _animeList1 = MutableLiveData<ArrayList<Photos>>().apply {

        CoroutineScope(Dispatchers.IO).launch {
            getLatestAnime(application)
            withContext(Dispatchers.Main) {
                value = aList1

            }
        }
    }

    private fun getLatestAnime(application: Application) {

        val db = Room.databaseBuilder(
            application.applicationContext, LinksRoomDatabase::class.java, "fav-db"
        ).build()

        val picInfo = arrayListOf<Photos>()
        val linkDao = db.linkDao()
        val favList = linkDao.getLinks()
        for (fav in favList) {
            val picObject = Photos(fav.nameString, fav.picLinkString, fav.linkString)
            picInfo.add(picObject)
        }

        db.close()
        aList1 = picInfo
    }

    val animeFavoriteList: LiveData<ArrayList<Photos>> = _animeList1
}