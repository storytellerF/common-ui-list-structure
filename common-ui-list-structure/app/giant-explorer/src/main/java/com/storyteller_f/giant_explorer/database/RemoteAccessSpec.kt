package com.storyteller_f.giant_explorer.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.storyteller_f.giant_explorer.service.FtpSpec
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "remote-access", primaryKeys = ["server", "port", "user", "password"])
class RemoteAccessSpec(val server: String = "", val port: Int = 0, val user: String = "", val password: String = "") {
    fun toFtpSpec(): FtpSpec {
        return FtpSpec(server, port, user, password)
    }
}

@Dao
interface RemoteAccessDao {
    @Query("select * from `remote-access`")
    fun list(): Flow<List<RemoteAccessSpec>>

    @Query("select * from `remote-access`")
    suspend fun listAsync(): List<RemoteAccessSpec>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun add(spec: RemoteAccessSpec)

    @Delete
    fun remove(spec: RemoteAccessSpec)
}