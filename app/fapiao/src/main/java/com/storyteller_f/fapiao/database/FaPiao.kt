package com.storyteller_f.fapiao.database

import android.app.Activity
import android.content.Context
import androidx.databinding.adapters.Converters
import androidx.fragment.app.Fragment
import androidx.room.*
import com.storyteller_f.ui_list.database.DefaultTypeConverter
import kotlinx.coroutines.flow.Flow
import java.util.*

@Entity(tableName = "fapiao", primaryKeys = arrayOf("code", "number"))
class FaPiaoEntity(val code: String, val number: String, val created: Date, val total: Float)

@Dao
interface FaPiaoDao {
    @Query("select * from fapiao")
    fun getAll(): Flow<List<FaPiaoEntity>>

    @Insert
    fun insert(faPiaoEntity: FaPiaoEntity)
}

@Database(
    entities = [FaPiaoEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(DefaultTypeConverter::class)
abstract class FaPiaoDatabase : RoomDatabase() {

    abstract fun fapiaoDao(): FaPiaoDao

    companion object {

        @Volatile
        private var INSTANCE: FaPiaoDatabase? = null

        fun getInstance(context: Context): FaPiaoDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(
                context.applicationContext,
                FaPiaoDatabase::class.java, "fapiao.db"
            )
                .fallbackToDestructiveMigration()
                .build()
    }
}

fun Fragment.requireDatabase() = FaPiaoDatabase.getInstance(requireContext())

fun Activity.requireDatabase() = FaPiaoDatabase.getInstance(this)

fun Context.requireDatabase() = FaPiaoDatabase.getInstance(this)
