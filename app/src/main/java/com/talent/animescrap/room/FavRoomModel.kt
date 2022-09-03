package com.talent.animescrap.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fav_table")
data class FavRoomModel(
    @PrimaryKey @ColumnInfo(name = "favLink") val linkString: String,
    @ColumnInfo(name = "favPic") val picLinkString: String,
    @ColumnInfo(name = "favName") val nameString: String,
    @ColumnInfo(name = "favSource") val sourceString: String?
)