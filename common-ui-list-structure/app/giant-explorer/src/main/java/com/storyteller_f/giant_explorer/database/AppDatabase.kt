package com.storyteller_f.giant_explorer.database

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.storyteller_f.ext_func_definition.ExtFuncFlat
import com.storyteller_f.ext_func_definition.ExtFuncFlatType


@Database(
    entities = [FileSizeRecord::class, FileMDRecord::class, FileTorrentRecord::class, BigTimeTask::class, RemoteAccessSpec::class],
    version = 5,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 2, to = 3), AutoMigration(from = 3, to = 4), AutoMigration(from = 4, to = 5)]
)
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
