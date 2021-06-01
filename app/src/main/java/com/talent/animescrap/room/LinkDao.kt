package com.talent.animescrap.room

import androidx.room.*

@Dao
interface LinkDao {
    @Query("SELECT * FROM fav_table")
    fun getLinks(): List<FavLinks>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(fav: FavLinks)

    @Query("DELETE FROM fav_table")
    fun deleteAll()

    @Delete()
    fun deleteOne(fav: FavLinks)

}