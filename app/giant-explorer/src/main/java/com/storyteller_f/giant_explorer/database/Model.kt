package com.storyteller_f.giant_explorer.database

import android.app.Activity
import android.content.Context
import androidx.fragment.app.Fragment
import androidx.room.*

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

@Dao
interface FileSizeRecordDao {
    @Query("select * from `file-size-record` where absolutePath = :absolutePath")
    suspend fun search(absolutePath: String): FileSizeRecord?

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

@Database(
    entities = [FileSizeRecord::class, FileMDRecord::class, FileTorrentRecord::class],
    version = 2,
    exportSchema = false
)
abstract class FileSizeRecordDatabase : RoomDatabase() {

    abstract fun sizeDao(): FileSizeRecordDao
    abstract fun mdDao(): FileMDRecordDao
    abstract fun torrentDao(): FileTorrentRecordDao

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

fun Fragment.requireDatabase() = FileSizeRecordDatabase.getInstance(requireContext())

fun Activity.requireDatabase() = FileSizeRecordDatabase.getInstance(this)

fun Context.requireDatabase() = FileSizeRecordDatabase.getInstance(this)
