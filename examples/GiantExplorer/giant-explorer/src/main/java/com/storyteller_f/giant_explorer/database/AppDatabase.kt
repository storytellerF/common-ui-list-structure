package com.storyteller_f.giant_explorer.database

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.storyteller_f.ext_func_definition.ExtFuncFlat
import com.storyteller_f.ext_func_definition.ExtFuncFlatType
import com.storyteller_f.file_system_remote.RemoteAccessDao
import com.storyteller_f.file_system_remote.RemoteAccessSpec

@Database(
    entities = [FileSizeRecord::class, FileMDRecord::class, FileTorrentRecord::class, BigTimeTask::class, RemoteAccessSpec::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class LocalDatabase : RoomDatabase() {

    abstract fun sizeDao(): FileSizeRecordDao
    abstract fun mdDao(): FileMDRecordDao
    abstract fun torrentDao(): FileTorrentRecordDao
    abstract fun bigTimeDao(): BigTimeWorkerDao
    abstract fun remoteAccessDao(): RemoteAccessDao

    companion object {

        @Volatile
        private var INSTANCE: LocalDatabase? = null

        fun getInstance(context: Context): LocalDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(
                context.applicationContext,
                LocalDatabase::class.java, "file-record.db"
            )
                .fallbackToDestructiveMigration()
                .build()
    }
}

@ExtFuncFlat(type = ExtFuncFlatType.V2)
val Context.requireDatabase
    get() = LocalDatabase.getInstance(this)
