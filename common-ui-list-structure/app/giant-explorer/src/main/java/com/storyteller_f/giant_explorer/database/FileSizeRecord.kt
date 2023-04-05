package com.storyteller_f.giant_explorer.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "file-size-record")
class FileSizeRecord(
    @PrimaryKey
    val absolutePath: String,
    val size: Long,
    val lastUpdateTime: Long
)

@Entity(tableName = "file-md-record")
class FileMDRecord(
    @PrimaryKey
    val absolutePath: String,
    val data: String,
    val lastUpdateTime: Long
)

@Entity(tableName = "file-torrent")
class FileTorrentRecord(
    @PrimaryKey
    val absolutePath: String,
    val torrent: String,
    val lastUpdateTime: Long
)

@Entity(tableName = "big-time-task", primaryKeys = ["absolutePath", "workerName"])
class BigTimeTask(
    val absolutePath: String,
    val enable: Boolean,
    val workerName: String
)

@Dao
interface FileSizeRecordDao {
    @Query("select * from `file-size-record` where absolutePath = :absolutePath")
    suspend fun search(absolutePath: String): FileSizeRecord?

    @Query("select * from `file-size-record` where absolutePath = :absolutePath")
    fun searchInThread(absolutePath: String): FileSizeRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(record: FileSizeRecord)
}

@Dao
interface FileMDRecordDao {
    @Query("select * from `file-md-record` where absolutePath = :absolutePath")
    suspend fun search(absolutePath: String): FileMDRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(record: FileMDRecord)
}

@Dao
interface FileTorrentRecordDao {
    @Query("select * from `file-torrent` where absolutePath = :absolutePath")
    suspend fun search(absolutePath: String): FileTorrentRecord?

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
