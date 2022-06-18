package com.storyteller_f.ui_list.database

import androidx.room.PrimaryKey
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.storyteller_f.ui_list.core.Datum
import java.util.*


abstract class CommonRoomDatabase<D : Datum<RK>, RK : RemoteKey, DT : RoomDatabase>(val database: DT) {
    abstract suspend fun clearOld()
    abstract suspend fun insertRemoteKey(remoteKeys: List<RK>)
    abstract suspend fun getRemoteKey(id: String): RK?
    abstract suspend fun insertAllData(repos: List<D>)
    abstract suspend fun deleteItemBy(d: D)
    @Deprecated("don't use it", replaceWith = ReplaceWith("deleteItem"))
    abstract suspend fun deleteItemById(commonDatumId: String)
}

class DefaultTypeConverter {
    @TypeConverter
    fun convertTimestampToDate(timestamp: Long) = Date(timestamp)

    @TypeConverter
    fun convertDateToTimestamp(date: Date) = date.time
}

open class RemoteKey(
    @PrimaryKey
    open val itemId: String,
    open val prevKey: Int?,
    open val nextKey: Int?
)
