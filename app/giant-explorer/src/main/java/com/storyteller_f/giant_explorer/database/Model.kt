package com.storyteller_f.giant_explorer.database

import android.content.Context
import androidx.room.*
import com.storyteller_f.ext_func_definition.ExtFuncFlat
import com.storyteller_f.ext_func_definition.ExtFuncFlatType
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

@Database(
    entities = [FileSizeRecord::class, FileMDRecord::class, FileTorrentRecord::class, BigTimeTask::class],
    version = 2,
    exportSchema = false
)
abstract class FileSizeRecordDatabase : RoomDatabase() {

    abstract fun sizeDao(): FileSizeRecordDao
    abstract fun mdDao(): FileMDRecordDao
    abstract fun torrentDao(): FileTorrentRecordDao
    abstract fun bigTimeDao(): BigTimeWorkerDao

    companion object {

        @Volatile
        private var INSTANCE: FileSizeRecordDatabase? = null

        fun getInstance(context: Context): FileSizeRecordDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(
                context.applicationContext,
                FileSizeRecordDatabase::class.java, "file-record.db"
            )
                .fallbackToDestructiveMigration()
                .build()
    }
}

@ExtFuncFlat(type = ExtFuncFlatType.V2)
val Context.requireDatabase get() = FileSizeRecordDatabase.getInstance(this)
