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

@Dao
interface FileSizeRecordDao {
    @Query("select * from `file-size-record` where absolutePath = :absolutePath")
    suspend fun search(absolutePath: String): FileSizeRecord?
}

@Database(
    entities = [FileSizeRecord::class],
    version = 2,
    exportSchema = false
)
abstract class FileSizeRecordDatabase : RoomDatabase() {

    abstract fun reposDao(): FileSizeRecordDao

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
                FileSizeRecordDatabase::class.java, "file-size-record.db"
            )
                .fallbackToDestructiveMigration()
                .build()
    }
}

fun Fragment.requireDatabase() = FileSizeRecordDatabase.getInstance(requireContext())

fun Activity.requireDatabase() = FileSizeRecordDatabase.getInstance(this)
