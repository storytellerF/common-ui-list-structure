package com.storyteller_f.giant_explorer.database

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.storyteller_f.giant_explorer.control.remote.RemoteAccessType
import com.storyteller_f.giant_explorer.service.FtpSpec
import com.storyteller_f.giant_explorer.service.SmbSpec
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "remote-access", primaryKeys = ["server", "port", "user", "password", "share"])
class RemoteAccessSpec(
    val server: String = "",
    val port: Int = 0,
    val user: String = "",
    val password: String = "",
    @ColumnInfo(defaultValue = "") val share: String = "",
    @ColumnInfo(defaultValue = RemoteAccessType.ftp) val type: String
) {
    fun toFtpSpec(): FtpSpec {
        return FtpSpec(server, port, user, password, type == RemoteAccessType.sftp)
    }

    fun toSmbSpec(): SmbSpec {
        return SmbSpec(server, port, user, password, share)
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