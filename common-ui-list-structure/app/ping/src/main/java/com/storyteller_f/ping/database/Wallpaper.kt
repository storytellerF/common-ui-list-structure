package com.storyteller_f.ping.database

import android.content.Context
import androidx.paging.PagingSource
import androidx.room.*
import com.storyteller_f.composite_defination.Composite
import com.storyteller_f.ext_func_definition.ExtFuncFlat
import com.storyteller_f.ext_func_definition.ExtFuncFlatType
import com.storyteller_f.ui_list.core.Model
import com.storyteller_f.ui_list.database.DefaultTypeConverter
import kotlinx.coroutines.flow.Flow
import java.util.*

@Entity(tableName = "wallpaper")
data class Wallpaper(@PrimaryKey val uri: String, val name: String, val createdTime: Date) : Model {
    override fun commonId() = uri
}

@Dao
interface RepoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(all: List<Wallpaper>)

    @Query("SELECT * FROM wallpaper ORDER BY createdTime DESC")
    fun selectAll(): Flow<List<Wallpaper>>

    @Query("select * from wallpaper")
    suspend fun select(): Wallpaper

    @Query("DELETE FROM wallpaper")
    suspend fun clearRepos()

    @Delete
    suspend fun delete(repo: Wallpaper)

    @Query("Delete From wallpaper where uri = :uri")
    suspend fun delete(uri: String)
}

@Database(
    entities = [Wallpaper::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(DefaultTypeConverter::class)
abstract class RepoDatabase : RoomDatabase() {

    abstract fun reposDao(): RepoDao

    companion object {

        @Volatile
        private var INSTANCE: RepoDatabase? = null

        fun getInstance(context: Context): RepoDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(
                context.applicationContext,
                RepoDatabase::class.java, "wallpapers.db"
            )
                .fallbackToDestructiveMigration()
                .build()
    }
}

@ExtFuncFlat(type = ExtFuncFlatType.V2)
val Context.requireRepoDatabase
    get() = RepoDatabase.getInstance(this)
