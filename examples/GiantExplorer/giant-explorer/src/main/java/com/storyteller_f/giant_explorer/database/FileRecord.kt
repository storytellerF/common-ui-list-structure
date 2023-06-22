package com.storyteller_f.giant_explorer.database

import android.net.Uri
import androidx.room.*
import kotlinx.coroutines.flow.Flow

class Converters {
    @TypeConverter
    fun parseUri(uriString: String?): Uri? {
        return uriString?.let { Uri.parse(it) }
    }

    @TypeConverter
    fun stringifyUri(date: Uri?): String? {
        return date?.toString()
    }
}

@Entity(tableName = "file-size-record")
class FileSizeRecord(
    @PrimaryKey
    val uri: Uri,
    val size: Long,
    val lastUpdateTime: Long
)

@Entity(tableName = "file-md-record")
class FileMDRecord(
    @PrimaryKey
    val uri: Uri,
    val data: String,
    val lastUpdateTime: Long
)

@Entity(tableName = "file-torrent")
class FileTorrentRecord(
    @PrimaryKey
    val uri: Uri,
    val torrent: String,
    val lastUpdateTime: Long
)

@Entity(tableName = "big-time-task", primaryKeys = ["uri", "category"])
class BigTimeTask(
    val uri: Uri,
    val enable: Boolean,
    val category: String,
)

@Dao
interface FileSizeRecordDao {
    @Query("select * from `file-size-record` where uri = :uri")
    suspend fun search(uri: Uri): FileSizeRecord?

    @Query("select * from `file-size-record` where uri = :uri")
    fun searchInThread(uri: Uri): FileSizeRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(record: FileSizeRecord)
}

@Dao
interface FileMDRecordDao {
    @Query("select * from `file-md-record` where uri = :uri")
    suspend fun search(uri: Uri): FileMDRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(record: FileMDRecord)
}

@Dao
interface FileTorrentRecordDao {
    @Query("select * from `file-torrent` where uri = :uri")
    suspend fun search(uri: Uri): FileTorrentRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(record: FileTorrentRecord)
}

@Dao
interface BigTimeWorkerDao {
    @Query("select * from `big-time-task`")
    fun fetch(): Flow<List<BigTimeTask>>

    @Query("select * from `big-time-task`")
    suspend fun fetchSuspend(): List<BigTimeTask>

    @Insert
    suspend fun add(task: BigTimeTask)

    @Update
    fun update(task: BigTimeTask)
}
