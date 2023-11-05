package com.storyteller_f.ping.database

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.storyteller_f.ext_func_definition.ExtFuncFlat
import com.storyteller_f.ext_func_definition.ExtFuncFlatType
import com.storyteller_f.ui_list.core.Model
import com.storyteller_f.ui_list.database.DefaultTypeConverter
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Entity(tableName = "wallpaper")
data class Wallpaper(
    @PrimaryKey val uri: String,
    val name: String,
    val createdTime: Date,
    val thumbnail: String
) : Model {
    override fun commonId() = uri
}

@Dao
interface MainDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(all: List<Wallpaper>)

    @Query("SELECT * FROM wallpaper ORDER BY createdTime DESC")
    fun selectAll(): Flow<List<Wallpaper>>

    @Query("SELECT * FROM wallpaper where uri = :uri ORDER BY createdTime DESC")
    fun select(uri: String): Flow<Wallpaper>

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
    entities = [Wallpaper::class], version = 1, exportSchema = false
)
@TypeConverters(DefaultTypeConverter::class)
abstract class MainDatabase : RoomDatabase() {

    abstract fun dao(): MainDao

    companion object {

        @Volatile
        private var INSTANCE: MainDatabase? = null

        fun getInstance(context: Context): MainDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
        }

        private fun buildDatabase(context: Context) = Room.databaseBuilder(
            context.applicationContext, MainDatabase::class.java, "wallpapers.db"
        ).fallbackToDestructiveMigration().build()
    }
}

@ExtFuncFlat(type = ExtFuncFlatType.V2)
val Context.requireMainDatabase
    get() = MainDatabase.getInstance(this)
