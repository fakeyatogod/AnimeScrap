package com.talent.animescrap.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fav_table")
data class FavLinks(@PrimaryKey @ColumnInfo(name = "fav") val linkString: String)