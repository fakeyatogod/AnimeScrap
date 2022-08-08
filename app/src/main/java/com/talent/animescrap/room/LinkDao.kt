package com.talent.animescrap.room

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LinkDao {
    @Query("SELECT * FROM fav_table")
    fun getLinks(): Flow<List<FavRoomModel>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(fav: FavRoomModel)

    @Delete
    fun deleteOne(fav: FavRoomModel)

    @Query("SELECT EXISTS (SELECT * FROM fav_table where favLink = :link)")
    fun isItFav(link: String): Boolean

    @Query("SELECT * FROM fav_table where favLink = :link")
    fun getFav(link: String): FavRoomModel

}