package com.storyteller_f.common_ui_list_structure.db

import android.app.Activity
import android.content.Context
import androidx.fragment.app.Fragment
import androidx.paging.PagingSource
import androidx.room.*
import com.storyteller_f.common_ui_list_structure.model.Repo
import com.storyteller_f.common_ui_list_structure.model.RepoRemoteKey
import com.storyteller_f.ui_list.database.CommonRoomDatabase

@Dao
interface RepoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(all: List<Repo>)

    @Query("SELECT * FROM repos ORDER BY stars DESC, name ASC")
    fun selectAll(): PagingSource<Int, Repo>

    @Query("DELETE FROM repos")
    suspend fun clearRepos()
}

@Dao
interface RemoteKeysDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(remoteKey: List<RepoRemoteKey>)

    @Query("SELECT * FROM repo_remote_keys WHERE itemId = :repoId")
    suspend fun remoteKeysRepoId(repoId: String): RepoRemoteKey?

    @Query("DELETE FROM repo_remote_keys")
    suspend fun clearRemoteKeys()
}

@Database(
    entities = [Repo::class, RepoRemoteKey::class],
    version = 2,
    exportSchema = false
)
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

class RepoComposite(private val repoDatabase: RepoDatabase) :
    CommonRoomDatabase<Repo, RepoRemoteKey, RepoDatabase>(repoDatabase) {
    override suspend fun clearOld() {
        repoDatabase.reposDao().clearRepos()
        repoDatabase.remoteKeyDao().clearRemoteKeys()
    }

    override suspend fun insertRemoteKey(remoteKeys: List<RepoRemoteKey>) =
        repoDatabase.remoteKeyDao().insertAll(remoteKeys)


    override suspend fun getRemoteKey(id: String): RepoRemoteKey? =
        repoDatabase.remoteKeyDao().remoteKeysRepoId(id)


    override suspend fun insertAllData(repos: List<Repo>) = repoDatabase.reposDao().insertAll(repos)
}

fun Fragment.requireRepoDatabase() = RepoDatabase.getInstance(requireContext())

fun Activity.requireRepoDatabase() = RepoDatabase.getInstance(this)
