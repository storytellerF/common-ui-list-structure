package com.storyteller_f.common_ui_list_structure.db

import android.content.Context
import androidx.paging.PagingSource
import androidx.room.*
import com.storyteller_f.common_ui_list_structure.model.Repo
import com.storyteller_f.common_ui_list_structure.model.RepoRemoteKey
import com.storyteller_f.composite_defination.Composite
import com.storyteller_f.ext_func_definition.ExtFuncFlat
import com.storyteller_f.ext_func_definition.ExtFuncFlatType

@Dao
interface RepoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(all: MutableList<out Repo>)

    @Query("SELECT * FROM repos ORDER BY stars DESC, name ASC")
    fun selectAll(): PagingSource<Int, Repo>

    @Query("select * from repos")
    fun select(): Repo

    @Query("DELETE FROM repos")
    suspend fun clearRepos()

    @Delete
    suspend fun delete(repo: Repo)

    @Query("Delete From repos where id = :repoId")
    suspend fun delete(repoId: Long)
}

@Dao
interface RemoteKeysDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(remoteKey: MutableList<out RepoRemoteKey>)

    @Query("SELECT * FROM repo_remote_keys WHERE itemId = :repoId")
    suspend fun remoteKeysRepoId(repoId: String): RepoRemoteKey?

    @Query("DELETE FROM repo_remote_keys")
    suspend fun clearRemoteKeys()

    @Query("Delete From repo_remote_keys where itemId = :repoId")
    suspend fun delete(repoId: String)
}

@Database(
    entities = [Repo::class, RepoRemoteKey::class],
    version = 2,
    exportSchema = false
)
@Composite("Repo")
abstract class RepoDatabase : RoomDatabase() {

    abstract fun reposDao(): RepoDao
    abstract fun remoteKeyDao(): RemoteKeysDao


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
                RepoDatabase::class.java, "repo.db"
            )
                .fallbackToDestructiveMigration()
                .build()
    }
}

@ExtFuncFlat(type = ExtFuncFlatType.V2)
val Context.requireRepoDatabase
    get() = RepoDatabase.getInstance(this)
